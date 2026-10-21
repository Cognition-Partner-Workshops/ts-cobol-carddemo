"""Acceptance tests for CBTRN02C (job POSTTRAN): post the daily transaction file.

Every expectation is recomputed from the run inputs, so the same tests run with
--impl cobol and --impl java. The account and category balance outputs are read
after the whole batch path (POSTTRAN then INTCALC); where INTCALC also touches a
field, the oracle takes its interest records into account.
"""
from collections import defaultdict
from decimal import Decimal
from pathlib import Path

import pytest
import yaml

SPEC = Path(__file__).resolve().parents[2]

REASON_CARD = ("100", "INVALID CARD NUMBER FOUND")
REASON_ACCOUNT = ("101", "ACCOUNT RECORD NOT FOUND")
REASON_OVERLIMIT = ("102", "OVERLIMIT TRANSACTION")
REASON_EXPIRED = ("103", "TRANSACTION RECEIVED AFTER ACCT EXPIRATION")

DAILY_TO_POSTED = {
    "dalytran_id": "tran_id", "dalytran_type_cd": "tran_type_cd", "dalytran_cat_cd": "tran_cat_cd",
    "dalytran_source": "tran_source", "dalytran_desc": "tran_desc", "dalytran_amt": "tran_amt",
    "dalytran_merchant_id": "tran_merchant_id", "dalytran_merchant_name": "tran_merchant_name",
    "dalytran_merchant_city": "tran_merchant_city", "dalytran_merchant_zip": "tran_merchant_zip",
    "dalytran_card_num": "tran_card_num", "dalytran_orig_ts": "tran_orig_ts",
}


def D(v):
    return Decimal(v)


def daily(run_inputs):
    return list(run_inputs["dailytran"].values())


def account_of(run_inputs, tran):
    xref = run_inputs["cardxref"].get(tran["dalytran_card_num"])
    return xref["xref_acct_id"] if xref else None


def reason(outputs, tran_id):
    r = outputs["rejects"][tran_id]
    return r["validation_fail_reason"], r["validation_fail_reason_desc"]


def simulate(run_inputs, measure):
    """Replay the daily file in order and return {tran_id: reason or None}.

    measure(state, amount) is the projected figure compared with the credit limit.
    """
    accounts = run_inputs["accounts"]
    state = {k: {"bal": D(a["acct_curr_bal"]), "cr": D(a["acct_curr_cyc_credit"]),
                 "db": D(a["acct_curr_cyc_debit"]), "limit": D(a["acct_credit_limit"]),
                 "expires": a["acct_expiraion_date"]} for k, a in accounts.items()}
    result = {}
    for t in daily(run_inputs):
        acct = account_of(run_inputs, t)
        if acct is None:
            result[t["dalytran_id"]] = REASON_CARD
            continue
        if acct not in state:
            result[t["dalytran_id"]] = REASON_ACCOUNT
            continue
        s, amt = state[acct], D(t["dalytran_amt"])
        why = None
        if measure(s, amt) > s["limit"]:
            why = REASON_OVERLIMIT
        if s["expires"] < t["dalytran_orig_ts"][:10]:
            why = REASON_EXPIRED
        result[t["dalytran_id"]] = why
        if why is None:
            s["bal"] += amt
            if amt >= 0:
                s["cr"] += amt
            else:
                s["db"] += amt
    return result


def cycle_measure(s, amt):
    return s["cr"] - s["db"] + amt


def balance_measure(s, amt):
    return s["bal"] + amt


def posted_by(run_inputs, outputs, key_fn):
    sums = defaultdict(Decimal)
    for t in daily(run_inputs):
        if t["dalytran_id"] in outputs["posted"]:
            sums[key_fn(t)] += D(t["dalytran_amt"])
    return sums


def test_cbtrn02c_r01_every_daily_record_posted_or_rejected(outputs, run_inputs):
    ids = [t["dalytran_id"] for t in daily(run_inputs)]
    posted, rejected = set(outputs["posted"]), set(outputs["rejects"])
    daily_posted = posted & set(ids)
    assert posted & rejected == set()
    assert daily_posted | rejected == set(ids)
    assert len(daily_posted) + len(rejected) == len(ids)


def test_cbtrn02c_r02_unknown_card_rejected_100(outputs, run_inputs):
    unknown = [t["dalytran_id"] for t in daily(run_inputs) if account_of(run_inputs, t) is None]
    assert unknown, "seeded data has no unknown card"
    for tid in unknown:
        assert tid not in outputs["posted"]
        assert reason(outputs, tid) == REASON_CARD


