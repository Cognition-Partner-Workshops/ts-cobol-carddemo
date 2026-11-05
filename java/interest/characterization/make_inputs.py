#!/usr/bin/env python3
"""Write the synthetic inputs for the CBACT04C characterization cases.

These cases are *not* golden fixtures: they live next to the Java port and
exist to pin down legacy behaviour the seven golden interest cases do not
exercise (abends, overflow, negative zero, load-order rules).  Their expected
outputs are recorded from the real COBOL with record_characterization.py.
"""
from __future__ import annotations

import sys
from decimal import Decimal
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT))
from tools.parity.copybook import encode_record  # noqa: E402

CASES = Path(__file__).resolve().parent / "cases" / "interest"
DISCGRP = ROOT / "fixtures" / "interest" / "i01-baseline" / "input" / "AWS.M2.CARDDEMO.DISCGRP.PS.txt"


def acct(acct_id, bal, group, credit="0", debit="0"):
    return encode_record("CVACT01Y", {
        "ACCT-ID": acct_id, "ACCT-ACTIVE-STATUS": "Y", "ACCT-CURR-BAL": Decimal(bal),
        "ACCT-CREDIT-LIMIT": Decimal("5000.00"), "ACCT-CASH-CREDIT-LIMIT": Decimal("2500.00"),
        "ACCT-OPEN-DATE": "2019-03-15", "ACCT-EXPIRAION-DATE": "2026-12-31", "ACCT-REISSUE-DATE": "2026-12-31",
        "ACCT-CURR-CYC-CREDIT": Decimal(credit), "ACCT-CURR-CYC-DEBIT": Decimal(debit),
        "ACCT-ADDR-ZIP": "10115", "ACCT-GROUP-ID": group})


def xref(card, acct_id):
    return encode_record("CVACT03Y", {"XREF-CARD-NUM": card, "XREF-CUST-ID": acct_id % 10**9, "XREF-ACCT-ID": acct_id})


def tcat(acct_id, type_cd, cat_cd, bal):
    return encode_record("CVTRA01Y", {"TRANCAT-ACCT-ID": acct_id, "TRANCAT-TYPE-CD": type_cd,
                                      "TRANCAT-CD": cat_cd, "TRAN-CAT-BAL": Decimal(bal)})


def card(acct_id, n=0):
    return f"4000{n:04d}{acct_id:08d}"


def plain(accounts):
    return [acct(a, "100.00", "GOLD") for a in accounts], [xref(card(a), a) for a in accounts]


