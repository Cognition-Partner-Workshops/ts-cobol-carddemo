"""Cases of the replay lab: the seven golden fixtures, the edge probes and custom inputs.

A probe or a custom input is a small JSON document (accounts, balances, optional rates; per account
optionally `cards` 0-3 and `onAccountFile` false; optionally `runs` 2). It is
checked, then encoded into the four unloaded files of the interest chain with the copybook codec of
tools/parity and written under work/lab/cases - never under fixtures/.
"""
import json
import re
import shutil
import sys
from decimal import Decimal, InvalidOperation
from pathlib import Path

labDir = Path(__file__).resolve().parent
repoRoot = labDir.parent
sys.path.insert(0, str(repoRoot))
from tools.parity.copybook import encode_record  # noqa: E402

fixturesRoot = repoRoot / "fixtures"
goldenDir = fixturesRoot / "interest"
labCasesRoot = repoRoot / "work" / "lab" / "cases"
probesPath = labDir / "probes.json"

# The rate table every golden fixture uses (tools/fixtures/gen_fixtures.py); a case may bring its own.
defaultRates = [
    ("GOLD", "01", 1, "12.00"), ("GOLD", "01", 2, "18.00"), ("GOLD", "02", 1, "0"), ("GOLD", "03", 1, "0"),
    ("GOLD", "05", 1, "0"),
    ("STANDARD", "01", 1, "18.50"), ("STANDARD", "01", 2, "24.00"), ("STANDARD", "02", 1, "0"),
    ("STANDARD", "03", 1, "0"), ("STANDARD", "05", 1, "0"),
    ("DEFAULT", "01", 1, "21.00"), ("DEFAULT", "01", 2, "27.00"), ("DEFAULT", "02", 1, "0"),
    ("DEFAULT", "03", 1, "0"), ("DEFAULT", "05", 1, "0")]
maxAccounts, maxBalances, maxRates = 20, 50, 60
maxAmount = Decimal("9999999.99")
maxRate = Decimal("9999.99")


class InvalidCase(ValueError):
    """The input cannot be run; `problems` says why in plain words."""

    def __init__(self, problems):
        super().__init__("; ".join(problems))
        self.problems = problems


def goldenCases():
    return sorted(p.name for p in goldenDir.iterdir() if (p / "input").is_dir())


def goldenNote(caseName):
    readme = goldenDir / caseName / "README.md"
    return readme.read_text().strip() if readme.exists() else ""


def loadProbes():
    return json.loads(probesPath.read_text())["probes"]


def amount(value, label, limit, problems, allowNegative=True):
    try:
        number = Decimal(str(value).strip())
    except (InvalidOperation, ValueError):
        problems.append(f"{label}: '{value}' is not a number")
        return None
    if not number.is_finite():
        problems.append(f"{label}: '{value}' is not a number")
        return None
    if number != number.quantize(Decimal("0.01")):
        problems.append(f"{label}: {value} has more than two decimals")
    elif abs(number) > limit:
        problems.append(f"{label}: {value} is outside -{limit} .. {limit}")
    elif number < 0 and not allowNegative:
        problems.append(f"{label}: {value} must not be negative")
    else:
        return number
    return None


def wholeNumber(value, label, upper, problems):
    if isinstance(value, bool) or not re.fullmatch(r"\d+", str(value).strip()) or not 1 <= int(value) <= upper:
        problems.append(f"{label}: '{value}' must be a whole number from 1 to {upper}")
        return None
    return int(value)


