#!/usr/bin/env python3
"""Replay lab of the interest stream (INTCALC -> CBACT04C against java/interest).

Runs one case through both engines and compares three things:

  expected   the outcome the rulebook derives (modernization/rules/interest/expected/<case>.json for a
             golden case; the same evaluator run on the inputs of an edge probe or a custom input)
  observed   the legacy COBOL run (GnuCOBOL in the `estate` container, the recorder of tools/parity);
             for a golden case the rerun is also compared with the golden files, as `make parity` does
  target     the Java job of java/interest (profile `database`, as `make parity-java` runs it)

Outputs are decoded with the copybook codec of tools/parity, never compared as raw bytes.

  python3 lab/replay.py gate [--set golden|probes|all] [--report <file>] [--json <file>]
  python3 lab/replay.py negative               the gate with the Java job rounding half-even: must fail
  python3 lab/replay.py case <name>            one golden case or edge probe, printed as JSON

Every compared value gets one class:
  MATCH            observed and target agree
  INTENDED_CHANGE  they differ and an approved decision in decisions.md says they should
  UNRESOLVED       they differ, or agree on behaviour the rulebook marks OPEN, and the decision is open
  UNCLASSIFIED     any other difference
UNRESOLVED is counted twice over: engines agree, and engines differ.
Independently, each engine is judged against the rulebook, CONFORMS or VIOLATES per rule, so two
engines that agree and both break a rule are seen.
The gate exits 0 only when UNCLASSIFIED is zero and neither engine VIOLATES a rule in any case.
"""
import argparse
import fcntl
import json
import os
import re
import shutil
import subprocess
import sys
import time
import uuid
from decimal import Decimal
from pathlib import Path

labDir = Path(__file__).resolve().parent
repoRoot = labDir.parent
rulesDir = repoRoot / "modernization" / "rules" / "interest"
sys.path.insert(0, str(repoRoot))
sys.path.insert(0, str(rulesDir / "tools"))
sys.path.insert(0, str(labDir))
sys.dont_write_bytecode = True

import cases  # noqa: E402
import evaluate  # noqa: E402  (the rulebook evaluator)
from tools.parity import compare  # noqa: E402
from tools.parity.copybook import decode_record, encode_record, parse_copybook  # noqa: E402

workDir = repoRoot / "work" / "lab"
runsDir = workDir / "runs"
decisionsPath = rulesDir / "decisions.md"
chain = json.loads((repoRoot / "tools" / "runjcl" / "chains.json").read_text())["interest"]
interestJar = "java/interest/target/interest.jar"
interestJre = os.environ.get("INTEREST_JRE", "eclipse-temurin:21-jre")
# cells of the expectation that no output file carries: the rate is only visible through TRAN-AMT
derivedOnly = ("rateGroup", "ratePercent")
abendLine = "ABENDING PROGRAM"
classes = ["MATCH", "INTENDED_CHANGE", "UNRESOLVED", "UNCLASSIFIED"]
outputLayouts = {"transactions": ("AWS.M2.CARDDEMO.SYSTRAN", "CVTRA05Y", 350),
                 "accounts": ("AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS", "CVACT01Y", 300)}
transactionFields = [f.name for f in parse_copybook("CVTRA05Y") if not f.is_filler]
accountFields = [f.name for f in parse_copybook("CVACT01Y") if not f.is_filler]


class LabError(RuntimeError):
    """The lab could not run a lane at all (containers down, jar missing)."""


# ------------------------------------------------------------------ rulebook and decision register
def loadRulebook():
    return evaluate.loadRulebook()


def runParameters(rulebook=None):
    params = (rulebook or loadRulebook())["runParameters"]
    return {"parmDate": params["parmDate"]["value"], "runClock": chain["clock"],
            "rulebookClock": params["runClock"]["value"]}


def loadDecisions(path=None):
    """Rows of the decision register: {id: {type, question, status, approvedOption}}.

    A decision is approved when its Status cell reads `APPROVED <option id>` (for example
    `APPROVED B`). OPEN is open; any other text is treated as open and reported as such.
    """
    decisions = {}
    for line in (path or decisionsPath).read_text().splitlines():
        cells = [c.strip() for c in re.split(r"(?<!\\)\|", line)[1:-1]]
        if len(cells) < 7 or not re.fullmatch(r"INT-[QH]\d+", cells[0]):
            continue
        status = cells[6]
        approved = re.fullmatch(r"APPROVED[:\s]+([A-Z])\b.*", status)
        decisions[cells[0]] = {"id": cells[0], "type": cells[1], "question": re.sub(r"\s*_\(.*\)_$", "", cells[2]),
                               "statusText": status, "owner": cells[5],
                               "status": "APPROVED" if approved else "OPEN",
                               "approvedOption": approved.group(1) if approved else None}
    return decisions


def expectationFor(caseRef, rulebook):
    if caseRef["kind"] == "golden":
        path = rulesDir / "expected" / f"{caseRef['name']}.json"
        return json.loads(path.read_text()), str(path.relative_to(repoRoot))
    result = json.loads(evaluate.render(evaluate.expectation(rulebook, caseRef["caseDir"])))
    return result, "rulebook evaluator (modernization/rules/interest/tools/evaluate.py) on this input"


# ------------------------------------------------------------------ the two lanes
def composeEnv():
    return {**os.environ, "UID": str(os.getuid()), "GID": str(os.getgid())}


def sh(command, timeout=300):
    return subprocess.run(command, cwd=repoRoot, env=composeEnv(), capture_output=True, text=True, timeout=timeout)


def dbContainer():
    result = sh(["docker", "compose", "ps", "-q", "db"])
    if result.returncode != 0 or not result.stdout.strip():
        raise LabError("the `db` service is not running - start the lab with `make lab` (or `make up`)")
    return result.stdout.strip().splitlines()[0]


def finishLane(lane, result, started):
    """Process facts of a lane, and from them how the run ended and its return code.

    The return code is STEP15 of rc.json. The Java job writes no rc.json when it stops; its return
    code is then the exit code of the process (12 for a job that did not complete, REL-10)."""
    lane.update(seconds=round(time.time() - started, 1), processExit=result.returncode,
                log=(result.stdout + result.stderr).strip()[-2000:])
    if lane["returnCode"] is None and lane["engine"] == "target" and result.returncode != 0:
        lane["returnCode"] = result.returncode
        lane["returnCodeSource"] = "exit code of the Java process (no rc.json is written when the job stops)"
        lane["runEnd"] = f"abend - exit code {result.returncode}"
    lane["stopped"] = lane["returnCode"] not in (0, None) or (lane["returnCode"] is None and result.returncode != 0)
    lane["jobLog"], lane["afterAbend"] = splitJobLog(lane["sysout"], lane["stopped"])
    return lane


def splitJobLog(sysout, stopped):
    """The job log up to and including ABENDING PROGRAM, and what follows it.

    After that line the off-host legacy log carries the line of the abend stub (tools/stubs/CEE3ABD.cbl)
    and warnings of the GnuCOBOL runtime; they are the harness's, not the program's, and are not compared."""
    if sysout is None:
        return None, []
    if stopped and abendLine in sysout:
        cutAt = sysout.index(abendLine) + 1
        return sysout[:cutAt], [line for line in sysout[cutAt:] if line]
    return sysout, []


