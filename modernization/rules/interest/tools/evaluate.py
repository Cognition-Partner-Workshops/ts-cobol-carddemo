#!/usr/bin/env python3
"""Rulebook evaluator for the interest stream.

Derives, for every case under fixtures/interest, the expected outcome from
rules.yaml and the case's input/ files only, and writes expected/<case>.json.
It never opens fixtures/*/expected, never runs the COBOL and does not use
tools/parity/naive_ref.py.

  python3 modernization/rules/interest/tools/evaluate.py           write expected/*.json
  python3 modernization/rules/interest/tools/evaluate.py --check   fail when they are stale
"""
import json
import sys
from decimal import ROUND_DOWN, ROUND_HALF_EVEN, ROUND_HALF_UP, Decimal
from pathlib import Path

import yaml

rulesDir = Path(__file__).resolve().parents[1]
repoRoot = rulesDir.parents[2]
ownerPlaceholder = "TBD - business owner"
roundingModes = {"truncate": ROUND_DOWN, "halfUp": ROUND_HALF_UP, "halfEven": ROUND_HALF_EVEN}
cent = Decimal("0.01")
positiveSigns = "{ABCDEFGHI"
negativeSigns = "}JKLMNOPQR"


def loadRulebook():
    return yaml.safe_load((rulesDir / "rules.yaml").read_text())


def undecidedRules(rulebook):
    return [r for r in rulebook["rules"] if r["type"] in ("QUIRK", "HOLE")]


def asCodedConfig(rulebook):
    """Settings of the run as the code behaves: RULE logic plus each as-coded option."""
    config = {}
    for rule in rulebook["rules"]:
        config.update(rule.get("logic", {}))
        for option in rule.get("options", []):
            if option.get("asCoded"):
                config.update(option.get("set", {}))
    return config


def zoned(text, decimals):
    last = text[-1]
    if last in positiveSigns:
        digits, sign = text[:-1] + str(positiveSigns.index(last)), 1
    elif last in negativeSigns:
        digits, sign = text[:-1] + str(negativeSigns.index(last)), -1
    else:
        digits, sign = text, 1
    if not digits.isdigit():
        raise ValueError(f"not a zoned decimal: {text!r}")
    return sign * Decimal(int(digits)).scaleb(-decimals)


def readInput(rulebook, caseDir, name):
    layout = rulebook["inputLayouts"][name]
    inputPath = caseDir / "input" / layout["file"]
    assert "expected" not in inputPath.parts, "the evaluator reads input/ only"
    records = []
    for line in inputPath.read_text().splitlines():
        if not line.strip():
            continue
        record = {"_line": line}
        for field, spec in layout["fields"].items():
            raw = line[spec["at"]:spec["at"] + spec["len"]].ljust(spec["len"])
            record[field] = zoned(raw, spec["decimals"]) if "decimals" in spec else raw
        records.append(record)
    return records


def money(value):
    return f"{value:.2f}"


