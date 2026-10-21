"""Acceptance tests for CBACT04C (job INTCALC, step STEP15): the interest calculator.

Every test reads only the implementation-neutral fixtures (outputs, run_inputs) and
recomputes the expected values from the inputs, so it runs unchanged with
--impl cobol and --impl java. CBACT04C opens the category balance file for input
only, so the tcatbal output dataset is exactly what the program read.
"""
from collections import defaultdict
from decimal import ROUND_DOWN, ROUND_HALF_UP, Decimal

import pytest
from harness.jcl import job_steps, manifest

CENT = Decimal("0.01")
ZERO = Decimal(0)
DEFAULT_GROUP = "DEFAULT"
ACCOUNT_FIELDS_UPDATED = {"acct_curr_bal", "acct_curr_cyc_credit", "acct_curr_cyc_debit"}

CR144 = "https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/issues/144"
PROMO_GROUP = "GOLDPROMO1"
PROMO_TYPE_CD = "01"
PROMO_CAT_CD = 1
PROMO_RATE = Decimal("4.99")
PROMO_CYCLES = 3
SEEDED_RUN_CYCLE = 1  # the seeded INTCALC run is the first that sees a GOLDPROMO1 account

# R19 is approved by change request #144 but is not in the COBOL, which still charges the
# DEFAULT rate; the assertions hold for any port and are expected to fail against the COBOL.
r19_not_in_cobol = pytest.mark.xfail(
    "config.getoption('--impl') == 'cobol'", strict=True,
    reason=f"change request #144 is not implemented in CBACT04C (COBOL keeps the DEFAULT rate) {CR144}")


def interest_parm():
    return next(s["parm"] for s in job_steps("INTCALC") if s["program"] == "CBACT04C")


def db2_timestamp(current_date):
    """'YYYY/MM/DD HH:MM:SS.hh' -> 'YYYY-MM-DD-HH.MM.SS.hh0000'."""
    day, time = current_date.split(" ")
    hh, mm, rest = time.split(":")
    ss, hundredths = rest.split(".")
    return f"{day.replace('/', '-')}-{hh}.{mm}.{ss}.{hundredths}0000"


def tcatbal_in_key_order(outputs):
    return sorted(outputs["tcatbal"].values(),
                  key=lambda r: (int(r["trancat_acct_id"]), r["trancat_type_cd"], int(r["trancat_cd"])))


def effective_rate(run_inputs, group, type_cd, cat_cd):
    """Rate for the account's group, or the DEFAULT group's rate when that key is missing."""
    discgrp = run_inputs["discgrp"]
    rec = discgrp.get(f"{group}|{type_cd}|{cat_cd}") or discgrp[f"{DEFAULT_GROUP}|{type_cd}|{cat_cd}"]
    return Decimal(rec["dis_int_rate"])


def exact_interest(balance, rate):
    return Decimal(balance) * rate / Decimal(1200)


def expected_interest(outputs, run_inputs):
    """One entry per category balance with a non-zero rate, in category key order."""
    out = []
    for cat in tcatbal_in_key_order(outputs):
        acct = run_inputs["accounts"][cat["trancat_acct_id"]]
        rate = effective_rate(run_inputs, acct["acct_group_id"], cat["trancat_type_cd"], cat["trancat_cd"])
        if rate == 0:
            continue
        exact = exact_interest(cat["tran_cat_bal"], rate)
        out.append({"cat": cat, "rate": rate, "exact": exact,
                    "amount": exact.quantize(CENT, rounding=ROUND_DOWN)})
    return out


def interest_in_id_order(outputs):
    return [outputs["interest"][k] for k in sorted(outputs["interest"])]


def card_to_account(run_inputs):
    return {r["xref_card_num"]: r["xref_acct_id"] for r in run_inputs["cardxref"].values()}


def account_of_interest(rec):
    return str(int(rec["tran_desc"][-11:]))


def sums_by_account(records, cards):
    total = defaultdict(Decimal)
    for r in records:
        total[cards[r["tran_card_num"]]] += Decimal(r["tran_amt"])
    return total


