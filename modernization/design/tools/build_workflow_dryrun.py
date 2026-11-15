#!/usr/bin/env python3
"""Dry run of modernization/design/build-workflow.py with stand-in agents.

No child session is started: register_workflow, agent and log are replaced by
local stand-ins and the script runs unchanged on the real ticket files.  Four
cases are run and checked: all pass; one ticket needs-decision; one build
fails twice; one integration fails once.

    python3 modernization/design/tools/build_workflow_dryrun.py            # the four cases
    python3 modernization/design/tools/build_workflow_dryrun.py --prompt 04-calculation-rules
"""
import asyncio
import pathlib
import random
import re
import sys

DESIGN = pathlib.Path(__file__).resolve().parents[1]
SCRIPT = DESIGN / "build-workflow.py"
TICKETS = DESIGN / "tickets"


class WorkflowAgentError(Exception):
    pass


def realPlan():
    """What the plan agent must return, read from the real ticket files."""
    names = sorted(p.stem for p in TICKETS.glob("[0-9][0-9]-*.md"))
    byNumber = {n.split("-")[0]: n for n in names}
    readme = (TICKETS / "README.md").read_text()
    pilot = re.search(r"\*\*Ticket \[([^\]]+)\]\([^)]*\) is the pilot", readme).group(1)
    tickets = []
    for name in names:
        if name == pilot:
            continue
        text = (TICKETS / f"{name}.md").read_text()
        section = text.split("- **Dependencies:**")[1].split("- **Acceptance:**")[0]
        deps = [byNumber[n] for n in re.findall(r"ticket (\d\d) ", section)]
        tickets.append({"ticket": name, "dependsOn": deps})
    return {"pilot": pilot, "tickets": tickets, "covered": []}


class Run:
    def __init__(self, scenario, plan=None):
        self.scenario = scenario
        self.plan = plan or realPlan()
        self.events = []          # (kind, ticket, attempt, outcome)
        self.logLines = []
        self.prompts = {}         # label -> prompt
        self.runningBuilds = 0
        self.runningIntegrations = 0
        self.maxBuilds = 0
        self.maxIntegrations = 0
        self.integrated = set()
        self.violations = []
        self.random = random.Random(5)

    async def registerWorkflow(self, meta):
        self.meta = meta

    def log(self, message):
        self.logLines.append(message)

    async def agent(self, prompt, phase, schema, label=None, **kwargs):
        self.prompts[label] = prompt
        if phase == "plan":
            return self.plan
        if phase == "report":
            return {"prUrl": "https://example.invalid/pr/report", "sessionUrl": "stand-in"}
        match = re.match(r"(build|integrate)-(.*?)(?:-attempt(\d))?$", label)
        ticket, attempt = match.group(2), int(match.group(3) or 1)
        deps = next(t["dependsOn"] for t in self.plan["tickets"] if t["ticket"] == ticket)
        if phase == "build":
            missing = [d for d in deps if d != self.plan["pilot"] and d not in self.integrated]
            if missing:
                self.violations.append(f"{ticket} started before {missing} integrated")
            self.runningBuilds += 1
            self.maxBuilds = max(self.maxBuilds, self.runningBuilds)
            await asyncio.sleep(self.random.uniform(0.01, 0.05))
            self.runningBuilds -= 1
            if attempt > 1 and "quoted:" not in prompt:
                self.violations.append(f"{ticket} retry prompt does not quote the failure")
            if self.scenario.get("needsDecision") == ticket:
                self.events.append(("build", ticket, attempt, "needs-decision"))
                return {"state": "needs-decision", "summary": "stand-in", "neededFile": "some/File.java",
                        "reason": "stand-in reason"}
            if self.scenario.get("buildFailsTwice") == ticket:
                self.events.append(("build", ticket, attempt, "failed"))
                return {"state": "failed", "summary": "stand-in", "failures": [f"stand-in build failure {attempt}"]}
            self.events.append(("build", ticket, attempt, "built"))
            return {"state": "built", "summary": "stand-in", "prUrl": f"https://example.invalid/pr/{ticket}",
                    "sessionUrl": f"stand-in-{ticket}-{attempt}"}
        self.runningIntegrations += 1
        self.maxIntegrations = max(self.maxIntegrations, self.runningIntegrations)
        await asyncio.sleep(self.random.uniform(0.005, 0.02))
        self.runningIntegrations -= 1
        if self.scenario.get("integrationFailsOnce") == ticket and attempt == 1:
            self.events.append(("integrate", ticket, attempt, "failed"))
            return {"passed": False, "failure": "stand-in: i02-tie-half-cent FAIL after combining"}
        self.integrated.add(ticket)
        self.events.append(("integrate", ticket, attempt, "passed"))
        return {"passed": True, "commit": "0000000"}

    def execute(self):
        scope = {"register_workflow": self.registerWorkflow, "agent": self.agent, "log": self.log,
                 "WorkflowAgentError": WorkflowAgentError, "__name__": "__workflow__"}
        exec(compile(SCRIPT.read_text(), str(SCRIPT), "exec"), scope)
        resultLine = next(l for l in self.logLines if l.startswith("RESULT "))
        self.states = __import__("json").loads(resultLine[len("RESULT "):])


