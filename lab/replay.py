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
  python3 lab/replay.py case <name>            one golden case or edge probe, printed as JSON

Every compared value gets one class:
  MATCH            observed and target agree
  INTENDED_CHANGE  they differ and an approved decision in decisions.md says they should
  UNRESOLVED       they differ, or agree on behaviour the rulebook marks OPEN, and the decision is open
  UNCLASSIFIED     any other difference
The gate exits 0 only when UNCLASSIFIED is zero. Independently, each engine is judged against the
rulebook, CONFORMS or VIOLATES per rule, so two engines that agree and both break a rule are seen.
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
from tools.parity.copybook import decode_record, parse_copybook  # noqa: E402

workDir = repoRoot / "work" / "lab"
runsDir = workDir / "runs"
decisionsPath = rulesDir / "decisions.md"
chain = json.loads((repoRoot / "tools" / "runjcl" / "chains.json").read_text())["interest"]
interestJar = "java/interest/target/interest.jar"
interestJre = os.environ.get("INTEREST_JRE", "eclipse-temurin:21-jre")
# cells of the expectation that no output file carries: the rate is only visible through TRAN-AMT
derivedOnly = ("rateGroup", "ratePercent")
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


def runLegacy(caseRef, outDir):
    """Observed side: INTCALC through the JCL runner in the estate container."""
    started = time.time()
    result = sh(["docker", "compose", "exec", "-T", "-e", "PYTHONDONTWRITEBYTECODE=1", "estate", "python3",
                 "lab/legacy_lane.py", str(caseRef["casesRoot"].relative_to(repoRoot)), caseRef["name"],
                 str(outDir.relative_to(repoRoot))])
    lane = decodeLane(outDir, "legacy", "COBOL CBACT04C (GnuCOBOL, JCL runner)")
    lane.update(seconds=round(time.time() - started, 1), processExit=result.returncode,
                log=(result.stdout + result.stderr).strip()[-2000:])
    if result.returncode != 0 and not lane["produced"]:
        raise LabError("the legacy lane did not run: " + (lane["log"] or "no output") +
                       " - is the estate built? (`make lab` builds it)")
    return lane


def runTarget(caseRef, outDir, runId):
    """Target side: the Java job, three steps through PostgreSQL, as `make parity-java` launches it."""
    if not (repoRoot / interestJar).exists():
        raise LabError(f"{interestJar} is missing - `make lab` builds it (mvn package, JDK 21)")
    params = runParameters()
    started = time.time()
    command = ["docker", "run", "--rm", "--network", f"container:{dbContainer()}",
               "--user", f"{os.getuid()}:{os.getgid()}", "-v", f"{repoRoot}:/estate", "-w", "/estate", interestJre,
               "java", "-jar", interestJar, "--spring.profiles.active=database",
               f"--interest.clock.frozen={params['runClock']}", "--logging.level.root=WARN",
               *os.environ.get("LAB_TARGET_ARGS", "").split(),
               f"parmDate={params['parmDate']}", f"runId=lab-{runId}-{time.time_ns()}",      # Spring Batch refuses a second launch with the same parameters
               "format=fixture",
               f"inputDir={(caseRef['caseDir'] / 'input').relative_to(repoRoot)}",
               f"outputDir={outDir.relative_to(repoRoot)}"]
    result = sh(command)
    lane = decodeLane(outDir, "target", "Java interestCalcJob (Spring Batch, PostgreSQL)")
    lane.update(seconds=round(time.time() - started, 1), processExit=result.returncode,
                log=(result.stdout + result.stderr).strip()[-2000:])
    return lane


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


def laneValues(lane, expectation):
    """{path: value} of one lane, on the paths the expectation and the comparison use."""
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
                   "returnCodes": "return codes (rc.json)",
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
def classify(observed, target, cell, decisions, path=None):
    """Class of one compared value. Returns (class, rule ids behind it, reason)."""
    if path == ("run", "end"):
        agree = runEndKind(observed) == runEndKind(target) and (runEndKind(observed) == "abend" or observed == target)
    else:
        agree = sameValue(observed, target)
    openRules = openRulesOf(cell) if cell else []
    stillOpen = [r for r in openRules if decisions.get(r, {}).get("status") != "APPROVED"]
    approved = [r for r in openRules if decisions.get(r, {}).get("status") == "APPROVED"]
    if agree:
        if stillOpen:
            return "UNRESOLVED", stillOpen, "both engines agree, on behaviour whose decision is still open"
        return "MATCH", [], "observed and target agree"
    if cell and cell["status"] == "OPEN":
        candidateValues = {c["rule"]: [] for c in cell["candidates"]}
        for candidate in cell["candidates"]:
            candidateValues[candidate["rule"]].append(candidate)
        asCoded = next(c["value"] for c in cell["candidates"] if c["asCoded"])
        for ruleId in approved:
            option = decisions[ruleId]["approvedOption"]
            wanted = [c["value"] for c in candidateValues[ruleId] if c["option"] == option]
            if wanted and sameValue(target, wanted[0]) and sameValue(observed, asCoded):
                return "INTENDED_CHANGE", [ruleId], f"decision {ruleId} is approved with option {option}"
        for ruleId in stillOpen:
            values = [c["value"] for c in candidateValues[ruleId]]
            if any(sameValue(observed, v) for v in values) and any(sameValue(target, v) for v in values):
                return "UNRESOLVED", [ruleId], f"the engines implement different options of {ruleId}, which is open"
    return "UNCLASSIFIED", [], "the engines differ and no decision of the register explains it"