def checkCase(spec):
    """Validate a probe or custom input; returns it normalised or raises InvalidCase."""
    problems = []
    if not isinstance(spec, dict):
        raise InvalidCase(["the input must be a JSON object with 'accounts' and 'balances'"])
    for name in ("accounts", "balances"):
        if not isinstance(spec.get(name), list) or not spec[name]:
            problems.append(f"'{name}' must be a list with at least one entry")
    if problems:
        raise InvalidCase(problems)
    unknown = sorted(set(spec) - {"accounts", "balances", "rates", "runs", "name", "title", "stresses", "note"})
    if unknown:
        problems.append(f"unknown key(s): {', '.join(unknown)}")
    runs = spec.get("runs", 1)
    if isinstance(runs, bool) or runs not in (1, 2):
        problems.append("'runs' must be 1 or 2 (2 runs the job a second time on the files the first run left)")
    if len(spec["accounts"]) > maxAccounts:
        problems.append(f"at most {maxAccounts} accounts")
    if len(spec["balances"]) > maxBalances:
        problems.append(f"at most {maxBalances} balances")
    accounts, seen = [], set()
    for index, entry in enumerate(spec["accounts"], 1):
        label = f"accounts[{index}]"
        if not isinstance(entry, dict):
            problems.append(f"{label} must be an object")
            continue
        accountId = wholeNumber(entry.get("id"), f"{label}.id", 99999999999, problems)
        if accountId in seen:
            problems.append(f"{label}.id: account {accountId} is listed twice")
        seen.add(accountId)
        group = str(entry.get("group", ""))
        if len(group) > 10 or not re.fullmatch(r"[A-Z0-9 ]*", group):
            problems.append(f"{label}.group: '{group}' must be up to 10 capital letters or digits (empty for none)")
        active = str(entry.get("active", "Y"))
        if active not in ("Y", "N"):
            problems.append(f"{label}.active: '{active}' must be Y or N")
        cards = entry.get("cards", 1)
        if isinstance(cards, bool) or cards not in (0, 1, 2, 3):
            problems.append(f"{label}.cards: '{cards}' must be 0, 1, 2 or 3 (how many cards the account has)")
        onAccountFile = entry.get("onAccountFile", True)
        if not isinstance(onAccountFile, bool):
            problems.append(f"{label}.onAccountFile: must be true or false")
        accounts.append({"id": accountId, "group": group, "active": active, "cards": cards,
                         "onAccountFile": onAccountFile,
                         "balance": amount(entry.get("balance", "0"), f"{label}.balance", maxAmount, problems),
                         "cycleCredit": amount(entry.get("cycleCredit", "0"), f"{label}.cycleCredit", maxAmount, problems),
                         "cycleDebit": amount(entry.get("cycleDebit", "0"), f"{label}.cycleDebit", maxAmount, problems)})
    balances, keys = [], set()
    for index, entry in enumerate(spec["balances"], 1):
        label = f"balances[{index}]"
        if not isinstance(entry, dict):
            problems.append(f"{label} must be an object")
            continue
        accountId = wholeNumber(entry.get("account"), f"{label}.account", 99999999999, problems)
        if accountId is not None and accountId not in seen:
            problems.append(f"{label}.account: account {accountId} is not in 'accounts'")
        typeCd = str(entry.get("type", ""))
        if not re.fullmatch(r"\d{2}", typeCd):
            problems.append(f"{label}.type: '{typeCd}' must be two digits, for example 01")
        category = wholeNumber(entry.get("category"), f"{label}.category", 9999, problems)
        if (accountId, typeCd, category) in keys:
            problems.append(f"{label}: balance {accountId}/{typeCd}/{category} is listed twice")
        keys.add((accountId, typeCd, category))
        balances.append({"account": accountId, "type": typeCd, "category": category,
                         "balance": amount(entry.get("balance"), f"{label}.balance", maxAmount, problems)})
    rates = []
    if "rates" in spec:
        if not isinstance(spec["rates"], list) or not spec["rates"] or len(spec["rates"]) > maxRates:
            problems.append(f"'rates', when given, must be a list of 1 to {maxRates} entries")
        else:
            for index, entry in enumerate(spec["rates"], 1):
                label = f"rates[{index}]"
                if not isinstance(entry, dict):
                    problems.append(f"{label} must be an object")
                    continue
                group = str(entry.get("group", ""))
                if not re.fullmatch(r"[A-Z0-9]{1,10}", group):
                    problems.append(f"{label}.group: '{group}' must be 1 to 10 capital letters or digits")
                typeCd = str(entry.get("type", ""))
                if not re.fullmatch(r"\d{2}", typeCd):
                    problems.append(f"{label}.type: '{typeCd}' must be two digits")
                rates.append((group, typeCd, wholeNumber(entry.get("category"), f"{label}.category", 9999, problems),
                              amount(entry.get("rate"), f"{label}.rate", maxRate, problems, allowNegative=False)))
    if problems:
        raise InvalidCase(problems)
    return {"accounts": accounts, "balances": balances, "runs": runs,
            "rates": rates or [(g, t, c, Decimal(r)) for g, t, c, r in defaultRates]}