def runLegacy(caseRef, outDir, runs=1):
    """Observed side: INTCALC through the JCL runner in the estate container. One lane per run."""
    started = time.time()
    command = ["docker", "compose", "exec", "-T", "-e", "PYTHONDONTWRITEBYTECODE=1", "estate", "python3",
               "lab/legacy_lane.py", str(caseRef["casesRoot"].relative_to(repoRoot)), caseRef["name"],
               str(outDir.relative_to(repoRoot))]
    result = sh(command + ([str(runs)] if runs > 1 else []))
    label = "COBOL CBACT04C (GnuCOBOL, JCL runner)"
    lanes = [decodeLane(outDir / f"run{n}" if runs > 1 else outDir, "legacy", label) for n in range(1, runs + 1)]
    if result.returncode != 0 and not lanes[0]["produced"]:
        raise LabError("the legacy lane did not run: " + ((result.stdout + result.stderr).strip()[-2000:] or "no output")
                       + " - is the estate built? (`make lab` builds it)")
    lanes = [finishLane(lane, result, started) for lane in lanes]
    for lane in lanes:
        lane["processExit"] = 0 if lane["produced"] else result.returncode
    return lanes


def targetArgs():
    return os.environ.get("LAB_TARGET_ARGS", "").split()


def launchTarget(inputDir, outDir, runId):
    params = runParameters()
    started = time.time()
    command = ["docker", "run", "--rm", "--network", f"container:{dbContainer()}",
               "--user", f"{os.getuid()}:{os.getgid()}", "-v", f"{repoRoot}:/estate", "-w", "/estate", interestJre,
               "java", "-jar", interestJar, "--spring.profiles.active=database",
               f"--interest.clock.frozen={params['runClock']}", "--logging.level.root=WARN", *targetArgs(),
               f"parmDate={params['parmDate']}",
               f"runId=lab-{runId}-{time.time_ns()}",      # Spring Batch refuses a second launch with the same parameters
               "format=fixture", f"inputDir={inputDir.relative_to(repoRoot)}",
               f"outputDir={outDir.relative_to(repoRoot)}"]
    result = sh(command)
    return finishLane(decodeLane(outDir, "target", "Java interestCalcJob (Spring Batch, PostgreSQL)"), result, started)


def runTarget(caseRef, outDir, runId, runs=1):
    """Target side: the Java job, three steps through PostgreSQL, as `make parity-java` launches it.

    The job reloads its tables from the unloaded files on every launch (REL-02). For a second run on
    "the same files" the lab plays the write-back of REL-03: the account file the first run exported
    becomes the account file the second run loads. Without that the second run would equal the first."""
    if not (repoRoot / interestJar).exists():
        raise LabError(f"{interestJar} is missing - `make lab` builds it (mvn package, JDK 21)")
    inputDir = caseRef["caseDir"] / "input"
    if runs == 1:
        return [launchTarget(inputDir, outDir, runId)]
    lanes = []
    for number in range(1, runs + 1):
        lanes.append(launchTarget(inputDir, outDir / f"run{number}", f"{runId}-run{number}"))
        exported = outDir / f"run{number}" / "datasets" / (outputLayouts["accounts"][0] + ".txt")
        if lanes[-1]["stopped"] or not exported.exists():
            break
        nextCase = cases.labCasesRoot / "interest" / f"{caseRef['name']}.target-run{number + 1}"
        if nextCase.exists():
            shutil.rmtree(nextCase)
        shutil.copytree(inputDir, nextCase / "input")
        shutil.copyfile(exported, nextCase / "input" / "AWS.M2.CARDDEMO.ACCTDATA.PS.txt")
        inputDir = nextCase / "input"
    return lanes


def showField(field, value):
    """A decoded value as text: money with its decimals, counters with their leading zeros."""
    if isinstance(value, Decimal):
        if field.scale:
            return f"{value:.{field.scale}f}"
        return f"{int(value):0{field.digits}d}"
    return value


def decodeDataset(outDir, kind):
    prefix, layout, lrecl = outputLayouts[kind]
    fields = {f.name: f for f in parse_copybook(layout)}
    paths = sorted((outDir / "datasets").glob(prefix + "*.txt")) if (outDir / "datasets").exists() else []
    if not paths:
        return None, None
    records = []
    for line in paths[-1].read_text(encoding="latin-1").splitlines():
        decoded = decode_record(layout, line.ljust(lrecl).encode("latin-1"))
        records.append({name: showField(fields[name], value) for name, value in decoded.items()})
    return records, paths[-1].name[:-4]


def decodeLane(outDir, engine, label):
    transactions, transactionDsn = decodeDataset(outDir, "transactions")
    accounts, accountDsn = decodeDataset(outDir, "accounts")
    rcPath = outDir / "rc.json"
    returnCodes = json.loads(rcPath.read_text()) if rcPath.exists() else None
    sysoutPath = outDir / "sysout" / "INTCALC.STEP15.txt"
    sysout = [line.rstrip() for line in sysoutPath.read_text(encoding="latin-1").splitlines()] \
        if sysoutPath.exists() else None
    stepRc = (returnCodes or {}).get("INTCALC", {}).get("steps", {}).get("STEP15")
    if stepRc == 0:
        runEnd = "normal end, return code 0"
    elif stepRc is not None:
        runEnd = f"abend - return code {stepRc}"
    else:
        runEnd = "no return code written"
    return {"engine": engine, "label": label, "produced": returnCodes is not None or transactions is not None,
            "returnCode": stepRc, "returnCodeSource": "STEP15 of rc.json", "stopped": stepRc not in (0, None),
            "jobLog": sysout, "afterAbend": [],
            "transactions": transactions or [], "transactionDataset": transactionDsn,
            "accounts": accounts or [], "accountDataset": accountDsn, "accountsWritten": accounts is not None,
            "transactionsWritten": transactions is not None,
            "sysout": sysout, "returnCodes": returnCodes, "runEnd": runEnd,
            "outputDir": str(outDir.relative_to(repoRoot))}


# ------------------------------------------------------------------ expectation cells
def openRulesOf(cell):
    return sorted({c["rule"] for c in cell.get("candidates", [])})


def requiredValue(cell, decisions):
    """What the rulebook requires today: the determined value, the approved option, or the as-coded one."""
    if cell["status"] == "DETERMINED":
        return cell["value"]
    for candidate in cell["candidates"]:
        decision = decisions.get(candidate["rule"], {})
        if decision.get("status") == "APPROVED" and decision["approvedOption"] == candidate["option"]:
            return candidate["value"]
    return next(c["value"] for c in cell["candidates"] if c["asCoded"])


moneyText = re.compile(r"-?\d+\.\d{2}")


def sameValue(left, right):
    if isinstance(left, str) and isinstance(right, str):
        if moneyText.fullmatch(left) and moneyText.fullmatch(right):
            return Decimal(left) == Decimal(right)      # the evaluator prints minus zero as -0.00
        return left.rstrip() == right.rstrip()
    return left == right


def runEndKind(value):
    return "normal" if str(value).startswith("normal end") else "abend"