def compareLanes(expectation, legacy, target, decisions, goldenDiffs=None):
    cells = expectationCells(expectation)
    observedValues, targetValues = laneValues(legacy, expectation), laneValues(target, expectation)
    paths = list(observedValues) + [p for p in targetValues if p not in observedValues]
    items = []
    for path in paths:
        observed, targetValue = observedValues.get(path), targetValues.get(path)
        cell = cells.get(path)
        if path[0] == "balance" and path[2] != "interestTransactionWritten" and observed is None \
                and targetValue is None:
            continue    # no transaction on either side: the written flag carries the comparison
        cls, ruleIds, reason = classify(observed, targetValue, cell, decisions, path)
        where, field = pathLabel(path)
        items.append({"where": where, "field": field, "observed": show(observed), "target": show(targetValue),
                      "expected": show(requiredValue(cell, decisions)) if cell else "(the rulebook states no value)",
                      "expectedStatus": cell["status"] if cell else "NONE",
                      "class": cls, "decisions": ruleIds, "reason": reason,
                      "rules": cell["rules"] if cell else [],
                      "options": [{"rule": c["rule"], "option": c["option"], "asCoded": c["asCoded"],
                                   "value": show(c["value"])} for c in (cell or {}).get("candidates", [])]})
    for entry in expectation.get("unresolved", []):
        items.append({"where": "case", "field": f"outcome not determined by the rulebook ({entry['rule']})",
                      "observed": legacy["runEnd"], "target": target["runEnd"], "expected": entry["reason"],
                      "expectedStatus": "UNRESOLVED", "class": "UNRESOLVED", "decisions": [entry["rule"]],
                      "reason": f"{entry['reason']}: the source does not determine the outcome and {entry['rule']} is open",
                      "rules": [entry["rule"]]})
    for diff in goldenDiffs or []:
        items.append({"where": f"golden file: {diff['where']}", "field": diff["field"], "observed": diff["actual"],
                      "target": "(not compared)", "expected": diff["expected"], "expectedStatus": "GOLDEN",
                      "class": "UNCLASSIFIED", "decisions": [], "rules": [],
                      "reason": "the legacy rerun differs from the golden file of the fixture"})
    return items


# ------------------------------------------------------------------ rule verdicts
def judgeEngine(expectation, lane, decisions, rulebook):
    """CONFORMS or VIOLATES per rule for one engine, against the rulebook alone."""
    cells = expectationCells(expectation)
    values = laneValues(lane, expectation)
    ruleIds = [r["id"] for r in rulebook["rules"]]
    exercised = set(expectation["rulesExercised"])
    undetermined = {u["rule"] for u in expectation.get("unresolved", [])}
    violations = {}

    def blame(ids, where, field, required, got):
        for ruleId in ids:
            violations.setdefault(ruleId, []).append(
                {"where": where, "field": field, "required": show(required), "got": show(got)})

    for path, cell in cells.items():
        required, got = requiredValue(cell, decisions), values.get(path)
        if path == ("run", "end"):
            conforms = runEndKind(required) == runEndKind(got)
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
    for rule in rulebook["rules"]:
        ruleId = rule["id"]
        if ruleId in violations:
            verdict = "VIOLATES"
        elif ruleId in undetermined:
            verdict = "NOT DETERMINED"
        elif ruleId in exercised:
            verdict = "CONFORMS"
        else:
            verdict = "NOT EXERCISED"
        verdicts.append({"rule": ruleId, "verdict": verdict, "evidence": violations.get(ruleId, [])})
    return verdicts, violations.get("(technical note)", [])


def ruleVerdicts(expectation, legacy, target, decisions, rulebook):
    legacyVerdicts, legacyNotes = judgeEngine(expectation, legacy, decisions, rulebook)
    targetVerdicts, targetNotes = judgeEngine(expectation, target, decisions, rulebook)
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


