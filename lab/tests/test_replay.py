"""Unit tests of the classification and the rule verdicts. No engine is run: the lanes are built
from the committed expectation of the tie case, then bent to show each class and each verdict."""
import copy
import json
import sys
from pathlib import Path

import pytest

labDir = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(labDir))
sys.dont_write_bytecode = True
import cases  # noqa: E402
import replay  # noqa: E402

tieCase = "i02-tie-half-cent"
tieBalance = "00000002003/01/0001"


@pytest.fixture(scope="module")
def rulebook():
    return replay.loadRulebook()


@pytest.fixture(scope="module")
def expectation():
    return json.loads((replay.rulesDir / "expected" / f"{tieCase}.json").read_text())


@pytest.fixture
def decisions():
    return replay.loadDecisions()


def asCoded(cell):
    return cell["value"] if cell["status"] == "DETERMINED" else next(c["value"] for c in cell["candidates"] if c["asCoded"])


def laneFrom(expectation, engine):
    """A lane that behaves exactly as the rulebook's as-coded outcome."""
    transactions = []
    for balance in expectation["balances"]:
        if asCoded(balance["interestTransactionWritten"]):
            transactions.append({f: asCoded(balance[f]) for f in replay.transactionFields})
    accounts = []
    for account in expectation["accounts"]:
        record = dict.fromkeys(replay.accountFields, "same")
        record.update({"ACCT-ID": account["ACCT-ID"]},
                      **{f: asCoded(c) for f, c in account.items() if f != "ACCT-ID"})
        accounts.append(record)
    return {"engine": engine, "transactions": transactions, "accounts": accounts,
            "sysout": asCoded(expectation["run"]["sysout"]), "returnCodes": {"INTCALC": {"maxrc": 0}},
            "runEnd": asCoded(expectation["run"]["end"])}


def approve(decisions, ruleId, option):
    decisions = copy.deepcopy(decisions)
    decisions[ruleId].update(status="APPROVED", approvedOption=option)
    return decisions


def itemOf(items, where, field):
    return next(i for i in items if i["where"] == where and i["field"] == field)


def counts(items):
    return {c: sum(1 for i in items if i["class"] == c) for c in replay.classes}


def test_register_is_parsed_and_every_decision_is_open(decisions):
    assert len(decisions) == 12
    assert {d["status"] for d in decisions.values()} == {"OPEN"}


def test_register_row_with_an_approved_option(tmp_path):
    text = replay.decisionsPath.read_text().replace("| TBD - business owner | OPEN |", "| J. Doe | APPROVED B |", 1)
    register = tmp_path / "decisions.md"
    register.write_text(text)
    decisions = replay.loadDecisions(register)
    assert decisions["INT-Q01"]["status"] == "APPROVED" and decisions["INT-Q01"]["approvedOption"] == "B"
    assert decisions["INT-Q02"]["status"] == "OPEN"


def test_agreeing_engines_are_match_or_unresolved_never_unclassified(expectation, decisions):
    legacy, target = laneFrom(expectation, "legacy"), laneFrom(expectation, "target")
    items = replay.compareLanes(expectation, legacy, target, decisions)
    assert counts(items)["UNCLASSIFIED"] == 0 and counts(items)["INTENDED_CHANGE"] == 0
    assert itemOf(items, f"balance {tieBalance}", "TRAN-AMT")["class"] == "UNRESOLVED"
    assert itemOf(items, f"balance {tieBalance}", "TRAN-AMT")["decisions"] == ["INT-Q01"]
    assert itemOf(items, "account 00000002003", "ACCT-CURR-BAL")["class"] == "UNRESOLVED"
    assert itemOf(items, f"balance {tieBalance}", "TRAN-TYPE-CD")["class"] == "MATCH"


def test_difference_between_two_options_of_an_open_decision_is_unresolved(expectation, decisions):
    legacy, target = laneFrom(expectation, "legacy"), laneFrom(expectation, "target")
    target["transactions"][0]["TRAN-AMT"] = "12.36"         # half-even, option C of INT-Q01
    item = itemOf(replay.compareLanes(expectation, legacy, target, decisions), f"balance {tieBalance}", "TRAN-AMT")
    assert (item["class"], item["decisions"]) == ("UNRESOLVED", ["INT-Q01"])


def test_difference_an_approved_decision_asks_for_is_intended_change(expectation, decisions):
    legacy, target = laneFrom(expectation, "legacy"), laneFrom(expectation, "target")
    target["transactions"][0]["TRAN-AMT"] = "12.36"
    approved = approve(decisions, "INT-Q01", "B")
    item = itemOf(replay.compareLanes(expectation, legacy, target, approved), f"balance {tieBalance}", "TRAN-AMT")
    assert (item["class"], item["decisions"]) == ("INTENDED_CHANGE", ["INT-Q01"])
    # agreeing on the old behaviour after the decision is no longer UNRESOLVED; the verdicts catch it
    same = itemOf(replay.compareLanes(expectation, legacy, laneFrom(expectation, "target"), approved),
                  f"balance {tieBalance}", "TRAN-AMT")
    assert same["class"] == "MATCH"


def test_any_other_difference_is_unclassified(expectation, decisions):
    legacy, target = laneFrom(expectation, "legacy"), laneFrom(expectation, "target")
    target["transactions"][0]["TRAN-AMT"] = "99.99"         # no option of any decision
    target["transactions"][0]["TRAN-SOURCE"] = "Batch"      # a determined value
    target["accounts"][0]["ACCT-CREDIT-LIMIT"] = "other"    # a field the rulebook says nothing about
    items = replay.compareLanes(expectation, legacy, target, decisions)
    assert itemOf(items, f"balance {tieBalance}", "TRAN-AMT")["class"] == "UNCLASSIFIED"
    assert itemOf(items, f"balance {tieBalance}", "TRAN-SOURCE")["class"] == "UNCLASSIFIED"
    assert itemOf(items, "account 00000002003", "ACCT-CREDIT-LIMIT")["class"] == "UNCLASSIFIED"
    assert counts(items)["UNCLASSIFIED"] == 3