def laneValues(lane, expectation, stoppedOnly=False):
    """{path: value} of one lane, on the paths the expectation and the comparison use.

    stoppedOnly: the three values of a run that stopped - how it ended, the return code and the job
    log up to ABENDING PROGRAM. The files a stopped run leaves behind are not values of the comparison."""
    if stoppedOnly:
        return {("run", "end"): lane["runEnd"], ("run", "returnCode"): lane.get("returnCode"),
                ("run", "sysout"): lane.get("jobLog", lane["sysout"])}
    values = {("run", "end"): lane["runEnd"], ("run", "sysout"): lane["sysout"],
              ("run", "returnCodes"): lane["returnCodes"],
              ("run", "transactionCount"): len(lane["transactions"])}
    written = iter(lane["transactions"])
    used = 0
    for balance in expectation["balances"]:
        key = balance["key"]
        accountId = key.split("/")[0]
        transaction = None
        if used < len(lane["transactions"]) and lane["transactions"][used]["TRAN-DESC"].endswith(accountId):
            transaction = next(written)
            used += 1
        values[("balance", key, "interestTransactionWritten")] = transaction is not None
        for field in transactionFields:
            values[("balance", key, field)] = transaction[field] if transaction else None
    for index, transaction in enumerate(lane["transactions"][used:], used + 1):
        for field in transactionFields:
            values[("extraTransaction", f"record {index}", field)] = transaction[field]
    for account in lane["accounts"]:
        for field in accountFields:
            values[("account", account["ACCT-ID"], field)] = account[field]
    return values


def expectationCells(expectation):
    cells = {("run", "end"): expectation["run"]["end"], ("run", "sysout"): expectation["run"]["sysout"]}
    for balance in expectation["balances"]:
        for field, cell in balance.items():
            if field not in ("key", *derivedOnly):
                cells[("balance", balance["key"], field)] = cell
    for account in expectation["accounts"]:
        for field, cell in account.items():
            if field != "ACCT-ID":
                cells[("account", account["ACCT-ID"], field)] = cell
    return cells


def pathLabel(path):
    if path[0] == "balance":
        return f"balance {path[1]}", path[2]
    if path[0] == "account":
        return f"account {path[1]}", path[2]
    if path[0] == "extraTransaction":
        return f"transaction {path[1]} (no balance of the input explains it)", path[2]
    if path[0] == "golden":
        return f"golden file: {path[1]}", path[2]
    return "run", {"end": "how the run ended", "sysout": "job log (SYSOUT INTCALC.STEP15)",
                   "returnCodes": "return codes (rc.json)", "returnCode": "return code of the step",
                   "transactionCount": "number of interest transactions"}[path[1]]


def show(value):
    if value is None:
        return "(none)"
    if isinstance(value, bool):
        return "yes" if value else "no"
    if isinstance(value, list):
        return "\n".join(str(v) for v in value)
    if isinstance(value, dict):
        return json.dumps(value, sort_keys=True)
    return str(value)


# ------------------------------------------------------------------ classification
def undeterminedBy(expectation, decisions):
    """Open questions that leave this case undetermined by the rulebook (INT-H03, INT-H05)."""
    return [u["rule"] for u in expectation.get("unresolved", [])
            if decisions.get(u["rule"], {}).get("status") != "APPROVED"]


def classify(observed, target, cell, decisions, path=None, undetermined=()):
    """Class of one compared value. Returns (class, rule ids behind it, reason, engines agree)."""
    if path == ("run", "end"):
        agree = runEndKind(observed) == runEndKind(target) and (runEndKind(observed) == "abend" or observed == target)
    else:
        agree = sameValue(observed, target)
    openRules = openRulesOf(cell) if cell else []
    stillOpen = [r for r in openRules if decisions.get(r, {}).get("status") != "APPROVED"]
    approved = [r for r in openRules if decisions.get(r, {}).get("status") == "APPROVED"]
    if agree:
        if stillOpen:
            return "UNRESOLVED", stillOpen, "both engines agree, on behaviour whose decision is still open", True
        return "MATCH", [], "observed and target agree", True
    if cell and cell["status"] == "OPEN":
        candidateValues = {c["rule"]: [] for c in cell["candidates"]}
        for candidate in cell["candidates"]:
            candidateValues[candidate["rule"]].append(candidate)
        asCoded = next(c["value"] for c in cell["candidates"] if c["asCoded"])
        for ruleId in approved:
            option = decisions[ruleId]["approvedOption"]
            wanted = [c["value"] for c in candidateValues[ruleId] if c["option"] == option]
            if wanted and sameValue(target, wanted[0]) and sameValue(observed, asCoded):
                return "INTENDED_CHANGE", [ruleId], f"decision {ruleId} is approved with option {option}", False
        for ruleId in stillOpen:
            values = [c["value"] for c in candidateValues[ruleId]]
            if any(sameValue(observed, v) for v in values) and any(sameValue(target, v) for v in values):
                return ("UNRESOLVED", [ruleId],
                        f"the engines implement different options of {ruleId}, which is open", False)
    if undetermined:
        return ("UNRESOLVED", list(undetermined),
                f"the source does not determine the outcome of this case and {', '.join(undetermined)} is open: "
                "the engines give different answers to it, or this value follows from that", False)
    return "UNCLASSIFIED", [], "the engines differ and no decision of the register explains it", False


def compareLanes(expectation, legacy, target, decisions, goldenDiffs=None):
    cells = expectationCells(expectation)
    undetermined = undeterminedBy(expectation, decisions)
    bothStopped = bool(legacy.get("stopped") and target.get("stopped"))
    observedValues = laneValues(legacy, expectation, bothStopped)
    targetValues = laneValues(target, expectation, bothStopped)
    paths = list(observedValues) + [p for p in targetValues if p not in observedValues]
    items = []
    for path in paths:
        observed, targetValue = observedValues.get(path), targetValues.get(path)
        cell = None if undetermined else cells.get(path)
        if path[0] == "balance" and path[2] != "interestTransactionWritten" and observed is None \
                and targetValue is None:
            continue    # no transaction on either side: the written flag carries the comparison
        cls, ruleIds, reason, agree = classify(observed, targetValue, cell, decisions, path, undetermined)
        where, field = pathLabel(path)
        if undetermined:
            expected, expectedStatus = "(the rulebook does not determine this case)", "UNRESOLVED"
        elif cell:
            expected, expectedStatus = show(requiredValue(cell, decisions)), cell["status"]
        else:
            expected, expectedStatus = "(the rulebook states no value)", "NONE"
        items.append({"where": where, "field": field, "observed": show(observed), "target": show(targetValue),
                      "expected": expected, "expectedStatus": expectedStatus,
                      "class": cls, "agree": agree, "decisions": ruleIds, "reason": reason,
                      "rules": cell["rules"] if cell else [],
                      "options": [{"rule": c["rule"], "option": c["option"], "asCoded": c["asCoded"],
                                   "value": show(c["value"])} for c in (cell or {}).get("candidates", [])]})
    for entry in expectation.get("unresolved", []):
        sameEnd = runEndKind(legacy["runEnd"]) == runEndKind(target["runEnd"])
        items.append({"where": "case", "field": f"outcome not determined by the rulebook ({entry['rule']})",
                      "observed": legacy["runEnd"], "target": target["runEnd"], "expected": entry["reason"],
                      "expectedStatus": "UNRESOLVED", "class": "UNRESOLVED", "agree": sameEnd,
                      "decisions": [entry["rule"]], "options": [],
                      "reason": f"{entry['reason']}: the source does not determine the outcome and {entry['rule']} is open",
                      "rules": [entry["rule"]]})
    for diff in goldenDiffs or []:
        items.append({"where": f"golden file: {diff['where']}", "field": diff["field"], "observed": diff["actual"],
                      "target": "(not compared)", "expected": diff["expected"], "expectedStatus": "GOLDEN",
                      "class": "UNCLASSIFIED", "agree": False, "decisions": [], "rules": [], "options": [],
                      "reason": "the legacy rerun differs from the golden file of the fixture"})
    return items