def balance_before_interest(outputs, run_inputs, acct_id):
    """The account balance CBACT04C read: the seeded balance plus what POSTTRAN posted."""
    posted = sums_by_account(outputs["posted"].values(), card_to_account(run_inputs))
    return Decimal(run_inputs["accounts"][acct_id]["acct_curr_bal"]) + posted[acct_id]


def last_account(outputs):
    return tcatbal_in_key_order(outputs)[-1]["trancat_acct_id"]


def is_promotional(run_inputs, cat, cycle=SEEDED_RUN_CYCLE):
    """R19: GOLDPROMO1 purchases (type 01, category 0001) in the account's first three cycles."""
    acct = run_inputs["accounts"][cat["trancat_acct_id"]]
    return (acct["acct_group_id"] == PROMO_GROUP and cat["trancat_type_cd"] == PROMO_TYPE_CD
            and int(cat["trancat_cd"]) == PROMO_CAT_CD and 1 <= cycle <= PROMO_CYCLES)


def promo_accounts(run_inputs):
    return sorted(a["acct_id"] for a in run_inputs["accounts"].values() if a["acct_group_id"] == PROMO_GROUP)


def test_cbact04c_r01_one_transaction_per_rated_category_in_key_order(outputs, run_inputs):
    expected = expected_interest(outputs, run_inputs)
    actual = interest_in_id_order(outputs)
    assert len(actual) == len(expected) > 0
    assert [account_of_interest(r) for r in actual] == [e["cat"]["trancat_acct_id"] for e in expected]


def test_cbact04c_r02_rate_from_account_group_type_and_category(outputs, run_inputs):
    discgrp = run_inputs["discgrp"]
    checked = 0
    for exp, rec in zip(expected_interest(outputs, run_inputs), interest_in_id_order(outputs)):
        cat = exp["cat"]
        group = run_inputs["accounts"][cat["trancat_acct_id"]]["acct_group_id"]
        own = discgrp.get(f"{group}|{cat['trancat_type_cd']}|{cat['trancat_cd']}")
        if own is None or Decimal(cat["tran_cat_bal"]) < 0:
            continue
        rate = Decimal(own["dis_int_rate"])
        assert Decimal(rec["tran_amt"]) == exact_interest(cat["tran_cat_bal"], rate).quantize(CENT, rounding=ROUND_DOWN)
        checked += 1
    assert checked > 0


def test_cbact04c_r03_missing_group_rate_falls_back_to_default(outputs, run_inputs):
    discgrp = run_inputs["discgrp"]
    expected = expected_interest(outputs, run_inputs)
    fallback = 0
    for exp, rec in zip(expected, interest_in_id_order(outputs)):
        cat = exp["cat"]
        group = run_inputs["accounts"][cat["trancat_acct_id"]]["acct_group_id"]
        if f"{group}|{cat['trancat_type_cd']}|{cat['trancat_cd']}" in discgrp or is_promotional(run_inputs, cat):
            continue  # R19 owns the GOLDPROMO1 01/0001 rate
        default = Decimal(discgrp[f"{DEFAULT_GROUP}|{cat['trancat_type_cd']}|{cat['trancat_cd']}"]["dis_int_rate"])
        assert exp["rate"] == default
        assert Decimal(rec["tran_amt"]) == exact_interest(cat["tran_cat_bal"], default).quantize(CENT, rounding=ROUND_DOWN)
        fallback += 1
    assert fallback > 0
    # a group with no disclosure records at all (GOLDPROMO1 on account 51) also uses DEFAULT
    promo = [a for a in run_inputs["accounts"].values()
             if a["acct_group_id"] and not any(k.startswith(a["acct_group_id"] + "|") for k in discgrp)]
    assert promo
    for acct in promo:
        cats = [c for c in outputs["tcatbal"].values() if c["trancat_acct_id"] == acct["acct_id"]]
        rated = [c for c in cats if Decimal(
            discgrp[f"{DEFAULT_GROUP}|{c['trancat_type_cd']}|{c['trancat_cd']}"]["dis_int_rate"]) != 0]
        written = [r for r in outputs["interest"].values() if account_of_interest(r) == acct["acct_id"]]
        assert len(written) == len(rated)