# ------------------------------------------------------------------ one case
def caseRefFor(kind, name=None, spec=None):
    if kind == "golden":
        if name not in cases.goldenCases():
            raise cases.InvalidCase([f"'{name}' is not a golden case"])
        return {"kind": kind, "name": name, "casesRoot": cases.fixturesRoot,
                "caseDir": cases.goldenDir / name, "title": name, "note": cases.goldenNote(name)}
    if kind == "probe":
        probe = next((p for p in cases.loadProbes() if p["name"] == name), None)
        if probe is None:
            raise cases.InvalidCase([f"'{name}' is not an edge probe"])
        return {"kind": kind, "name": name, "casesRoot": cases.labCasesRoot,
                "caseDir": cases.materialise(name, probe), "title": probe["title"], "note": probe["stresses"]}
    name = "custom-" + uuid.uuid4().hex[:8]
    return {"kind": "custom", "name": name, "casesRoot": cases.labCasesRoot,
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


def runCase(caseRef, runId=None):
    """Both lanes, the classification and the rule verdicts of one case."""
    runId = runId or uuid.uuid4().hex[:10]
    rulebook, decisions = loadRulebook(), loadDecisions()
    expectation, expectationSource = expectationFor(caseRef, rulebook)
    runDir = runsDir / runId
    workDir.mkdir(parents=True, exist_ok=True)
    with open(workDir / ".lock", "w") as lock:      # datasets/ and the replica tables are shared: one run at a time
        fcntl.flock(lock, fcntl.LOCK_EX)
        if runDir.exists():
            shutil.rmtree(runDir)
        legacy = runLegacy(caseRef, runDir / "legacy")
        target = runTarget(caseRef, runDir / "target", runId)
    goldenDiffs, goldenParity = None, None
    if caseRef["kind"] == "golden":
        goldenDiffs, checked = compare.compare_case("interest", caseRef["name"], runDir / "legacy")
        goldenParity = {"verdict": "PASS" if not goldenDiffs else "FAIL", "differences": len(goldenDiffs),
                        "records": checked["records"], "fields": checked["fields"]}
    items = compareLanes(expectation, legacy, target, decisions, goldenDiffs)
    verdictRows, technical = ruleVerdicts(expectation, legacy, target, decisions, rulebook)
    counts = {c: sum(1 for i in items if i["class"] == c) for c in classes}
    return {"runId": runId, "kind": caseRef["kind"], "case": caseRef["name"], "title": caseRef["title"],
            "note": caseRef["note"], "expectationSource": expectationSource,
            "expectationStatus": expectation["status"], "rulesExercised": expectation["rulesExercised"],
            "input": inputRecords(caseRef), "legacy": legacy, "target": target, "goldenParity": goldenParity,
            "items": items, "counts": counts, "compared": len(items), "ruleVerdicts": verdictRows,
            "technicalViolations": technical,
            "violations": {engine: sorted(r["rule"] for r in verdictRows if r[engine] == "VIOLATES")
                           for engine in ("legacy", "target")},
            "gate": "PASS" if counts["UNCLASSIFIED"] == 0 else "FAIL"}


# ------------------------------------------------------------------ gate and report
def summarise(results, rulebook=None):
    """Totals over a list of case results: class counts, rule coverage, violations, the gate."""
    rulebook = rulebook or loadRulebook()
    ruleIds = [r["id"] for r in rulebook["rules"]]
    counts = {c: sum(r["counts"][c] for r in results) for c in classes}
    exercised = sorted({ruleId for r in results for ruleId in r["rulesExercised"]})
    violations = {engine: sorted({ruleId for r in results for ruleId in r["violations"][engine]})
                  for engine in ("legacy", "target")}
    return {"cases": len(results), "compared": sum(r["compared"] for r in results), "counts": counts,
            "rulesExercised": exercised, "rulesTotal": len(ruleIds),
            "rulesNotExercised": [r for r in ruleIds if r not in exercised], "violations": violations,
            "gate": "PASS" if counts["UNCLASSIFIED"] == 0 else "FAIL"}


def countsLine(counts):
    return "  ".join(f"{c} {counts[c]}" for c in classes)


def gateLines(sections):
    """The text of `make lab-gate`. sections: [(title, [results])]."""
    lines = ["lab-gate: replay of the interest stream (INTCALC -> CBACT04C against java/interest)"]
    everything = []
    for title, results in sections:
        for result in results:
            violates = "; ".join(f"{engine} VIOLATES {', '.join(result['violations'][engine])}"
                                 for engine in ("legacy", "target") if result["violations"][engine])
            lines.append(f"{title:<6} {result['case']:<28} {countsLine(result['counts'])}"
                         + (f"  [{violates}]" if violates else ""))
        summary = summarise(results)
        lines.append(f"{title} total: {summary['cases']} cases, {summary['compared']} compared values: "
                     f"{countsLine(summary['counts'])}")
        lines.append(f"{title} rule coverage: {len(summary['rulesExercised'])} of {summary['rulesTotal']} rules "
                     f"exercised")
        everything += results
    total = summarise(everything)
    if len(sections) > 1:
        lines.append(f"all total: {total['cases']} cases, {total['compared']} compared values: "
                     f"{countsLine(total['counts'])}")
    lines.append(f"rule coverage: {len(total['rulesExercised'])} of {total['rulesTotal']} rules exercised by "
                 f"these cases; not exercised: {', '.join(total['rulesNotExercised']) or 'none'}")
    for engine in ("legacy", "target"):
        lines.append(f"rule verdicts, {engine}: "
                     + (f"VIOLATES {', '.join(total['violations'][engine])}" if total["violations"][engine]
                        else "no rule violated in any case"))
    lines.append(f"gate: {total['gate']} (UNCLASSIFIED = {total['counts']['UNCLASSIFIED']})")
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
                      f"Compared values: {result['compared']} - {countsLine(result['counts'])}. "
                      + (f"Legacy rerun against the golden files: {result['goldenParity']['verdict']} "
                         f"({result['goldenParity']['fields']} decoded fields). " if result["goldenParity"]
                         else "No golden file: the observed side is this run of the legacy lane. ")
                      + f"Legacy: {result['legacy']['runEnd']}; target: {result['target']['runEnd']}.", ""]
            shown = [i for i in result["items"] if i["class"] != "MATCH"]
            if shown:
                lines += ["| Class | Where | Field | Observed (legacy) | Target (Java) | Rulebook requires | Decision | Why |",
                          "|---|---|---|---|---|---|---|---|"]
                lines += [f"| {i['class']} | {i['where']} | `{i['field']}` | {cellText(i['observed'])} | "
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


def limitsMarkdown():
    return [f"- {text}" for text in limits]


limits = [
    "Sequential batch replay only: one clean run of INTCALC per case on small synthetic inputs. No rerun, no restart "
    "after a failure, no volume.",
    "No CICS: the online programs that read and write the same account file are not run.",
    "No concurrency: one run at a time; nothing else touches the files or the tables during a run.",
    "GnuCOBOL arithmetic, not Enterprise COBOL: the observed side is GnuCOBOL 3 with -std=ibm; intermediate results, "
    "truncation and overflow under the host compiler options are not shown.",
    "No real Db2 and no VSAM: indexed files are GnuCOBOL files, the target database is PostgreSQL 15 in a container.",
    "ASCII with zoned signs as the off-host harness writes them, not cp037 host files; a frozen clock.",
    "A rule verdict is a check against the rulebook's own evaluator, which an agent wrote from the code; "
    "it is not a business owner's view. Rules no case exercised have no verdict.",
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
            "businessDate": params["runClock"], "parmDate": params["parmDate"]}


def runSet(kind):
    names = cases.goldenCases() if kind == "golden" else [p["name"] for p in cases.loadProbes()]
    results = []
    for name in names:
        result = runCase(caseRefFor(kind, name), runId=f"gate-{name}")
        print(f"  ran {kind} {name}: {countsLine(result['counts'])}", file=sys.stderr, flush=True)
        results.append(result)
    return results


def gate(args):
    sections = []
    if args.set in ("golden", "all"):
        sections.append(("golden", runSet("golden")))
    if args.set in ("probes", "all"):
        sections.append(("probe", runSet("probe")))
    lines, total = gateLines(sections)
    print("\n".join(lines))
    if args.report:
        header = headerInfo()
        meta = [("Baseline of the estate", f"`{header['baselineSha']}` ({header['baselineState']})"),
                ("Frozen business date", f"{header['businessDate']} (clock of the run), PARM {header['parmDate']}"),
                ("Toolchain", ", ".join(f"{name} {version}" for name, version in header["toolchain"])),
                ("Cases", ", ".join(f"{len(results)} {title}" for title, results in sections))]
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(reportMarkdown(sections, lines, meta))
        print(f"report: {args.report}")
    if args.json:
        args.json.parent.mkdir(parents=True, exist_ok=True)
        args.json.write_text(json.dumps({"sections": [{"title": t, "results": r} for t, r in sections],
                                         "total": total}, indent=1))
    return 0 if total["gate"] == "PASS" else 1


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    commands = parser.add_subparsers(dest="command", required=True)
    gateParser = commands.add_parser("gate")
    gateParser.add_argument("--set", choices=["golden", "probes", "all"], default="all")
    gateParser.add_argument("--report", type=Path)
    gateParser.add_argument("--json", type=Path)
    caseParser = commands.add_parser("case")
    caseParser.add_argument("name")
    args = parser.parse_args()
    try:
        if args.command == "gate":
            return gate(args)
        kind = "golden" if args.name in cases.goldenCases() else "probe"
        print(json.dumps(runCase(caseRefFor(kind, args.name)), indent=1))
        return 0
    except (LabError, cases.InvalidCase) as error:
        print(f"lab: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