# ------------------------------------------------------------------ rule verdicts
def inOrder(wanted, lines):
    """True when the lines of `wanted` appear in `lines` in that order; also the lines in between."""
    position, extra = 0, []
    for line in lines or []:
        if position < len(wanted) and line.rstrip() == wanted[position].rstrip():
            position += 1
        else:
            extra.append(line)
    return position == len(wanted), extra


def judgeEngine(expectation, lane, decisions, rulebook):
    """CONFORMS or VIOLATES per rule for one engine, against the rulebook alone.

    NOT EXERCISED  the case does not reach the rule.
    NOT DETERMINED the source does not determine the outcome of the case (INT-H03, INT-H05): nothing is judged.
    NOT JUDGED     the run stopped: only how it ended and its job log are judged; a rule that shows in
                   the files alone cannot be judged, because the files of a stopped run are not compared.
    Returns (verdict rows, violations with no rule to blame, job log lines the rulebook does not list)."""
    ruleIds = [r["id"] for r in rulebook["rules"]]
    exercised = set(expectation["rulesExercised"])
    if undeterminedBy(expectation, decisions) or expectation.get("unresolved"):
        return [{"rule": r, "verdict": "NOT DETERMINED" if r in exercised else "NOT EXERCISED", "evidence": []}
                for r in ruleIds], [], []
    cells = expectationCells(expectation)
    stopped = bool(lane.get("stopped"))
    values = laneValues(lane, expectation, stopped)
    if stopped:
        cells = {path: cell for path, cell in cells.items() if path in (("run", "end"), ("run", "sysout"))}
    judged, violations, notInRulebook = set(), {}, []

    def blame(ids, where, field, required, got):
        for ruleId in ids:
            violations.setdefault(ruleId, []).append(
                {"where": where, "field": field, "required": show(required), "got": show(got)})

    for path, cell in cells.items():
        required, got = requiredValue(cell, decisions), values.get(path)
        judged.update(r for r in cell["rules"] if r in ruleIds)
        if path == ("run", "end"):
            conforms = runEndKind(required) == runEndKind(got)
        elif path == ("run", "sysout") and stopped:
            # the evaluator of the rulebook does not write the FILE STATUS line of 9910-DISPLAY-IO-STATUS
            # (INT-T08): its lines must be there in order; what the engine writes besides is listed
            conforms, notInRulebook = inOrder(required, got)
        else:
            conforms = sameValue(required, got)
        if conforms:
            continue
        cited = [r for r in cell["rules"] if r in ruleIds]
        # a value that is another option of one open rule breaks that rule, not every rule the value cites
        optionOf = sorted({c["rule"] for c in cell.get("candidates", []) if sameValue(c["value"], got)})
        where, field = pathLabel(path)
        blame(optionOf or cited or ["(technical note)"], where, field, required, got)
        exercised.update(optionOf or cited)
    for path, value in values.items():
        if path[0] == "extraTransaction" and path[2] == "TRAN-ID":
            where, field = pathLabel(path)
            blame(["INT-R01", "INT-H01"], where, field, None, value)
    verdicts = []
    for ruleId in ruleIds:
        if ruleId in violations:
            verdict = "VIOLATES"
        elif ruleId not in exercised:
            verdict = "NOT EXERCISED"
        elif stopped and ruleId not in judged:
            verdict = "NOT JUDGED"
        else:
            verdict = "CONFORMS"
        verdicts.append({"rule": ruleId, "verdict": verdict, "evidence": violations.get(ruleId, [])})
    return verdicts, violations.get("(technical note)", []), notInRulebook


verdictRank = ["VIOLATES", "CONFORMS", "NOT DETERMINED", "NOT JUDGED", "NOT EXERCISED"]


def ruleVerdicts(expectation, legacy, target, decisions, rulebook):
    """Verdict rows of one run for both engines."""
    legacyVerdicts, legacyNotes, _ = judgeEngine(expectation, legacy, decisions, rulebook)
    targetVerdicts, targetNotes, _ = judgeEngine(expectation, target, decisions, rulebook)
    byId = {r["id"]: r for r in rulebook["rules"]}
    rows = []
    for legacyRow, targetRow in zip(legacyVerdicts, targetVerdicts):
        rule = byId[legacyRow["rule"]]
        decision = decisions.get(rule["id"])
        rows.append({"rule": rule["id"], "type": rule["type"], "title": rule["title"],
                     "decision": decision["status"] if decision else None,
                     "legacy": legacyRow["verdict"], "target": targetRow["verdict"],
                     "legacyEvidence": legacyRow["evidence"], "targetEvidence": targetRow["evidence"]})
    return rows, {"legacy": legacyNotes, "target": targetNotes}


def mergeVerdicts(perRun):
    """One row per rule over the runs of a case: the worst verdict wins, the evidence says which run."""
    merged = [dict(row) for row in perRun[0]]
    for number, rows in enumerate(perRun[1:], 2):
        for row, later in zip(merged, rows):
            for engine in ("legacy", "target"):
                if verdictRank.index(later[engine]) < verdictRank.index(row[engine]):
                    row[engine] = later[engine]
                row[f"{engine}Evidence"] = row[f"{engine}Evidence"] + [
                    {**e, "where": f"run {number}: {e['where']}"} for e in later[f"{engine}Evidence"]]
    return merged


# ------------------------------------------------------------------ open questions shown by a probe
openQuestionNotes = {
    "INT-H02": "Open question, not a rule: nothing in the job stops a second run in the same cycle. The lab "
               "shows what each engine does today when the job is run twice on the files the first run left. "
               "That is what the engines do, not what is right; what is right waits for the business answer to INT-H02.",
    "INT-H03": "Open question, not a rule: the source does not say which card number an account with several "
               "cards gets, or whether the run stops. The lab shows what each engine does today. That is what the "
               "engines do, not what is right. The legacy side here is GnuCOBOL: on the read of the cross-reference "
               "by account id it may differ from the host, where a duplicate key can come back with another file "
               "status and stop the program. Only a host run or the business answer to INT-H03 settles it.",
    "INT-H05": "Open question, not a rule: the result of an overflow depends on compiler options the estate does "
               "not show. What the engines do here is not what is right.",
}
# open questions a replay cannot exercise at all, with what each waits for
notReplayable = {
    "INT-H05": "needs host evidence: the result of an overflow depends on compiler options the estate does not show",
    "INT-H06": "needs an answer from operations: the order of posting, the file reloads and the interest run "
               "is outside a single job",
}


def describeRuns(lanes):
    parts = []
    for number, lane in enumerate(lanes, 1):
        total = sum((Decimal(t["TRAN-AMT"]) for t in lane["transactions"]), Decimal(0))
        cards = sorted({t["TRAN-CARD-NUM"] for t in lane["transactions"]})
        last = (lane.get("jobLog") or ["(no job log)"])[-1]
        parts.append((f"run {number}: " if len(lanes) > 1 else "") + f"{lane['runEnd']}; "
                     f"{len(lane['transactions'])} interest transaction(s) totalling {total:.2f}"
                     + (f" on card(s) {', '.join(cards)}" if cards else "") + f"; job log ends '{last}'")
    return "; ".join(parts)