def dependents(plan, root):
    hit, changed = {root}, True
    while changed:
        changed = False
        for item in plan["tickets"]:
            if item["ticket"] not in hit and hit & set(item["dependsOn"]):
                hit.add(item["ticket"])
                changed = True
    return hit - {root}


def expectedStates(plan, scenario):
    states = {item["ticket"]: "integrated" for item in plan["tickets"]}
    for key, state in (("needsDecision", "needs-decision"), ("buildFailsTwice", "failed")):
        ticket = scenario.get(key)
        if ticket:
            states[ticket] = state
            for other in dependents(plan, ticket):
                states[other] = "skipped"
    return states


def expectedCalls(plan, scenario, states):
    builds, integrations = {}, {}
    for ticket, state in states.items():
        builds[ticket] = {"integrated": 1, "needs-decision": 1, "failed": 2, "skipped": 0}[state]
        integrations[ticket] = 1 if state == "integrated" else 0
    ticket = scenario.get("integrationFailsOnce")
    if ticket:
        builds[ticket], integrations[ticket] = 2, 2
    return builds, integrations


SCENARIOS = [
    ("all pass", {}),
    ("one ticket needs-decision (06-calc-step-jdbc)", {"needsDecision": "06-calc-step-jdbc"}),
    ("one build fails twice (03-record-codec)", {"buildFailsTwice": "03-record-codec"}),
    ("one integration fails once (04-calculation-rules)", {"integrationFailsOnce": "04-calculation-rules"}),
]


def runScenario(title, scenario, plan=None):
    run = Run(scenario, plan)
    run.execute()
    want = expectedStates(run.plan, scenario)
    wantBuilds, wantIntegrations = expectedCalls(run.plan, scenario, want)
    gotBuilds = {t: sum(1 for e in run.events if e[0] == "build" and e[1] == t) for t in want}
    gotIntegrations = {t: sum(1 for e in run.events if e[0] == "integrate" and e[1] == t) for t in want}
    problems = list(run.violations)
    if run.states != want:
        problems.append(f"states {run.states} != expected {want}")
    if gotBuilds != wantBuilds:
        problems.append(f"build calls {gotBuilds} != expected {wantBuilds}")
    if gotIntegrations != wantIntegrations:
        problems.append(f"integration calls {gotIntegrations} != expected {wantIntegrations}")
    if run.maxBuilds > 4:
        problems.append(f"{run.maxBuilds} builds at a time")
    if run.maxIntegrations > 1:
        problems.append(f"{run.maxIntegrations} integrations at a time")
    stateLines = [l for l in run.logLines if l.startswith("STATE ") and "->" in l]
    for ticket, state in want.items():
        if not any(l.startswith(f"STATE {ticket}:") and f"-> {state}" in l for l in stateLines):
            problems.append(f"no log line for {ticket} -> {state}")
    print(f"== {title}: {'PASS' if not problems else 'FAIL'}")
    print(f"   order ok: {not run.violations}; max builds at a time: {run.maxBuilds} (limit 4); "
          f"max integrations at a time: {run.maxIntegrations} (limit 1); state changes logged: {len(stateLines)}")
    for ticket in sorted(want):
        print(f"   {ticket:22} {run.states.get(ticket):15} builds={gotBuilds[ticket]} integrations={gotIntegrations[ticket]}")
    for problem in problems:
        print(f"   PROBLEM: {problem}")
    return not problems, run


def main():
    if len(sys.argv) == 3 and sys.argv[1] == "--prompt":
        run = Run({})
        run.execute()
        print(run.prompts[f"build-{sys.argv[2]}"])
        return 0
    if len(sys.argv) == 2 and sys.argv[1] == "--log":
        run = Run({"integrationFailsOnce": "04-calculation-rules"})
        run.execute()
        print("\n".join(run.logLines))
        return 0
    results = [runScenario(title, scenario)[0] for title, scenario in SCENARIOS]
    print(f"{sum(results)} of {len(results)} cases pass")
    # The real tickets never have more than three builds ready at once, so the limit of four
    # is also shown on eight stand-in tickets that depend on nothing.
    wide = {"pilot": "01-pilot-thin-slice", "covered": [],
            "tickets": [{"ticket": f"{n}-wide", "dependsOn": []} for n in range(20, 28)]}
    ok, run = runScenario("limit check: eight independent stand-in tickets", {}, wide)
    results.append(ok and run.maxBuilds == 4)
    print(f"limit check: {'PASS' if results[-1] else 'FAIL'} (max builds at a time {run.maxBuilds})")
    return 0 if all(results) else 1


if __name__ == "__main__":
    sys.exit(main())