def test_cbtrn02c_r03_card_without_account_rejected_101(outputs, run_inputs):
    orphans = [t["dalytran_id"] for t in daily(run_inputs)
               if account_of(run_inputs, t) is not None and account_of(run_inputs, t) not in run_inputs["accounts"]]
    assert orphans, "seeded data has no card whose account is missing"
    for tid in orphans:
        assert tid not in outputs["posted"]
        assert reason(outputs, tid) == REASON_ACCOUNT


def _seeded_discriminating_cases(run_inputs):
    """Transactions where the balance reading and the cycle-to-date reading decide differently."""
    by_cycle = simulate(run_inputs, cycle_measure)
    by_balance = simulate(run_inputs, balance_measure)
    return [tid for tid in by_balance if by_balance[tid] != by_cycle[tid]]


@pytest.mark.xfail(strict=True, reason="amber: business question https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/issues/140")
def test_cbtrn02c_r04_overlimit_judged_on_balance(outputs, run_inputs):
    """Business reading: reject as over limit when current balance + amount exceeds the credit limit.

    The reading is only demonstrated once the seeded data holds a transaction on
    which it and the observed cycle-to-date reading decide differently.
    """
    expected = simulate(run_inputs, balance_measure)
    for tid, why in expected.items():
        if why is None:
            assert tid in outputs["posted"], tid
        else:
            assert reason(outputs, tid) == why, tid
    assert _seeded_discriminating_cases(run_inputs), \
        "no seeded transaction separates the balance reading from the cycle-to-date reading"


def test_cbtrn02c_r05_limit_is_inclusive_and_rejects_do_not_count(outputs, run_inputs):
    # account 53: limit 500.00, opening 421.37; 95.00 rejected, 30.00 and 48.63 reach
    # exactly 500.00 and post, the next 0.01 is over by one cent
    acct = run_inputs["accounts"]["53"]
    seq = [t for t in daily(run_inputs) if account_of(run_inputs, t) == "53"]
    assert [t["dalytran_amt"] for t in seq] == ["95.00", "30.00", "48.63", "0.01"]
    opening = D(acct["acct_curr_cyc_credit"]) - D(acct["acct_curr_cyc_debit"])
    limit = D(acct["acct_credit_limit"])
    assert opening + D("95.00") > limit
    assert opening + D("30.00") + D("48.63") == limit
    first, second, third, fourth = (t["dalytran_id"] for t in seq)
    assert reason(outputs, first) == REASON_OVERLIMIT
    assert second in outputs["posted"] and third in outputs["posted"]
    assert reason(outputs, fourth) == REASON_OVERLIMIT


def test_cbtrn02c_r06_after_expiration_rejected_103_same_day_accepted(outputs, run_inputs):
    late, same_day = [], []
    for t in daily(run_inputs):
        acct = run_inputs["accounts"].get(account_of(run_inputs, t) or "")
        if acct is None:
            continue
        day = t["dalytran_orig_ts"][:10]
        if acct["acct_expiraion_date"] < day:
            late.append(t["dalytran_id"])
        elif acct["acct_expiraion_date"] == day:
            same_day.append(t["dalytran_id"])
    assert late and same_day
    for tid in late:
        assert reason(outputs, tid) == REASON_EXPIRED
    for tid in same_day:
        assert tid in outputs["posted"]


def test_cbtrn02c_r07_expiration_reason_wins_over_overlimit(outputs, run_inputs):
    # account 54: limit 100.00, cycle 90.00, expired 2022-05-31; 50.00 on 2022-07-17 fails both
    acct = run_inputs["accounts"]["54"]
    both = [t for t in daily(run_inputs) if account_of(run_inputs, t) == "54"]
    assert both
    for t in both:
        assert D(acct["acct_curr_cyc_credit"]) - D(acct["acct_curr_cyc_debit"]) + D(t["dalytran_amt"]) \
            > D(acct["acct_credit_limit"])
        assert acct["acct_expiraion_date"] < t["dalytran_orig_ts"][:10]
        assert reason(outputs, t["dalytran_id"]) == REASON_EXPIRED