# ------------------------------------------------------------------ one case
def caseRefFor(kind, name=None, spec=None):
    if kind == "golden":
        if name not in cases.goldenCases():
            raise cases.InvalidCase([f"'{name}' is not a golden case"])
        return {"kind": kind, "name": name, "casesRoot": cases.fixturesRoot, "runs": 1,
                "caseDir": cases.goldenDir / name, "title": name, "note": cases.goldenNote(name)}
    if kind == "probe":
        probe = next((p for p in cases.loadProbes() if p["name"] == name), None)
        if probe is None:
            raise cases.InvalidCase([f"'{name}' is not an edge probe"])
        return {"kind": kind, "name": name, "casesRoot": cases.labCasesRoot, "runs": probe.get("runs", 1),
                "caseDir": cases.materialise(name, probe), "title": probe["title"], "note": probe["stresses"]}
    name = "custom-" + uuid.uuid4().hex[:8]
    return {"kind": "custom", "name": name, "casesRoot": cases.labCasesRoot, "runs": spec.get("runs", 1),
            "caseDir": cases.materialise(name, spec), "title": "Custom input", "note": "Typed into the lab page."}


def inputRecords(caseRef):
    layouts = {"AWS.M2.CARDDEMO.ACCTDATA.PS": "CVACT01Y", "AWS.M2.CARDDEMO.TCATBALF.PS": "CVTRA01Y",
               "AWS.M2.CARDDEMO.DISCGRP.PS": "CVTRA02Y", "AWS.M2.CARDDEMO.CARDXREF.PS": "CVACT03Y"}
    decoded = {}
    for dsn, layout in layouts.items():
        fields = {f.name: f for f in parse_copybook(layout)}
        path = caseRef["caseDir"] / "input" / f"{dsn}.txt"
        lines = path.read_text(encoding="latin-1").splitlines() if path.exists() else []
        decoded[dsn] = [{n: showField(fields[n], v) for n, v in
                         decode_record(layout, line.ljust(chain["inputs"][dsn]).encode("latin-1")).items()}
                        for line in lines]
    return decoded


def expectedNextRun(caseRef, expectation, decisions, rulebook, number):
    """The rulebook's expectation of a further run: the evaluator on the same inputs, with the account
    file as the rulebook says the run before left it (as coded). None when that run was to stop."""
    if runEndKind(requiredValue(expectation["run"]["end"], decisions)) != "normal" or expectation.get("unresolved"):
        return None
    after = {a["ACCT-ID"]: {f: requiredValue(c, decisions) for f, c in a.items() if f != "ACCT-ID"}
             for a in expectation["accounts"]}
    nextCase = cases.labCasesRoot / "interest" / f"{caseRef['name']}.expected-run{number}"
    if nextCase.exists():
        shutil.rmtree(nextCase)
    shutil.copytree(caseRef["caseDir"] / "input", nextCase / "input")
    accountFile = nextCase / "input" / "AWS.M2.CARDDEMO.ACCTDATA.PS.txt"
    fields = {f.name: f for f in parse_copybook("CVACT01Y")}
    lines = []
    for line in accountFile.read_text(encoding="latin-1").splitlines():
        record = decode_record("CVACT01Y", line.ljust(300).encode("latin-1"))
        record.update(after.get(showField(fields["ACCT-ID"], record["ACCT-ID"]), {}))
        lines.append(encode_record("CVACT01Y", record).rstrip(b" ") + b"\n")
    accountFile.write_bytes(b"".join(lines))
    return json.loads(evaluate.render(evaluate.expectation(rulebook, nextCase)))


def isTieCase(expectations):
    """True when half-even rounding gives another value than cutting off somewhere in the case (INT-Q01 A against C)."""
    for expectation in expectations:
        for cell in expectationCells(expectation).values():
            byOption = {c["option"]: c["value"] for c in cell.get("candidates", []) if c["rule"] == "INT-Q01"}
            if "A" in byOption and "C" in byOption and not sameValue(byOption["A"], byOption["C"]):
                return True
    return False