def test_cbact04c_r04_zero_rate_writes_no_transaction(outputs, run_inputs):
    per_account = defaultdict(int)
    zero_rated = 0
    for cat in outputs["tcatbal"].values():
        acct = run_inputs["accounts"][cat["trancat_acct_id"]]
        if effective_rate(run_inputs, acct["acct_group_id"], cat["trancat_type_cd"], cat["trancat_cd"]) != 0:
            per_account[cat["trancat_acct_id"]] += 1
        else:
            zero_rated += 1
    written = defaultdict(int)
    for rec in outputs["interest"].values():
        written[account_of_interest(rec)] += 1
    assert zero_rated > 0
    assert dict(written) == dict(per_account)


def test_cbact04c_r05_monthly_interest_is_balance_times_rate_over_1200(outputs, run_inputs):
    checked = 0
    for exp, rec in zip(expected_interest(outputs, run_inputs), interest_in_id_order(outputs)):
        if Decimal(exp["cat"]["tran_cat_bal"]) < 0 or is_promotional(run_inputs, exp["cat"]):
            continue
        assert Decimal(rec["tran_amt"]) == exp["amount"], rec["tran_id"]
        checked += 1
    assert checked > 0


def test_cbact04c_r06_interest_truncated_to_cents_not_rounded(outputs, run_inputs):
    differs = 0
    for exp, rec in zip(expected_interest(outputs, run_inputs), interest_in_id_order(outputs)):
        if Decimal(exp["cat"]["tran_cat_bal"]) < 0 or is_promotional(run_inputs, exp["cat"]):
            continue
        rounded = exp["exact"].quantize(CENT, rounding=ROUND_HALF_UP)
        truncated = exp["exact"].quantize(CENT, rounding=ROUND_DOWN)
        if rounded != truncated:
            assert Decimal(rec["tran_amt"]) == truncated, rec["tran_id"]
            differs += 1
    assert differs > 0


def test_cbact04c_r07_zero_balance_with_rate_writes_zero_amount(outputs, run_inputs):
    zero = 0
    for exp, rec in zip(expected_interest(outputs, run_inputs), interest_in_id_order(outputs)):
        if Decimal(exp["cat"]["tran_cat_bal"]) == 0:
            assert Decimal(rec["tran_amt"]) == 0
            zero += 1
    assert zero > 0


@pytest.mark.xfail(strict=True, reason="amber: business question https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/issues/141")
def test_cbact04c_r08_negative_balance_earns_no_interest_credit(outputs, run_inputs):
    negative = [e for e in expected_interest(outputs, run_inputs) if Decimal(e["cat"]["tran_cat_bal"]) < 0]
    assert negative, "seeded data must contain a negative category balance with a non-zero rate"
    credits = [r["tran_id"] for r in outputs["interest"].values() if Decimal(r["tran_amt"]) < 0]
    assert credits == []


def test_cbact04c_r09_transaction_id_is_parm_date_plus_sequence(outputs):
    parm = interest_parm()
    ids = sorted(outputs["interest"])
    assert ids == [f"{parm}{n:06d}" for n in range(1, len(ids) + 1)]


def test_cbact04c_r10_fixed_transaction_fields(outputs):
    assert outputs["interest"]
    for rec in outputs["interest"].values():
        assert rec["tran_type_cd"] == "01"
        assert int(rec["tran_cat_cd"]) == 5
        assert rec["tran_source"] == "System"
        assert int(rec["tran_merchant_id"]) == 0
        assert rec["tran_merchant_name"] == ""
        assert rec["tran_merchant_city"] == ""
        assert rec["tran_merchant_zip"] == ""


def test_cbact04c_r11_description_names_the_account(outputs, run_inputs):
    actual = interest_in_id_order(outputs)
    expected = expected_interest(outputs, run_inputs)
    assert len(actual) == len(expected)
    for exp, rec in zip(expected, actual):
        assert rec["tran_desc"] == f"Int. for a/c {int(exp['cat']['trancat_acct_id']):011d}"