CASE_DATA = {
    # 3 accounts with balances: first two rewritten (cycle amounts zeroed), last untouched,
    # an account absent from TCATBALF untouched, type-02 category at rate 0 skipped.
    "c01-three-accounts": dict(
        acct=[acct(3001, "1000.00", "GOLD", "120.00", "45.50"), acct(3002, "2400.00", "STANDARD", "10.00", "20.00"),
              acct(3003, "500.00", "GOLD", "7.00", "8.00"), acct(3004, "42.00", "GOLD", "1.00", "2.00")],
        xref=[xref(card(a), a) for a in (3001, 3002, 3003, 3004)],
        tcat=[tcat(3001, "01", 1, "1000.00"), tcat(3001, "01", 2, "250.50"), tcat(3001, "02", 1, "75.00"),
              tcat(3002, "01", 1, "333.33"), tcat(3003, "01", 1, "500.00")]),
    # truncation is toward zero for negatives; -0.0033 truncates to a negative zero amount
    "c02-negative-truncation": dict(
        acct=[acct(3101, "-1235.50", "GOLD"), acct(3102, "-0.33", "GOLD"), acct(3103, "10.00", "GOLD")],
        xref=[xref(card(a), a) for a in (3101, 3102, 3103)],
        tcat=[tcat(3101, "01", 1, "-1235.50"), tcat(3102, "01", 1, "-0.33"), tcat(3103, "01", 1, "10.00")]),
    # an account whose only category has rate 0 is still rewritten (cycle amounts zeroed, balance unchanged)
    "c03-zero-rate-then-next": dict(
        acct=[acct(3201, "800.00", "GOLD", "55.00", "66.00"), acct(3202, "900.00", "GOLD")],
        xref=[xref(card(a), a) for a in (3201, 3202)],
        tcat=[tcat(3201, "02", 1, "800.00"), tcat(3202, "01", 1, "900.00")]),
    # high-order truncation of S9(9)V99 interest / total and S9(10)V99 balance (no ON SIZE ERROR)
    "c04-overflow": dict(
        acct=[acct(3301, "9999999999.99", "HIGH"), acct(3302, "1.00", "HIGH")],
        xref=[xref(card(a), a) for a in (3301, 3302)],
        tcat=[tcat(3301, "01", 1, "999999999.99"), tcat(3301, "01", 2, "999999999.99"), tcat(3302, "01", 1, "1.00")],
        extra_disc=[b"HIGH      01000199999I", b"HIGH      01000299999I"], sort_disc=True),
    # same rows as c04 but appended after STANDARD: the sequential KSDS load skips the
    # out-of-sequence HIGH rows, so every lookup falls back to DEFAULT
    "c09-discgrp-out-of-sequence": dict(
        acct=[acct(3301, "9999999999.99", "HIGH"), acct(3302, "1.00", "HIGH")],
        xref=[xref(card(a), a) for a in (3301, 3302)],
        tcat=[tcat(3301, "01", 1, "999999999.99"), tcat(3301, "01", 2, "999999999.99"), tcat(3302, "01", 1, "1.00")],
        extra_disc=[b"HIGH      01000199999I", b"HIGH      01000299999I"]),
    # an account with two cards (alternate key with duplicates): the first card in key order is used
    "c10-two-cards-one-account": dict(
        acct=[acct(3801, "100.00", "GOLD"), acct(3802, "200.00", "GOLD")],
        xref=[xref(card(3801, 1), 3801), xref(card(3801, 9), 3801), xref(card(3802, 9), 3802)],
        tcat=[tcat(3801, "01", 1, "100.00"), tcat(3802, "01", 1, "200.00")]),
    # default disclosure group missing for the type/category: abend U999, RC 12, earlier work kept
    "c05-default-missing-abend": dict(
        acct=[acct(3401, "300.00", "GOLD", "3.00", "4.00"), acct(3402, "400.00", "PLATINUM")],
        xref=[xref(card(a), a) for a in (3401, 3402)],
        tcat=[tcat(3401, "01", 1, "300.00"), tcat(3402, "01", 9, "400.00")]),
    # TCATBALF names an account that is not on ACCTDATA
    "c06-account-missing-abend": dict(
        acct=[acct(3501, "300.00", "GOLD")],
        xref=[xref(card(3501), 3501), xref(card(3502), 3502)],
        tcat=[tcat(3501, "01", 1, "300.00"), tcat(3502, "01", 1, "100.00")]),
    # account exists but has no card cross-reference
    "c07-xref-missing-abend": dict(
        acct=[acct(3601, "300.00", "GOLD"), acct(3602, "200.00", "GOLD")],
        xref=[xref(card(3601), 3601)],
        tcat=[tcat(3601, "01", 1, "300.00"), tcat(3602, "01", 1, "200.00")]),
    # load rules: out-of-sequence and duplicate keys are skipped by the KSDS load; an account with
    # two cards uses the first card in primary-key order
    "c08-load-order": dict(
        acct=[acct(3701, "100.00", "GOLD"), acct(3703, "300.00", "GOLD"), acct(3702, "200.00", "GOLD")],
        xref=[xref(card(3701, 9), 3701), xref(card(3701, 1), 3701), xref(card(3702), 3702), xref(card(3703), 3703)],
        tcat=[tcat(3701, "01", 1, "100.00"), tcat(3701, "01", 1, "999.00"), tcat(3703, "01", 1, "300.00"),
              tcat(3702, "01", 1, "200.00"), tcat(3703, "01", 2, "50.00")]),
}


def write(path: Path, records) -> None:
    path.write_bytes(b"".join(r.rstrip(b" ") + b"\n" for r in records))


def main() -> None:
    base_disc = [line.encode("latin-1") for line in DISCGRP.read_text(encoding="latin-1").splitlines()]
    for name, data in CASE_DATA.items():
        inp = CASES / name / "input"
        inp.mkdir(parents=True, exist_ok=True)
        write(inp / "AWS.M2.CARDDEMO.ACCTDATA.PS.txt", data["acct"])
        write(inp / "AWS.M2.CARDDEMO.CARDXREF.PS.txt", data["xref"])
        write(inp / "AWS.M2.CARDDEMO.TCATBALF.PS.txt", data["tcat"])
        disc = base_disc + data.get("extra_disc", [])
        if data.get("sort_disc"):
            disc = sorted(disc, key=lambda r: r[:16])
        write(inp / "AWS.M2.CARDDEMO.DISCGRP.PS.txt", disc)
        print(f"wrote {inp}")


if __name__ == "__main__":
    main()