def runCase(caseRef, runId=None):
    """Both lanes, the classification and the rule verdicts of one case."""
    runId = runId or uuid.uuid4().hex[:10]
    rulebook, decisions = loadRulebook(), loadDecisions()
    runs = caseRef.get("runs", 1)
    expectation, expectationSource = expectationFor(caseRef, rulebook)
    expectations = [expectation]
    for number in range(2, runs + 1):
        following = expectedNextRun(caseRef, expectations[-1], decisions, rulebook, number)
        if following is None:
            break
        expectations.append(following)
    runDir = runsDir / runId
    workDir.mkdir(parents=True, exist_ok=True)
    with open(workDir / ".lock", "w") as lock:      # datasets/ and the replica tables are shared: one run at a time
        fcntl.flock(lock, fcntl.LOCK_EX)
        if runDir.exists():
            shutil.rmtree(runDir)
        legacyRuns = runLegacy(caseRef, runDir / "legacy", runs)
        targetRuns = runTarget(caseRef, runDir / "target", runId, runs)
    goldenDiffs, goldenParity = None, None
    if caseRef["kind"] == "golden":
        goldenDiffs, checked = compare.compare_case("interest", caseRef["name"], runDir / "legacy")
        goldenParity = {"verdict": "PASS" if not goldenDiffs else "FAIL", "differences": len(goldenDiffs),
                        "records": checked["records"], "fields": checked["fields"]}
    items, perRun, technical, notes = [], [], {"legacy": [], "target": []}, []
    exercised = set()
    secondRunOpen = runs > 1 and decisions.get("INT-H02", {}).get("status") != "APPROVED"
    for number, (expected, legacy, target) in enumerate(zip(expectations, legacyRuns, targetRuns), 1):
        runItems = compareLanes(expected, legacy, target, decisions, goldenDiffs if number == 1 else None)
        for item in runItems:
            if runs > 1:
                item["where"] = f"run {number}: {item['where']}"
            if number > 1 and secondRunOpen:
                # everything a second run does stands under INT-H02: should it have run at all?
                if item["class"] == "MATCH":
                    item.update({"class": "UNRESOLVED", "decisions": ["INT-H02"],
                                 "reason": "both engines agree on what a second run does; whether a second run "
                                           "may charge again is INT-H02, which is open"})
                elif item["class"] == "UNRESOLVED" and "INT-H02" not in item["decisions"]:
                    item["decisions"] = item["decisions"] + ["INT-H02"]
        items += runItems
        rows, notesOfRun = ruleVerdicts(expected, legacy, target, decisions, rulebook)
        perRun.append(rows)
        for engine in ("legacy", "target"):
            technical[engine] += notesOfRun[engine]
        exercised.update(expected["rulesExercised"])
        prefix = f"Run {number}: " if runs > 1 else ""
        if legacy.get("stopped") and target.get("stopped"):
            notes.append(prefix + "both runs stopped. Compared: how the run ended, the return code and the job log up to "
                         f"'{abendLine}'. Return code: legacy {legacy['returnCode']} ({legacy['returnCodeSource']}; 12 is "
                         f"the value of the off-host abend stub, U0999 on the host), target {target['returnCode']} "
                         f"({target['returnCodeSource']}).")
            notes.append(prefix + "not compared - the files a stopped run leaves behind (a restart is INT-H02): legacy left "
                         f"{len(legacy['transactions'])} interest transaction(s) and an account file of "
                         f"{len(legacy['accounts'])} record(s); target exported "
                         + (f"{len(target['transactions'])} transaction(s) and {len(target['accounts'])} account record(s)."
                            if target["transactionsWritten"] or target["accountsWritten"] else "no file."))
            if legacy["afterAbend"]:
                notes.append(prefix + f"not compared - {len(legacy['afterAbend'])} line(s) of the legacy job log after "
                             f"'{abendLine}': the line of the off-host abend stub and warnings of the GnuCOBOL runtime.")
            extra = judgeEngine(expected, legacy, decisions, rulebook)[2]
            if extra:
                notes.append(prefix + "job log line(s) both engines write that the rulebook's expectation does not list: "
                             + "; ".join(f"'{line}'" for line in extra) + ". The rulebook's own lines are all there, in "
                             "order, so the lab does not count this as a violated rule; it is a gap of the rulebook's "
                             "evaluator (INT-T08 describes the line).")
    if len(legacyRuns) != len(targetRuns) or len(expectations) != runs:
        items.append({"where": "case", "field": "number of runs", "observed": str(len(legacyRuns)),
                      "target": str(len(targetRuns)), "expected": str(len(expectations)), "expectedStatus": "NONE",
                      "class": "UNCLASSIFIED", "agree": False, "decisions": [], "rules": [], "options": [],
                      "reason": "a further run could not be made in one engine or is not expected by the rulebook"})
    verdictRows = mergeVerdicts(perRun)
    openQuestions = []
    if runs > 1:
        exercised.add("INT-H02")
        row = next(r for r in verdictRows if r["rule"] == "INT-H02")
        for engine, lanes in (("legacy", legacyRuns), ("target", targetRuns)):
            # as coded (option A) a second run charges again; an engine that does not is not as coded
            again = len(lanes) == runs and len(expectations) == runs and \
                len(lanes[-1]["transactions"]) == sum(1 for b in expectations[-1]["balances"]
                                                       if requiredValue(b["interestTransactionWritten"], decisions))
            row[engine] = "CONFORMS" if again else "VIOLATES"
            if not again:
                row[f"{engine}Evidence"] = row[f"{engine}Evidence"] + [
                    {"where": f"run {runs}", "field": "interest transactions", "got": describeRuns(lanes),
                     "required": "as coded, the second run charges every balance again"}]
        notes.append("Second run of the target: the Java job reloads its tables from the files on every run, so the lab "
                     "gives the account file its first run exported to its second run (the write-back of REL-03). "
                     "The legacy job reads the file its first run rewrote. Generation names of SYSTRAN are not compared.")
    for ruleId in sorted({"INT-H02"} & exercised | set(u["rule"] for e in expectations for u in e.get("unresolved", []))):
        if decisions.get(ruleId, {}).get("status") != "APPROVED":
            openQuestions.append({"rule": ruleId, "note": openQuestionNotes.get(ruleId, "Open question, not a rule."),
                                  "legacy": describeRuns(legacyRuns), "target": describeRuns(targetRuns)})
    counts = {c: sum(1 for i in items if i["class"] == c) for c in classes}
    unresolved = {"agree": sum(1 for i in items if i["class"] == "UNRESOLVED" and i["agree"]),
                  "differ": sum(1 for i in items if i["class"] == "UNRESOLVED" and not i["agree"])}
    legacyLane, targetLane = dict(legacyRuns[0]), dict(targetRuns[0])
    legacyLane["laterRuns"], targetLane["laterRuns"] = legacyRuns[1:], targetRuns[1:]
    return {"runId": runId, "kind": caseRef["kind"], "case": caseRef["name"], "title": caseRef["title"],
            "note": caseRef["note"], "expectationSource": expectationSource, "runs": runs,
            "expectationStatus": expectations[0]["status"], "rulesExercised": sorted(exercised),
            "input": inputRecords(caseRef), "legacy": legacyLane, "target": targetLane, "goldenParity": goldenParity,
            "items": items, "counts": counts, "unresolved": unresolved, "compared": len(items),
            "ruleVerdicts": verdictRows, "technicalViolations": technical, "notes": notes,
            "openQuestions": openQuestions, "tieCase": isTieCase(expectations), "targetArgs": targetArgs(),
            "violations": {engine: sorted(r["rule"] for r in verdictRows if r[engine] == "VIOLATES")
                           for engine in ("legacy", "target")},
            "gate": "PASS" if counts["UNCLASSIFIED"] == 0 and not any(
                r[engine] == "VIOLATES" for r in verdictRows for engine in ("legacy", "target")) else "FAIL"}


# ------------------------------------------------------------------ gate and report
def summarise(results, rulebook=None):
    """Totals over a list of case results: class counts, rule coverage, violations, the gate.

    The gate passes only when no compared value is UNCLASSIFIED and neither engine VIOLATES a rule
    in any case. `failures` names what failed: the case, and for a violation the engine and the rule."""
    rulebook = rulebook or loadRulebook()
    ruleIds = [r["id"] for r in rulebook["rules"]]
    counts = {c: sum(r["counts"][c] for r in results) for c in classes}
    unresolved = {kind: sum(unresolvedOf(r)[kind] for r in results) for kind in ("agree", "differ")}
    exercised = sorted({ruleId for r in results for ruleId in r["rulesExercised"]})
    violations = {engine: sorted({ruleId for r in results for ruleId in r["violations"][engine]})
                  for engine in ("legacy", "target")}
    failures = [{"condition": "UNCLASSIFIED", "case": r["case"], "count": r["counts"]["UNCLASSIFIED"]}
                for r in results if r["counts"]["UNCLASSIFIED"]]
    failures += [{"condition": "VIOLATES", "case": r["case"], "engine": engine, "rule": ruleId}
                 for r in results for engine in ("legacy", "target") for ruleId in r["violations"][engine]]
    replayable = [r for r in ruleIds if r not in notReplayable]
    return {"cases": len(results), "compared": sum(r["compared"] for r in results), "counts": counts,
            "unresolved": unresolved, "rulesExercised": exercised, "rulesTotal": len(ruleIds),
            "rulesReplayable": len(replayable),
            "rulesExercisedReplayable": [r for r in replayable if r in exercised],
            "rulesNotExercised": [r for r in replayable if r not in exercised],
            "notReplayable": [{"rule": r, "waitsFor": reason} for r, reason in notReplayable.items() if r in ruleIds],
            "violations": violations, "failures": failures, "gate": "PASS" if not failures else "FAIL"}


def unresolvedOf(result):
    return result.get("unresolved") or {"agree": result["counts"]["UNRESOLVED"], "differ": 0}


def countsLine(counts, unresolved=None):
    unresolved = unresolved or {"agree": counts["UNRESOLVED"], "differ": 0}
    return "  ".join(f"{c} {counts[c]}" + (f" (engines agree {unresolved['agree']}, engines differ "
                                           f"{unresolved['differ']})" if c == "UNRESOLVED" else "") for c in classes)


def coverageLines(summary, title="rule coverage"):
    """Two lines: the rules a replay can exercise, and the open questions no case can settle."""
    missing = summary["rulesNotExercised"]
    return [f"{title}: {len(summary['rulesExercisedReplayable'])} of {summary['rulesReplayable']} rules a replay can "
            f"exercise were exercised by these cases" + (f"; not exercised: {', '.join(missing)}" if missing else ""),
            f"open questions no case can settle: {len(summary['notReplayable'])} - "
            + "; ".join(f"{n['rule']} ({n['waitsFor']})" for n in summary["notReplayable"])]