def test_cbact04c_r12_card_number_from_cross_reference(outputs, run_inputs):
    by_account = {r["xref_acct_id"]: r["xref_card_num"] for r in run_inputs["cardxref"].values()}
    actual = interest_in_id_order(outputs)
    expected = expected_interest(outputs, run_inputs)
    assert len(actual) == len(expected)
    for exp, rec in zip(expected, actual):
        assert rec["tran_card_num"] == by_account[exp["cat"]["trancat_acct_id"]]


def test_cbact04c_r13_timestamps_are_run_time_in_db2_format(outputs):
    expected = db2_timestamp(manifest()["run"]["current_date"])
    assert outputs["interest"]
    for rec in outputs["interest"].values():
        assert rec["tran_orig_ts"] == expected
        assert rec["tran_proc_ts"] == expected


def test_cbact04c_r14_account_balance_gains_total_interest(outputs, run_inputs):
    cards = card_to_account(run_inputs)
    interest = sums_by_account(outputs["interest"].values(), cards)
    last = last_account(outputs)
    accounts = {c["trancat_acct_id"] for c in outputs["tcatbal"].values()} - {last}
    assert accounts
    for acct_id in accounts:
        expected = balance_before_interest(outputs, run_inputs, acct_id) + interest[acct_id]
        assert Decimal(outputs["accounts"][acct_id]["acct_curr_bal"]) == expected, acct_id


def test_cbact04c_r15_cycle_credit_and_debit_reset_to_zero(outputs):
    last = last_account(outputs)
    accounts = {c["trancat_acct_id"] for c in outputs["tcatbal"].values()} - {last}
    assert accounts
    for acct_id in accounts:
        acct = outputs["accounts"][acct_id]
        assert Decimal(acct["acct_curr_cyc_credit"]) == 0, acct_id
        assert Decimal(acct["acct_curr_cyc_debit"]) == 0, acct_id


def test_cbact04c_r16_other_account_fields_unchanged(outputs, run_inputs):
    assert set(outputs["accounts"]) == set(run_inputs["accounts"])
    for acct_id, before in run_inputs["accounts"].items():
        after = outputs["accounts"][acct_id]
        for field, value in before.items():
            if field not in ACCOUNT_FIELDS_UPDATED:
                assert after[field] == value, (acct_id, field)


@pytest.mark.xfail(strict=True, reason="amber: business question https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/issues/142")
def test_cbact04c_r17_last_account_is_updated_too(outputs, run_inputs):
    last = last_account(outputs)
    interest = sums_by_account(outputs["interest"].values(), card_to_account(run_inputs))
    assert interest[last] != 0, "seeded data must give the last account non-zero interest"
    acct = outputs["accounts"][last]
    assert Decimal(acct["acct_curr_bal"]) == balance_before_interest(outputs, run_inputs, last) + interest[last]
    assert Decimal(acct["acct_curr_cyc_credit"]) == 0
    assert Decimal(acct["acct_curr_cyc_debit"]) == 0


def test_cbact04c_r18_category_balances_not_changed_by_interest(outputs, run_inputs):
    cards = card_to_account(run_inputs)
    posted = defaultdict(Decimal)
    for r in outputs["posted"].values():
        posted[f"{cards[r['tran_card_num']]}|{r['tran_type_cd']}|{r['tran_cat_cd']}"] += Decimal(r["tran_amt"])
    assert set(run_inputs["tcatbal"]) <= set(outputs["tcatbal"])
    for key, cat in outputs["tcatbal"].items():
        before = Decimal(run_inputs["tcatbal"][key]["tran_cat_bal"]) if key in run_inputs["tcatbal"] else ZERO
        assert Decimal(cat["tran_cat_bal"]) == before + posted[key], key