def test_cbtrn02c_r08_reject_record_is_daily_record_plus_reason(outputs, run_inputs):
    assert outputs["rejects"]
    for tid, rec in outputs["rejects"].items():
        src = run_inputs["dailytran"][tid]
        for field, value in src.items():
            assert rec[field] == value, (tid, field)
        assert rec["validation_fail_reason"] in {r[0] for r in (REASON_CARD, REASON_ACCOUNT, REASON_OVERLIMIT, REASON_EXPIRED)}
        assert rec["validation_fail_reason_desc"]


def test_cbtrn02c_r09_posted_record_copies_daily_fields(outputs, run_inputs):
    daily_posted = [t for t in daily(run_inputs) if t["dalytran_id"] in outputs["posted"]]
    assert daily_posted
    for t in daily_posted:
        rec = outputs["posted"][t["dalytran_id"]]
        for src, dst in DAILY_TO_POSTED.items():
            assert rec[dst] == t[src], (t["dalytran_id"], dst)


def test_cbtrn02c_r10_processing_timestamp_is_run_time(outputs, run_inputs):
    manifest = yaml.safe_load((SPEC / "manifest.yaml").read_text())
    date, time = manifest["run"]["current_date"].split()
    yyyy, mm, dd = date.split("/")
    hh, mi, rest = time.split(":")
    ss, hundredths = rest.split(".")
    expected = f"{yyyy}-{mm}-{dd}-{hh}.{mi}.{ss}.{hundredths}0000"
    for t in daily(run_inputs):
        if t["dalytran_id"] in outputs["posted"]:
            assert outputs["posted"][t["dalytran_id"]]["tran_proc_ts"] == expected


def _tcat_key(run_inputs):
    return lambda t: f"{account_of(run_inputs, t)}|{t['dalytran_type_cd']}|{t['dalytran_cat_cd']}"


def test_cbtrn02c_r11_category_balance_adds_posted_amounts(outputs, run_inputs):
    sums = posted_by(run_inputs, outputs, _tcat_key(run_inputs))
    for key, rec in run_inputs["tcatbal"].items():
        assert D(outputs["tcatbal"][key]["tran_cat_bal"]) == D(rec["tran_cat_bal"]) + sums.get(key, Decimal(0)), key


def test_cbtrn02c_r12_missing_category_balance_created(outputs, run_inputs):
    sums = posted_by(run_inputs, outputs, _tcat_key(run_inputs))
    new = {k: v for k, v in sums.items() if k not in run_inputs["tcatbal"]}
    assert new, "seeded data creates no category balance"
    assert set(outputs["tcatbal"]) == set(run_inputs["tcatbal"]) | set(new)
    for key, total in new.items():
        rec = outputs["tcatbal"][key]
        acct, type_cd, cat_cd = key.split("|")
        assert (rec["trancat_acct_id"], rec["trancat_type_cd"], rec["trancat_cd"]) == (acct, type_cd, cat_cd)
        assert D(rec["tran_cat_bal"]) == total


def test_cbtrn02c_r13_account_balance_adds_posted_amounts(outputs, run_inputs):
    posted = posted_by(run_inputs, outputs, lambda t: account_of(run_inputs, t))
    # INTCALC later adds its interest records to the same balance; accounts with
    # nothing posted are INTCALC's alone and are left to its specification
    interest = defaultdict(Decimal)
    for rec in outputs["interest"].values():
        interest[run_inputs["cardxref"][rec["tran_card_num"]]["xref_acct_id"]] += D(rec["tran_amt"])
    assert posted
    for acct, total in posted.items():
        opening = D(run_inputs["accounts"][acct]["acct_curr_bal"])
        assert D(outputs["accounts"][acct]["acct_curr_bal"]) == opening + total + interest.get(acct, Decimal(0)), acct


def test_cbtrn02c_r14_rejected_transactions_change_nothing(outputs, run_inputs):
    rejected = set(outputs["rejects"])
    assert rejected
    assert rejected.isdisjoint(outputs["posted"])
    key = _tcat_key(run_inputs)
    touched = defaultdict(set)
    for t in daily(run_inputs):
        if account_of(run_inputs, t) in run_inputs["accounts"]:
            touched[key(t)].add(t["dalytran_id"])
    only_rejected = [k for k, ids in touched.items() if ids <= rejected]
    # account 54 category 01/1 receives only a rejected transaction
    assert only_rejected
    for k in only_rejected:
        if k in run_inputs["tcatbal"]:
            assert outputs["tcatbal"][k]["tran_cat_bal"] == run_inputs["tcatbal"][k]["tran_cat_bal"], k
        else:
            assert k not in outputs["tcatbal"], k