def evaluate(rulebook, caseDir, config):
    """One run of the rulebook. Returns values {path: value}, cites {path: [ids]},
    the rule ids the run exercised and the reasons it could not be resolved."""
    params = rulebook["runParameters"]
    parmDate, runClock = params["parmDate"]["value"], params["runClock"]["value"]
    balances = sorted(readInput(rulebook, caseDir, "balances"),
                      key=lambda b: tuple(b[f] for f in config["order"]))
    rates = {(r["DIS-ACCT-GROUP-ID"], r["DIS-TRAN-TYPE-CD"], r["DIS-TRAN-CAT-CD"]): r["DIS-INT-RATE"]
             for r in readInput(rulebook, caseDir, "rates")}
    accounts = {a["ACCT-ID"]: a for a in readInput(rulebook, caseDir, "accounts")}
    cards = {}
    for card in readInput(rulebook, caseDir, "cards"):
        cards.setdefault(card["XREF-ACCT-ID"], []).append(card["XREF-CARD-NUM"])

    values, cites, exercised, unresolved = {}, {}, set(), []
    sysout = [("START OF EXECUTION OF PROGRAM CBACT04C", ["INT-T02"])]
    accountState = {k: {"ACCT-CURR-BAL": a["ACCT-CURR-BAL"], "ACCT-CURR-CYC-CREDIT": a["ACCT-CURR-CYC-CREDIT"],
                        "ACCT-CURR-CYC-DEBIT": a["ACCT-CURR-CYC-DEBIT"]} for k, a in accounts.items()}
    accountCites = {k: ["INT-T06"] for k in accounts}
    transactionFields = ["TRAN-ID", "TRAN-TYPE-CD", "TRAN-CAT-CD", "TRAN-SOURCE", "TRAN-DESC", "TRAN-AMT",
                         "TRAN-MERCHANT-ID", "TRAN-MERCHANT-NAME", "TRAN-MERCHANT-CITY", "TRAN-MERCHANT-ZIP",
                         "TRAN-CARD-NUM", "TRAN-ORIG-TS", "TRAN-PROC-TS"]

    def put(path, value, ruleIds):
        values[path], cites[path] = value, ruleIds

    def updateAccount(accountId, totalInterest):
        state = accountState[accountId]
        state["ACCT-CURR-BAL"] += totalInterest
        if config["resetCycleAmounts"]:
            state["ACCT-CURR-CYC-CREDIT"] = state["ACCT-CURR-CYC-DEBIT"] = Decimal(0)
        accountCites[accountId] = ["INT-R07", "INT-R05", "INT-Q01"]

    if config["idPrefix"] == "parm":
        idPrefix = parmDate
    else:
        idPrefix = runClock[:10].replace("-", "") + "00"
    if config["timestampSource"] == "runClock":
        stamp = runClock[:10] + "-" + runClock[11:].replace(":", ".") + "0000"
    else:
        stamp = f"{parmDate[:4]}-{parmDate[4:6]}-{parmDate[6:8]}-00.00.00.000000"

    lastAccount, account, cardNumber, totalInterest, sequence, abend = None, None, None, Decimal(0), 0, None
    skipAccount = False
    for balance in balances:
        accountId, typeCd, catCd = (balance[f] for f in config["order"])
        key = f"{accountId}/{typeCd}/{catCd}"
        exercised.update(["INT-R01", "INT-Q04"])
        sysout.append((balance["_line"].rstrip(), ["INT-T02", "INT-R01"]))
        if accountId != lastAccount:
            if lastAccount is not None and not skipAccount:
                updateAccount(lastAccount, totalInterest)
                exercised.add("INT-R07")
            totalInterest, lastAccount = Decimal(0), accountId
            account = accounts.get(accountId)
            if account is None:
                sysout.append((f"ACCOUNT NOT FOUND: {accountId}", ["INT-R08"]))
                sysout.append(("ERROR READING ACCOUNT FILE", ["INT-R08"]))
                abend = "account missing from the account file"
                break
            ownCards = cards.get(accountId, [])
            if not ownCards:
                sysout.append((f"ACCOUNT NOT FOUND: {accountId}", ["INT-R08"]))
                sysout.append(("ERROR READING XREF FILE", ["INT-R08"]))
                abend = "account missing from the card cross-reference"
                break
            if len(ownCards) > 1:
                exercised.add("INT-H03")
                unresolved.append({"rule": "INT-H03", "reason": f"account {accountId} has {len(ownCards)} cards"})
            cardNumber = ownCards[0]
            skipAccount = config["inactiveAccounts"] == "skip" and account["ACCT-ACTIVE-STATUS"] != "Y"
            if account["ACCT-ACTIVE-STATUS"] != "Y":
                exercised.add("INT-H04")
        base = ("balance", key)
        decisionCites = ["INT-R01", "INT-R02"]
        exercised.add("INT-R02")
        group = account["ACCT-GROUP-ID"]
        rate = rates.get((group, typeCd, catCd))
        rateGroup = group.rstrip()
        if rate is None:
            exercised.add("INT-R03")
            decisionCites.append("INT-R03")
            for message in config["messages"]:
                sysout.append((message, ["INT-R03", "INT-T02"]))
            rateGroup = config["defaultGroup"]
            rate = rates.get((rateGroup.ljust(10), typeCd, catCd))
            if rate is None:
                sysout.append(("ERROR READING DEFAULT DISCLOSURE GROUP", ["INT-R08"]))
                abend = "no rate for the DEFAULT disclosure group"
                break
        put(base + ("rateGroup",), rateGroup, decisionCites)
        put(base + ("ratePercent",), money(rate), decisionCites)
        written, amount, writeCites = False, None, list(decisionCites)
        if skipAccount:
            writeCites.append("INT-H04")
        elif rate == 0 and config["skipZeroRate"]:
            exercised.add("INT-R04")
            writeCites.append("INT-R04")
        else:
            exact = balance["TRAN-CAT-BAL"] * rate / Decimal(config["divisor"])
            amount = exact.quantize(cent, rounding=roundingModes[config["rounding"]])
            exercised.update(["INT-R05", "INT-H01"])
            writeCites += ["INT-R04", "INT-R05"]
            if exact != exact.quantize(cent, rounding=ROUND_DOWN):
                exercised.add("INT-Q01")
            if balance["TRAN-CAT-BAL"] < 0 or rate < 0:
                exercised.add("INT-Q05")
            if exact.quantize(cent, rounding=ROUND_DOWN) == 0:
                exercised.add("INT-Q06")
            if abs(amount) >= Decimal(10) ** 9:
                exercised.add("INT-H05")
                unresolved.append({"rule": "INT-H05", "reason": f"interest {amount} on {key} does not fit S9(09)V99"})
            if balance["TRAN-CAT-BAL"] < 0 and config["negativeBalance"] == "skip":
                amount = None
            elif amount == 0 and config["zeroInterest"] == "skip":
                amount = None
            else:
                written = True
        put(base + ("interestTransactionWritten",), written, writeCites)
        transaction = dict.fromkeys(transactionFields)
        if written:
            totalInterest += amount
            sequence += 1
            if sequence > 999999:
                exercised.add("INT-H05")
                unresolved.append({"rule": "INT-H05", "reason": "more than 999,999 interest transactions"})
            exercised.update(["INT-R06", "INT-Q02", "INT-Q03"])
            transaction.update({
                "TRAN-ID": f"{idPrefix}{sequence % 1000000:06d}", "TRAN-TYPE-CD": config["tranTypeCd"],
                "TRAN-CAT-CD": config["tranCatCd"], "TRAN-SOURCE": config["tranSource"],
                "TRAN-DESC": config["descPrefix"] + accountId, "TRAN-AMT": money(amount),
                "TRAN-MERCHANT-ID": config["merchantId"], "TRAN-MERCHANT-NAME": "", "TRAN-MERCHANT-CITY": "",
                "TRAN-MERCHANT-ZIP": "", "TRAN-CARD-NUM": cardNumber, "TRAN-ORIG-TS": stamp, "TRAN-PROC-TS": stamp})
        fieldCites = {"TRAN-ID": ["INT-Q02"], "TRAN-AMT": ["INT-R05", "INT-Q01"], "TRAN-DESC": ["INT-R06", "INT-T04"],
                      "TRAN-ORIG-TS": ["INT-Q03", "INT-T05"], "TRAN-PROC-TS": ["INT-Q03", "INT-T05"]}
        for field in transactionFields:
            put(base + (field,), transaction[field], fieldCites.get(field, ["INT-R06"]) if written else writeCites)

    if abend:
        exercised.add("INT-R08")
        sysout.append(("ABENDING PROGRAM", ["INT-R08"]))
        put(("run", "end"), f"abend 999 - {abend}", ["INT-R08"])
    else:
        if lastAccount is not None and not skipAccount:
            if config["updateLastAccount"]:
                updateAccount(lastAccount, totalInterest)
            else:
                accountCites[lastAccount] = ["INT-Q04"]
        sysout.append(("END OF EXECUTION OF PROGRAM CBACT04C", ["INT-T02"]))
        put(("run", "end"), "normal end, return code 0", ["INT-T03"])
    for accountId, state in accountState.items():
        for field, value in state.items():
            put(("account", accountId, field), money(value), accountCites[accountId])
    put(("run", "sysout"), [text for text, _ in sysout], sorted({i for _, ids in sysout for i in ids}))
    return {"values": values, "cites": cites, "exercised": exercised, "unresolved": unresolved,
            "sysoutCites": [ids for _, ids in sysout]}


