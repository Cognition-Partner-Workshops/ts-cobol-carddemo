#!/usr/bin/env python3
"""Naive half-even reference for the `interest` chain (parity-naive).

    naive_ref.py interest <case> --out <dir>

Re-implements CBACT04C's arithmetic in Python with *banker's rounding*
(ROUND_HALF_EVEN) - what a careless Java port using BigDecimal.setScale(2,
HALF_EVEN) would do - and writes the candidate ACCTDATA / SYSTRAN datasets in
the recorded format so tools/parity/compare.py can diff them against the
golden files.  Everything else (control flow, record layout, timestamps,
SYSOUT, return codes) is reproduced faithfully from the legacy run, so the
only possible divergence is the rounding rule.  COBOL truncates (COMPUTE
without ROUNDED); the two agree except when the third decimal is exactly 5
(fixtures/interest/i02-tie-half-cent).

Note the reference deliberately mirrors a CBACT04C control-flow quirk: the
last account on TCATBALF never has its balance rewritten (END-OF-FILE is
tested before the ELSE branch that performs 1050-UPDATE-ACCOUNT).
"""
from __future__ import annotations

import argparse
import json
import shutil
import sys
from decimal import ROUND_HALF_EVEN, Decimal
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))
from tools.parity.copybook import decode_record, encode_record, parse_copybook  # noqa: E402

FIXTURES = ROOT / "fixtures"
ACCT_DSN = "AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS"
TRAN_DSN = "AWS.M2.CARDDEMO.SYSTRAN.G0001V00"
DEFAULT_TS = "2022-07-05-10.00.00.000000"


def read_records(path: Path, layout: str) -> list[dict]:
    lrecl = sum(f.length for f in parse_copybook(layout))
    return [decode_record(layout, line.ljust(lrecl).encode("latin-1"))
            for line in path.read_text(encoding="latin-1").splitlines()]


def write_records(path: Path, layout: str, records: list[dict]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(b"".join(encode_record(layout, r).rstrip(b" ") + b"\n" for r in records))


def interest_half_even(balance: Decimal, rate: Decimal) -> Decimal:
    return (balance * rate / Decimal(1200)).quantize(Decimal("0.01"), rounding=ROUND_HALF_EVEN)


def run(case: str, out: Path) -> None:
    case_dir = FIXTURES / "interest" / case
    inp, expected = case_dir / "input", case_dir / "expected"
    if not expected.exists():
        raise SystemExit(f"{expected} missing - run `make record CHAIN=interest CASE={case}` first")
    chain = json.loads((ROOT / "tools" / "runjcl" / "chains.json").read_text())["interest"]
    parm_date = chain.get("parm_date", "2022071800")

    accounts = {r["ACCT-ID"]: r for r in read_records(inp / "AWS.M2.CARDDEMO.ACCTDATA.PS.txt", "CVACT01Y")}
    cards = {r["XREF-ACCT-ID"]: r["XREF-CARD-NUM"] for r in read_records(inp / "AWS.M2.CARDDEMO.CARDXREF.PS.txt", "CVACT03Y")}
    rates = {(r["DIS-ACCT-GROUP-ID"], r["DIS-TRAN-TYPE-CD"], r["DIS-TRAN-CAT-CD"]): r["DIS-INT-RATE"]
             for r in read_records(inp / "AWS.M2.CARDDEMO.DISCGRP.PS.txt", "CVTRA02Y")}
    balances = sorted(read_records(inp / "AWS.M2.CARDDEMO.TCATBALF.PS.txt", "CVTRA01Y"),
                      key=lambda r: (r["TRANCAT-ACCT-ID"], r["TRANCAT-TYPE-CD"], r["TRANCAT-CD"]))

    # timestamps come from the frozen clock of the recorded run
    golden_trans = expected / "datasets" / f"{TRAN_DSN}.txt"
    stamp = DEFAULT_TS
    if golden_trans.exists() and golden_trans.stat().st_size:
        stamp = read_records(golden_trans, "CVTRA05Y")[0]["TRAN-ORIG-TS"]

    trans: list[dict] = []
    last_acct, total = None, Decimal(0)
    for bal in balances:
        acct_id = bal["TRANCAT-ACCT-ID"]
        if acct_id != last_acct:
            if last_acct is not None:
                update_account(accounts[last_acct], total)
            last_acct, total = acct_id, Decimal(0)
        key = (accounts[acct_id]["ACCT-GROUP-ID"], bal["TRANCAT-TYPE-CD"], bal["TRANCAT-CD"])
        rate = rates.get(key, rates.get(("DEFAULT", key[1], key[2])))
        if rate is None:
            raise SystemExit(f"no disclosure group for {key} (CBACT04C would abend U0999)")
        if rate == 0:
            continue
        amount = interest_half_even(bal["TRAN-CAT-BAL"], rate)
        total += amount
        trans.append({"TRAN-ID": f"{parm_date}{len(trans) + 1:06d}", "TRAN-TYPE-CD": "01", "TRAN-CAT-CD": 5,
                      "TRAN-SOURCE": "System", "TRAN-DESC": f"Int. for a/c {int(acct_id):011d}", "TRAN-AMT": amount,
                      "TRAN-MERCHANT-ID": 0, "TRAN-MERCHANT-NAME": "", "TRAN-MERCHANT-CITY": "",
                      "TRAN-MERCHANT-ZIP": "", "TRAN-CARD-NUM": cards[acct_id], "TRAN-ORIG-TS": stamp,
                      "TRAN-PROC-TS": stamp})
    # the last account is intentionally NOT updated (legacy control-flow quirk, see module docstring)

    if out.exists():
        shutil.rmtree(out)
    shutil.copytree(expected, out)  # SYSOUT, rc.json, manifest and untouched datasets
    write_records(out / "datasets" / f"{ACCT_DSN}.txt", "CVACT01Y", [accounts[k] for k in sorted(accounts)])
    write_records(out / "datasets" / f"{TRAN_DSN}.txt", "CVTRA05Y", trans)
    print(f"interest/{case}: naive half-even reference wrote {len(trans)} interest transactions to {out}")


def update_account(acct: dict, total: Decimal) -> None:
    acct["ACCT-CURR-BAL"] = acct["ACCT-CURR-BAL"] + total
    acct["ACCT-CURR-CYC-CREDIT"] = Decimal(0)
    acct["ACCT-CURR-CYC-DEBIT"] = Decimal(0)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("chain", choices=["interest"])
    parser.add_argument("case")
    parser.add_argument("--out", required=True, type=Path)
    args = parser.parse_args()
    run(args.case, args.out)
    return 0


if __name__ == "__main__":
    sys.exit(main())