def cardNumber(accountId, index=0):
    """Synthetic card numbers of an account: 4000..., then 4001..., 4002... for further cards."""
    return f"{4000 + index}{accountId:012d}"[-16:]


def materialise(caseName, spec):
    """Write the four unloaded files of a checked case under work/lab/cases/interest/<name>/input."""
    case = checkCase(spec)
    caseDir = labCasesRoot / "interest" / caseName
    assert fixturesRoot not in caseDir.parents, "lab cases are never written under fixtures/"
    if caseDir.exists():
        shutil.rmtree(caseDir)
    inputDir = caseDir / "input"
    inputDir.mkdir(parents=True)

    def write(dsn, layout, records):
        (inputDir / f"{dsn}.txt").write_bytes(
            b"".join(encode_record(layout, r).rstrip(b" ") + b"\n" for r in records))

    write("AWS.M2.CARDDEMO.ACCTDATA.PS", "CVACT01Y", [
        {"ACCT-ID": a["id"], "ACCT-ACTIVE-STATUS": a["active"], "ACCT-CURR-BAL": a["balance"],
         "ACCT-CREDIT-LIMIT": "5000.00", "ACCT-CASH-CREDIT-LIMIT": "2500.00", "ACCT-OPEN-DATE": "2019-03-15",
         "ACCT-EXPIRAION-DATE": "2026-12-31", "ACCT-REISSUE-DATE": "2026-12-31",
         "ACCT-CURR-CYC-CREDIT": a["cycleCredit"], "ACCT-CURR-CYC-DEBIT": a["cycleDebit"],
         "ACCT-ADDR-ZIP": "10115", "ACCT-GROUP-ID": a["group"]}
        for a in sorted(case["accounts"], key=lambda a: a["id"]) if a["onAccountFile"]])
    write("AWS.M2.CARDDEMO.CARDXREF.PS", "CVACT03Y", sorted((
        {"XREF-CARD-NUM": cardNumber(a["id"], index), "XREF-CUST-ID": a["id"] % 10 ** 9, "XREF-ACCT-ID": a["id"]}
        for a in case["accounts"] for index in range(a["cards"])), key=lambda r: r["XREF-CARD-NUM"]))
    write("AWS.M2.CARDDEMO.TCATBALF.PS", "CVTRA01Y", [
        {"TRANCAT-ACCT-ID": b["account"], "TRANCAT-TYPE-CD": b["type"], "TRANCAT-CD": b["category"],
         "TRAN-CAT-BAL": b["balance"]}
        for b in sorted(case["balances"], key=lambda b: (b["account"], b["type"], b["category"]))])
    write("AWS.M2.CARDDEMO.DISCGRP.PS", "CVTRA02Y", [
        {"DIS-ACCT-GROUP-ID": g, "DIS-TRAN-TYPE-CD": t, "DIS-TRAN-CAT-CD": c, "DIS-INT-RATE": r}
        for g, t, c, r in sorted(case["rates"], key=lambda x: (x[0].ljust(10), x[1], x[2]))])
    return caseDir