def optionRuns(rulebook, caseDir):
    """The as-coded run plus one run per computable option of every undecided rule."""
    baseConfig = asCodedConfig(rulebook)
    baseRun = evaluate(rulebook, caseDir, baseConfig)
    runs = []
    for rule in undecidedRules(rulebook):
        for option in rule["options"]:
            if "set" not in option:
                continue
            run = baseRun if option.get("asCoded") else evaluate(rulebook, caseDir, {**baseConfig, **option["set"]})
            runs.append((rule, option, run))
    return baseRun, runs


def differences(baseRun, run):
    paths = list(baseRun["values"]) + [p for p in run["values"] if p not in baseRun["values"]]
    return [p for p in paths if baseRun["values"].get(p) != run["values"].get(p)]


def expectation(rulebook, caseDir):
    baseRun, runs = optionRuns(rulebook, caseDir)
    changedBy = {}
    for rule, option, run in runs:
        for path in differences(baseRun, run):
            changedBy.setdefault(path, [])
            if rule["id"] not in changedBy[path]:
                changedBy[path].append(rule["id"])

    def cell(path):
        ruleIds = list(baseRun["cites"][path])
        if path not in changedBy:
            return {"value": baseRun["values"][path], "status": "DETERMINED", "rules": ruleIds}
        candidates = []
        for rule, option, run in runs:
            if rule["id"] in changedBy[path]:
                candidates.append({"rule": rule["id"], "option": option["id"], "label": option["label"],
                                   "asCoded": bool(option.get("asCoded")), "value": run["values"].get(path)})
        return {"status": "OPEN", "rules": ruleIds + [r for r in changedBy[path] if r not in ruleIds],
                "candidates": candidates}

    balances, accounts = {}, {}
    for path in baseRun["values"]:
        if path[0] == "balance":
            balances.setdefault(path[1], {})[path[2]] = cell(path)
        elif path[0] == "account":
            accounts.setdefault(path[1], {})[path[2]] = cell(path)
    openRules = sorted({r for ids in changedBy.values() for r in ids})
    byId = {r["id"]: r for r in rulebook["rules"]}
    notDerivable = []
    for ruleId in sorted(baseRun["exercised"]):
        for option in byId[ruleId].get("options", []):
            if "notDerivable" in option and not byId[ruleId].get("unresolvedWhen"):
                notDerivable.append({"rule": ruleId, "option": option["id"], "label": option["label"],
                                     "outcome": option["notDerivable"]})
    status = "UNRESOLVED" if baseRun["unresolved"] else "OPEN" if openRules or notDerivable else "DETERMINED"
    result = {
        "case": caseDir.name,
        "status": status,
        "notice": "Derived from rules.yaml and this case's input/ files only. Not SME sign-off; "
                  "the recorded legacy output under fixtures/ was not read.",
        "derivedFrom": {"rules": "modernization/rules/interest/rules.yaml",
                        "inputs": sorted(str(p.relative_to(repoRoot)) for p in (caseDir / "input").iterdir()
                                         if p.name in {l["file"] for l in rulebook["inputLayouts"].values()})},
        "rulesExercised": sorted(baseRun["exercised"]),
        "openRules": openRules,
        "optionsNotDerivable": notDerivable,
        "run": {"end": cell(("run", "end")),
                "sysout": {**cell(("run", "sysout")), "lineRules": baseRun["sysoutCites"]}},
        "balances": [{"key": key, **fields} for key, fields in balances.items()],
        "accounts": [{"ACCT-ID": key, **fields} for key, fields in accounts.items()],
    }
    if baseRun["unresolved"]:
        result["unresolved"] = [{**u, "owner": ownerPlaceholder} for u in baseRun["unresolved"]]
    return result