@r19_not_in_cobol
def test_cbact04c_r19_goldpromo1_purchases_at_promotional_rate(outputs, run_inputs):
    checked = 0
    for exp, rec in zip(expected_interest(outputs, run_inputs), interest_in_id_order(outputs)):
        cat = exp["cat"]
        if not is_promotional(run_inputs, cat):
            continue
        promo = exact_interest(cat["tran_cat_bal"], PROMO_RATE).quantize(CENT, rounding=ROUND_DOWN)
        assert Decimal(rec["tran_amt"]) == promo, rec["tran_id"]
        checked += 1
    assert checked > 0, "seeded data must contain a GOLDPROMO1 type 01 category 0001 balance"


def test_cbact04c_r19_goldpromo1_other_categories_keep_default_rate(outputs, run_inputs):
    discgrp = run_inputs["discgrp"]
    promo = set(promo_accounts(run_inputs))
    assert promo
    expected = expected_interest(outputs, run_inputs)
    by_cat = {(e["cat"]["trancat_acct_id"], e["cat"]["trancat_type_cd"], e["cat"]["trancat_cd"]): rec
              for e, rec in zip(expected, interest_in_id_order(outputs))}
    others = [c for c in outputs["tcatbal"].values()
              if c["trancat_acct_id"] in promo and not is_promotional(run_inputs, c)]
    assert others
    for cat in others:
        default = Decimal(discgrp[f"{DEFAULT_GROUP}|{cat['trancat_type_cd']}|{cat['trancat_cd']}"]["dis_int_rate"])
        rec = by_cat.get((cat["trancat_acct_id"], cat["trancat_type_cd"], cat["trancat_cd"]))
        if default == 0:
            assert rec is None
        elif Decimal(cat["tran_cat_bal"]) >= 0:
            assert Decimal(rec["tran_amt"]) == exact_interest(cat["tran_cat_bal"], default).quantize(
                CENT, rounding=ROUND_DOWN), rec["tran_id"]
        else:
            assert rec is not None  # sign handling is R08
    written = [r for r in outputs["interest"].values() if account_of_interest(r) in promo]
    assert len(written) == sum(1 for e in expected if e["cat"]["trancat_acct_id"] in promo)


def test_cbact04c_r19_other_accounts_unchanged(outputs, run_inputs):
    promo = set(promo_accounts(run_inputs))
    checked = 0
    for exp, rec in zip(expected_interest(outputs, run_inputs), interest_in_id_order(outputs)):
        cat = exp["cat"]
        if cat["trancat_acct_id"] in promo:
            continue
        assert account_of_interest(rec) == cat["trancat_acct_id"]
        if Decimal(cat["tran_cat_bal"]) >= 0:
            assert Decimal(rec["tran_amt"]) == exp["amount"], rec["tran_id"]
            checked += 1
    assert checked > 0
    cards = card_to_account(run_inputs)
    interest = sums_by_account(outputs["interest"].values(), cards)
    last = last_account(outputs)
    for acct_id in {c["trancat_acct_id"] for c in outputs["tcatbal"].values()} - promo - {last}:
        expected = balance_before_interest(outputs, run_inputs, acct_id) + interest[acct_id]
        assert Decimal(outputs["accounts"][acct_id]["acct_curr_bal"]) == expected, acct_id


@r19_not_in_cobol
def test_cbact04c_r19_balance_gains_promotional_total(outputs, run_inputs):
    promo = set(promo_accounts(run_inputs))
    last = last_account(outputs)
    assert promo - {last}, "seeded data must give a GOLDPROMO1 account that is not the last account (R17)"
    total = defaultdict(Decimal)
    for exp, rec in zip(expected_interest(outputs, run_inputs), interest_in_id_order(outputs)):
        cat = exp["cat"]
        if cat["trancat_acct_id"] not in promo:
            continue
        if is_promotional(run_inputs, cat):
            total[cat["trancat_acct_id"]] += exact_interest(cat["tran_cat_bal"], PROMO_RATE).quantize(
                CENT, rounding=ROUND_DOWN)
        else:
            total[cat["trancat_acct_id"]] += Decimal(rec["tran_amt"])
    for acct_id in promo - {last}:
        expected = balance_before_interest(outputs, run_inputs, acct_id) + total[acct_id]
        assert Decimal(outputs["accounts"][acct_id]["acct_curr_bal"]) == expected, acct_id