def gateConditionLines(summary):
    """Which condition of the gate failed, with the case, the engine and the rule."""
    unclassified = [f for f in summary["failures"] if f["condition"] == "UNCLASSIFIED"]
    violated = [f for f in summary["failures"] if f["condition"] == "VIOLATES"]
    lines = ["gate condition 1, no compared value is UNCLASSIFIED: "
             + ("passed" if not unclassified else f"FAILED ({summary['counts']['UNCLASSIFIED']} values)")]
    lines += [f"  UNCLASSIFIED: case {f['case']}, {f['count']} value(s)" for f in unclassified]
    lines.append("gate condition 2, neither engine VIOLATES a rule in any case: "
                 + ("passed" if not violated else f"FAILED ({len(violated)} violation(s))"))
    lines += [f"  VIOLATES: case {f['case']}, engine {f['engine']}, rule {f['rule']}" for f in violated]
    return lines


def gateLines(sections):
    """The text of `make lab-gate`. sections: [(title, [results])]."""
    lines = ["lab-gate: replay of the interest stream (INTCALC -> CBACT04C against java/interest)"]
    if targetArgs():
        lines.append(f"target options (LAB_TARGET_ARGS): {' '.join(targetArgs())}")
    everything = []
    for title, results in sections:
        for result in results:
            violates = "; ".join(f"{engine} VIOLATES {', '.join(result['violations'][engine])}"
                                 for engine in ("legacy", "target") if result["violations"][engine])
            lines.append(f"{title:<6} {result['case']:<28} {countsLine(result['counts'], unresolvedOf(result))}"
                         + (f"  [{violates}]" if violates else ""))
        summary = summarise(results)
        lines.append(f"{title} total: {summary['cases']} cases, {summary['compared']} compared values: "
                     f"{countsLine(summary['counts'], summary['unresolved'])}")
        lines.append(coverageLines(summary, f"{title} rule coverage")[0])
        everything += results
    total = summarise(everything)
    if len(sections) > 1:
        lines.append(f"all total: {total['cases']} cases, {total['compared']} compared values: "
                     f"{countsLine(total['counts'], total['unresolved'])}")
    lines += coverageLines(total)
    for engine in ("legacy", "target"):
        lines.append(f"rule verdicts, {engine}: "
                     + (f"VIOLATES {', '.join(total['violations'][engine])}" if total["violations"][engine]
                        else "no rule violated in any case"))
    lines += gateConditionLines(total)
    lines.append(f"gate: {total['gate']}")
    return lines, total


def cellText(text):
    return str(text).replace("|", "\\|").replace("\n", "<br>")


def reportMarkdown(sections, gateText, meta):
    lines = ["# Classified report - replay lab, interest stream", "",
             "Written by `make lab-gate` (`lab/replay.py gate`). Observed is the legacy COBOL run, target is the Java job of "
             "`java/interest`, expected is what the rulebook derives. Values are decoded with the copybook codec of "
             "`tools/parity`.", "",
             "> **Not SME sign-off.** Every decision of `modernization/rules/interest/decisions.md` is a question an agent "
             "raised from the code. A value classed UNRESOLVED is waiting for one of them.", "",
             "| | |", "|---|---|"]
    lines += [f"| {key} | {cellText(value)} |" for key, value in meta]
    lines += ["", "## Gate", "", "```", *gateText, "```", "",
              "## What the lab does not prove", "", *limitsMarkdown(), ""]
    for title, results in sections:
        for result in results:
            lines += [f"## {title}: {result['case']}", "", result["note"].replace("\n", " "), "",
                      f"Expected from: {result['expectationSource']} (status {result['expectationStatus']}). "
                      f"Compared values: {result['compared']} - "
                      f"{countsLine(result['counts'], unresolvedOf(result))}. "
                      + (f"Legacy rerun against the golden files: {result['goldenParity']['verdict']} "
                         f"({result['goldenParity']['fields']} decoded fields). " if result["goldenParity"]
                         else "No golden file: the observed side is this run of the legacy lane. ")
                      + f"Legacy: {result['legacy']['runEnd']}; target: {result['target']['runEnd']}.", ""]
            for question in result.get("openQuestions", []):
                lines += [f"> **{question['rule']} - what the engines do today, not what is right.** {question['note']}",
                          f"> Legacy today: {cellText(question['legacy'])}.",
                          f"> Target today: {cellText(question['target'])}.", ""]
            lines += [f"- {cellText(note)}" for note in result.get("notes", [])] + ([""] if result.get("notes") else [])
            shown = [i for i in result["items"] if i["class"] != "MATCH"]
            if shown:
                lines += ["| Class | Where | Field | Observed (legacy) | Target (Java) | Rulebook requires | Decision | Why |",
                          "|---|---|---|---|---|---|---|---|"]
                lines += [f"| {i['class']}{unresolvedKind(i)} | {i['where']} | `{i['field']}` | {cellText(i['observed'])} | "
                          f"{cellText(i['target'])} | {cellText(i['expected'])} | {', '.join(i['decisions']) or '-'} | "
                          f"{cellText(i['reason'])} |" for i in shown]
            else:
                lines.append("Every compared value is MATCH.")
            lines += ["", "| Rule | Type | Legacy | Target |", "|---|---|---|---|"]
            lines += [f"| {r['rule']} {cellText(r['title'])} | {r['type']} | {r['legacy']} | {r['target']} |"
                      for r in result["ruleVerdicts"] if r["legacy"] != "NOT EXERCISED" or r["target"] != "NOT EXERCISED"]
            for engine in ("legacy", "target"):
                for row in result["ruleVerdicts"]:
                    for evidence in row[f"{engine}Evidence"]:
                        lines.append(f"- {engine} VIOLATES {row['rule']}: {evidence['where']} `{evidence['field']}` is "
                                     f"{cellText(evidence['got'])}, the rulebook requires {cellText(evidence['required'])}")
            lines.append("")
    return "\n".join(lines) + "\n"


def unresolvedKind(item):
    if item["class"] != "UNRESOLVED":
        return ""
    return " - engines agree" if item.get("agree", True) else " - engines differ"


def limitsMarkdown():
    return [f"- {text}" for text in limits]


limits = [
    "Sequential batch replay only: one clean run of INTCALC per case on small synthetic inputs. No restart after a "
    "failure and no volume; the one rerun (probe p10) is the job a second time on its own files, nothing more.",
    "No CICS: the online programs that read and write the same account file are not run.",
    "No concurrency: one run at a time; nothing else touches the files or the tables during a run.",
    "GnuCOBOL arithmetic, not Enterprise COBOL: the observed side is GnuCOBOL 3 with -std=ibm; intermediate results, "
    "truncation and overflow under the host compiler options are not shown.",
    "No real Db2 and no VSAM: indexed files are GnuCOBOL files, the target database is PostgreSQL 15 in a container.",
    "ASCII with zoned signs as the off-host harness writes them, not cp037 host files; a frozen clock.",
    "A rule verdict is a check against the rulebook's own evaluator, which an agent wrote from the code; "
    "it is not a business owner's view. Rules no case exercised have no verdict.",
    "A run that stops is compared on how it ended, its return code and its job log only. The files it leaves "
    "behind are not compared, and neither is a restart.",
    "A probe of an open question (INT-H02 second run, INT-H03 several cards) shows what each engine does today, "
    "not what is right. For INT-H03 the legacy side is GnuCOBOL, which may differ from the host on the "
    "duplicate-key read.",
]