def caseDirs(rulebook):
    return sorted(p for p in (repoRoot / rulebook["stream"]["fixtures"]).iterdir() if (p / "input").is_dir())


def render(result):
    return json.dumps(result, indent=1) + "\n"


def main():
    check = "--check" in sys.argv
    rulebook = loadRulebook()
    outDir = rulesDir / "expected"
    outDir.mkdir(exist_ok=True)
    problems, exercisedBy = [], {}
    for caseDir in caseDirs(rulebook):
        result = expectation(rulebook, caseDir)
        for ruleId in result["rulesExercised"]:
            exercisedBy.setdefault(ruleId, []).append(caseDir.name)
        target = outDir / f"{caseDir.name}.json"
        text = render(result)
        if check:
            if not target.exists() or target.read_text() != text:
                problems.append(f"{target.relative_to(repoRoot)} is out of date with rules.yaml or the case inputs")
        else:
            target.write_text(text)
            print(f"{target.relative_to(repoRoot)}: {result['status']}"
                  + (f" ({', '.join(result['openRules'])})" if result["openRules"] else ""))
    for rule in rulebook["rules"]:
        derived = exercisedBy.get(rule["id"], [])
        if sorted(rule["fixtures"]) != derived:
            problems.append(f"rules.yaml {rule['id']} lists fixtures {sorted(rule['fixtures'])}, "
                            f"the evaluator finds {derived}")
    known = {c.name + ".json" for c in caseDirs(rulebook)}
    for stray in sorted(p.name for p in outDir.glob("*.json") if p.name not in known):
        problems.append(f"expected/{stray} has no fixture case")
    for problem in problems:
        print("FAIL:", problem)
    if problems:
        sys.exit(1)
    if check:
        print(f"expectations are current: {len(known)} cases")


if __name__ == "__main__":
    main()