def test_two_engines_that_agree_and_both_break_a_rule_show_in_the_verdicts(expectation, decisions, rulebook):
    legacy, target = laneFrom(expectation, "legacy"), laneFrom(expectation, "target")
    for lane in (legacy, target):
        lane["transactions"][0]["TRAN-SOURCE"] = "Batch"    # INT-R06 says System
    items = replay.compareLanes(expectation, legacy, target, decisions)
    assert counts(items)["UNCLASSIFIED"] == 0               # the comparison sees nothing: they agree
    rows, _ = replay.ruleVerdicts(expectation, legacy, target, decisions, rulebook)
    row = next(r for r in rows if r["rule"] == "INT-R06")
    assert (row["legacy"], row["target"]) == ("VIOLATES", "VIOLATES")
    assert row["legacyEvidence"][0]["got"] == "Batch" and row["legacyEvidence"][0]["required"] == "System"
    others = {r["rule"]: (r["legacy"], r["target"]) for r in rows if r["rule"] != "INT-R06"}
    assert "VIOLATES" not in {v for pair in others.values() for v in pair}


def test_conforming_engines_and_rules_the_case_does_not_exercise(expectation, decisions, rulebook):
    lane = laneFrom(expectation, "legacy")
    rows, technical = replay.ruleVerdicts(expectation, lane, lane, decisions, rulebook)
    verdicts = {r["rule"]: r["legacy"] for r in rows}
    assert technical == {"legacy": [], "target": []}
    assert verdicts["INT-Q01"] == "CONFORMS" and verdicts["INT-R05"] == "CONFORMS"
    assert verdicts["INT-R03"] == "NOT EXERCISED" and verdicts["INT-H03"] == "NOT EXERCISED"
    assert "VIOLATES" not in verdicts.values()


def test_another_option_of_an_open_rule_breaks_that_rule_only(expectation, decisions, rulebook):
    legacy, target = laneFrom(expectation, "legacy"), laneFrom(expectation, "target")
    target["transactions"][0]["TRAN-AMT"] = "12.36"
    rows, _ = replay.ruleVerdicts(expectation, legacy, target, decisions, rulebook)
    verdicts = {r["rule"]: r["target"] for r in rows}
    assert verdicts["INT-Q01"] == "VIOLATES" and verdicts["INT-R05"] == "CONFORMS"
    # once option B is approved, the engine that still truncates is the one that breaks INT-Q01
    target["accounts"][0]["ACCT-CURR-BAL"] = "1247.86"      # the account carries the rounded cent too
    target["transactions"][1]["TRAN-AMT"] = "12.35"         # 12.345 rounds half up as well
    rows, _ = replay.ruleVerdicts(expectation, legacy, target, approve(decisions, "INT-Q01", "B"), rulebook)
    row = next(r for r in rows if r["rule"] == "INT-Q01")
    assert (row["legacy"], row["target"]) == ("VIOLATES", "CONFORMS")


def test_gate_fails_only_on_unclassified():
    def result(unclassified):
        return {"counts": {"MATCH": 5, "INTENDED_CHANGE": 1, "UNRESOLVED": 3, "UNCLASSIFIED": unclassified},
                "compared": 9 + unclassified, "rulesExercised": ["INT-R01"], "case": "x",
                "violations": {"legacy": ["INT-R06"], "target": []}}
    assert replay.summarise([result(0)])["gate"] == "PASS"      # a violated rule is reported, not gated
    assert replay.summarise([result(0), result(1)])["gate"] == "FAIL"
    lines, total = replay.gateLines([("golden", [result(0)])])
    assert lines[-1] == "gate: PASS (UNCLASSIFIED = 0)" and total["rulesTotal"] == 20
    assert "rule verdicts, legacy: VIOLATES INT-R06" in lines


def test_minus_zero_is_zero_but_text_is_compared_as_text():
    assert replay.sameValue("-0.00", "0.00")
    assert not replay.sameValue("12.35", "12.36")
    assert not replay.sameValue("0005", "5")


@pytest.mark.parametrize("spec, expected", [
    ([], "must be a JSON object"),
    ({"accounts": [], "balances": []}, "'accounts' must be a list with at least one entry"),
    ({"accounts": [{"id": 1, "balance": "ten"}], "balances": [{"account": 1, "type": "01", "category": 1, "balance": "1"}]},
     "'ten' is not a number"),
    ({"accounts": [{"id": 1}], "balances": [{"account": 2, "type": "01", "category": 1, "balance": "1"}]},
     "account 2 is not in 'accounts'"),
    ({"accounts": [{"id": 1}], "balances": [{"account": 1, "type": "1", "category": 1, "balance": "1.005"}]},
     "more than two decimals"),
])
def test_invalid_input_is_refused_in_plain_words(spec, expected):
    with pytest.raises(cases.InvalidCase) as refused:
        cases.checkCase(spec)
    assert expected in str(refused.value)


def test_probes_are_valid_and_cover_what_was_asked():
    probes = cases.loadProbes()
    for probe in probes:
        cases.checkCase(probe)
    names = " ".join(p["name"] for p in probes)
    for wanted in ("tie", "last-account", "no-disclosure-group", "negative"):
        assert wanted in names
    assert cases.fixturesRoot not in cases.labCasesRoot.parents