def gitValue(*args):
    result = subprocess.run(["git", *args], cwd=repoRoot, capture_output=True, text=True)
    return result.stdout.strip() if result.returncode == 0 else "unknown"


def baselineInfo():
    """The estate baseline of the stream manifest, and whether app/ and fixtures/ still equal it."""
    manifest = (repoRoot / "modernization" / "streams" / "interest" / "manifest.json").read_text()
    found = re.search(r'"sha":\s*"([0-9a-f]{40})"', manifest)
    if not found:
        return "unknown", "not checked"
    sha = found.group(1)
    diff = subprocess.run(["git", "diff", "--quiet", sha, "HEAD", "--", "app", "fixtures"], cwd=repoRoot,
                          capture_output=True)
    state = {0: "app/ and fixtures/ unchanged since", 1: "app/ or fixtures/ CHANGED since"}.get(
        diff.returncode, "not checked: the baseline commit is not in this checkout")
    return sha, state


def toolchain():
    def first(command, pattern=None):
        try:
            result = sh(command, timeout=120)
        except (subprocess.TimeoutExpired, OSError):
            return "not available"
        text = (result.stdout + result.stderr).strip()
        if result.returncode != 0 or not text:
            return "not available"
        line = text.splitlines()[0]
        match = re.search(pattern, text) if pattern else None
        return match.group(1) if match else line
    estate = ["docker", "compose", "exec", "-T", "estate"]
    db = "not available"
    try:
        db = first(["docker", "compose", "exec", "-T", "db", "psql", "-U", "carddemo", "-d", "carddemo", "-Atc",
                    "show server_version"])
    except LabError:
        pass
    return [("GnuCOBOL", first(estate + ["cobc", "--version"], r"cobc \(GnuCOBOL\) (\S+)")),
            ("Open COBOL ESQL", first(estate + ["ocesql", "--version"], r"(\d+\.\d+(?:\.\d+)*)")),
            ("Python (estate)", first(estate + ["python3", "--version"], r"Python (\S+)")),
            ("Java (target run)", first(["docker", "run", "--rm", interestJre, "java", "-version"],
                                        r'version "([^"]+)"')),
            ("PostgreSQL", db.split()[0]),
            ("Python (lab)", sys.version.split()[0]),
            ("Docker", first(["docker", "--version"], r"version ([^,\s]+)"))]


def headerInfo():
    params = runParameters()
    return {"toolchain": toolchain(), "baselineSha": baselineInfo()[0], "baselineState": baselineInfo()[1], "labCommit": gitValue("rev-parse", "HEAD"),
            "branch": gitValue("rev-parse", "--abbrev-ref", "HEAD"),
            "businessDate": params["runClock"], "parmDate": params["parmDate"], "targetArgs": targetArgs()}


def runSet(kind, prefix="gate"):
    names = cases.goldenCases() if kind == "golden" else [p["name"] for p in cases.loadProbes()]
    results = []
    for name in names:
        result = runCase(caseRefFor(kind, name), runId=f"{prefix}-{name}")
        print(f"  ran {kind} {name}: {countsLine(result['counts'], result['unresolved'])}", file=sys.stderr, flush=True)
        results.append(result)
    return results


def runSections(which, prefix="gate"):
    sections = []
    if which in ("golden", "all"):
        sections.append(("golden", runSet("golden", prefix)))
    if which in ("probes", "all"):
        sections.append(("probe", runSet("probe", prefix)))
    return sections


def writeOutputs(args, sections, lines, total):
    if args.report:
        header = headerInfo()
        meta = [("Baseline of the estate", f"`{header['baselineSha']}` ({header['baselineState']})"),
                ("Frozen business date", f"{header['businessDate']} (clock of the run), PARM {header['parmDate']}"),
                ("Toolchain", ", ".join(f"{name} {version}" for name, version in header["toolchain"])),
                ("Target options", " ".join(header["targetArgs"]) or "none (the defaults of java/interest)"),
                ("Cases", ", ".join(f"{len(results)} {title}" for title, results in sections))]
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(reportMarkdown(sections, lines, meta))
        print(f"report: {args.report}")
    if args.json:
        args.json.parent.mkdir(parents=True, exist_ok=True)
        args.json.write_text(json.dumps({"sections": [{"title": t, "results": r} for t, r in sections],
                                         "total": total}, indent=1))


def gate(args):
    sections = runSections(args.set)
    lines, total = gateLines(sections)
    print("\n".join(lines))
    writeOutputs(args, sections, lines, total)
    return 0 if total["gate"] == "PASS" else 1


negativeArgs = "--interest.policy.rounding=HALF_EVEN"


def negativeVerdict(results, total):
    """The negative control passes only when the gate fails and the violations are INT-Q01 by the target
    on the tie cases and nowhere else. Returns (passed, lines)."""
    expected = {(r["case"], "target", "INT-Q01") for r in results if r["tieCase"]}
    actual = {(r["case"], engine, ruleId) for r in results for engine in ("legacy", "target")
              for ruleId in r["violations"][engine]}
    problems = []
    if total["gate"] != "FAIL":
        problems.append("the gate passed with half-even rounding")
    if not expected:
        problems.append("no case has a rounding tie, the control cannot show anything")
    problems += [f"missing violation: case {c}, engine {e}, rule {r}" for c, e, r in sorted(expected - actual)]
    problems += [f"unexpected violation: case {c}, engine {e}, rule {r}" for c, e, r in sorted(actual - expected)]
    if total["counts"]["UNCLASSIFIED"]:
        problems.append(f"{total['counts']['UNCLASSIFIED']} UNCLASSIFIED value(s)")
    lines = [f"negative control: tie cases (half-even differs from cutting off): "
             f"{', '.join(sorted(c for c, _, _ in expected)) or 'none'}"]
    lines += [f"negative control problem: {problem}" for problem in problems]
    lines.append("negative control: " + ("PASS - the gate failed, on target VIOLATES INT-Q01 in the tie cases and "
                                         "nowhere else" if not problems else "FAIL"))
    return not problems, lines


def negative(args):
    """The gate with the Java job set to round half-even: it must fail, on INT-Q01 in the tie cases only."""
    os.environ["LAB_TARGET_ARGS"] = negativeArgs
    sections = runSections("all", prefix="negative")
    lines, total = gateLines(sections)
    passed, verdictLines = negativeVerdict([r for _, results in sections for r in results], total)
    print("\n".join(lines))
    print(f"gate exit code would be: {0 if total['gate'] == 'PASS' else 1}")
    print("\n".join(verdictLines))
    writeOutputs(args, sections, lines + verdictLines, total)
    return 0 if passed else 1


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    commands = parser.add_subparsers(dest="command", required=True)
    gateParser = commands.add_parser("gate")
    gateParser.add_argument("--set", choices=["golden", "probes", "all"], default="all")
    negativeParser = commands.add_parser("negative")
    for sub in (gateParser, negativeParser):
        sub.add_argument("--report", type=Path)
        sub.add_argument("--json", type=Path)
    caseParser = commands.add_parser("case")
    caseParser.add_argument("name")
    args = parser.parse_args()
    try:
        if args.command == "gate":
            return gate(args)
        if args.command == "negative":
            return negative(args)
        kind = "golden" if args.name in cases.goldenCases() else "probe"
        print(json.dumps(runCase(caseRefFor(kind, args.name)), indent=1))
        return 0
    except (LabError, cases.InvalidCase) as error:
        print(f"lab: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
