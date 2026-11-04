#!/usr/bin/env python3
"""Generate the deterministic fixture inputs under fixtures/<chain>/<case>/input.

All data is synthetic (account numbers 1001.., card numbers 4000..., invented
merchants).  Records are encoded through tools/parity/copybook.py so that
every field honours the copybook picture (zoned overpunch signs as written
by ``cobc -fsign=EBCDIC``).  Re-running the script is idempotent; the golden
outputs under expected/ are produced separately by ``make record``.
"""
from __future__ import annotations

import sys
from decimal import ROUND_DOWN, ROUND_HALF_EVEN, Decimal
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))
from tools.parity.copybook import encode_record  # noqa: E402

FIXTURES = ROOT / "fixtures"
DATE_WINDOW = "2022-01-01 2022-07-06"
ORIG_TS = "2022-07-04-09.15.00.000000"

TRANTYPE = [("01", "Purchase"), ("02", "Payment"), ("03", "Credit"), ("04", "Authorization"), ("05", "Refund")]
TRANCATG = [("01", 1, "Regular Sales Draft"), ("01", 2, "Cash Advance"), ("02", 1, "Payment - Bank Transfer"),
            ("03", 1, "Credit to Account"), ("05", 1, "Refund - Merchandise")]
# group, type, category, annual rate %
DISCGRP = [("GOLD", "01", 1, "12.00"), ("GOLD", "01", 2, "18.00"), ("GOLD", "02", 1, "0"), ("GOLD", "03", 1, "0"),
           ("GOLD", "05", 1, "0"),
           ("STANDARD", "01", 1, "18.50"), ("STANDARD", "01", 2, "24.00"), ("STANDARD", "02", 1, "0"),
           ("STANDARD", "03", 1, "0"), ("STANDARD", "05", 1, "0"),
           ("DEFAULT", "01", 1, "21.00"), ("DEFAULT", "01", 2, "27.00"), ("DEFAULT", "02", 1, "0"),
           ("DEFAULT", "03", 1, "0"), ("DEFAULT", "05", 1, "0")]


def card(acct: int) -> str:
    return f"4000{acct:012d}"


def account(acct: int, bal: str, limit: str, group: str, expiry: str = "2026-12-31",
            cyc_credit: str = "0", cyc_debit: str = "0", status: str = "Y") -> dict:
    return {"ACCT-ID": acct, "ACCT-ACTIVE-STATUS": status, "ACCT-CURR-BAL": bal, "ACCT-CREDIT-LIMIT": limit,
            "ACCT-CASH-CREDIT-LIMIT": str(Decimal(limit) / 2), "ACCT-OPEN-DATE": "2019-03-15",
            "ACCT-EXPIRAION-DATE": expiry, "ACCT-REISSUE-DATE": expiry, "ACCT-CURR-CYC-CREDIT": cyc_credit,
            "ACCT-CURR-CYC-DEBIT": cyc_debit, "ACCT-ADDR-ZIP": "10115", "ACCT-GROUP-ID": group}


def xref(acct: int, card_no: str | None = None) -> dict:
    return {"XREF-CARD-NUM": card_no or card(acct), "XREF-CUST-ID": acct, "XREF-ACCT-ID": acct}


def tcatbal(acct: int, typ: str, cat: int, bal: str) -> dict:
    return {"TRANCAT-ACCT-ID": acct, "TRANCAT-TYPE-CD": typ, "TRANCAT-CD": cat, "TRAN-CAT-BAL": bal}


def tran(seq: int, typ: str, cat: int, amt: str, card_no: str, desc: str, merchant: str = "Nordlicht Buchhandel",
         city: str = "Hamburg", orig_ts: str = ORIG_TS) -> dict:
    return {"DALYTRAN-ID": f"{seq:016d}", "DALYTRAN-TYPE-CD": typ, "DALYTRAN-CAT-CD": cat,
            "DALYTRAN-SOURCE": "POS TERM" if typ in ("01", "04") else "OPERATOR", "DALYTRAN-DESC": desc,
            "DALYTRAN-AMT": amt, "DALYTRAN-MERCHANT-ID": 100000000 + seq, "DALYTRAN-MERCHANT-NAME": merchant,
            "DALYTRAN-MERCHANT-CITY": city, "DALYTRAN-MERCHANT-ZIP": "20095", "DALYTRAN-CARD-NUM": card_no,
            "DALYTRAN-ORIG-TS": orig_ts, "DALYTRAN-PROC-TS": ""}


def write(path: Path, layout: str, records: list[dict]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(b"".join(encode_record(layout, r).rstrip(b" ") + b"\n" for r in records))


def reference_files(inp: Path, discgrp: list | None = None) -> None:
    write(inp / "AWS.M2.CARDDEMO.TRANTYPE.PS.txt", "CVTRA03Y",
          [{"TRAN-TYPE": t, "TRAN-TYPE-DESC": d} for t, d in TRANTYPE])
    write(inp / "AWS.M2.CARDDEMO.TRANCATG.PS.txt", "CVTRA04Y",
          [{"TRAN-TYPE-CD": t, "TRAN-CAT-CD": c, "TRAN-CAT-TYPE-DESC": d} for t, c, d in TRANCATG])
    write(inp / "AWS.M2.CARDDEMO.DISCGRP.PS.txt", "CVTRA02Y",
          [{"DIS-ACCT-GROUP-ID": g, "DIS-TRAN-TYPE-CD": t, "DIS-TRAN-CAT-CD": c, "DIS-INT-RATE": r}
           for g, t, c, r in sorted(discgrp or DISCGRP, key=lambda x: (x[0].ljust(10), x[1], x[2]))])


def posting_case(name: str, readme: str, accounts: list[dict], xrefs: list[dict], balances: list[dict],
                 trans: list[dict]) -> None:
    inp = FIXTURES / "posting" / name / "input"
    reference_files(inp)
    write(inp / "AWS.M2.CARDDEMO.ACCTDATA.PS.txt", "CVACT01Y", sorted(accounts, key=lambda r: r["ACCT-ID"]))
    write(inp / "AWS.M2.CARDDEMO.CARDXREF.PS.txt", "CVACT03Y", sorted(xrefs, key=lambda r: r["XREF-CARD-NUM"]))
    write(inp / "AWS.M2.CARDDEMO.TCATBALF.PS.txt", "CVTRA01Y",
          sorted(balances, key=lambda r: (r["TRANCAT-ACCT-ID"], r["TRANCAT-TYPE-CD"], r["TRANCAT-CD"])))
    write(inp / "AWS.M2.CARDDEMO.DALYTRAN.PS.txt", "CVTRA06Y", trans)
    (inp / "AWS.M2.CARDDEMO.DALYTRAN.PS.INIT.txt").write_text("")
    (inp / "AWS.M2.CARDDEMO.DATEPARM.txt").write_text(DATE_WINDOW + "\n")
    (inp.parent / "README.md").write_text(readme.strip() + "\n")


def interest_case(name: str, readme: str, accounts: list[dict], xrefs: list[dict], balances: list[dict],
                  discgrp: list | None = None) -> None:
    inp = FIXTURES / "interest" / name / "input"
    reference_files(inp, discgrp)
    write(inp / "AWS.M2.CARDDEMO.ACCTDATA.PS.txt", "CVACT01Y", sorted(accounts, key=lambda r: r["ACCT-ID"]))
    write(inp / "AWS.M2.CARDDEMO.CARDXREF.PS.txt", "CVACT03Y", sorted(xrefs, key=lambda r: r["XREF-CARD-NUM"]))
    write(inp / "AWS.M2.CARDDEMO.TCATBALF.PS.txt", "CVTRA01Y",
          sorted(balances, key=lambda r: (r["TRANCAT-ACCT-ID"], r["TRANCAT-TYPE-CD"], r["TRANCAT-CD"])))
    (inp.parent / "README.md").write_text(readme.strip() + "\n")


def trantype_case(name: str, readme: str, lines: list[str], seed: list[tuple[str, str]]) -> None:
    inp = FIXTURES / "trantype-update" / name / "input"
    (inp / "db").mkdir(parents=True, exist_ok=True)
    (inp / "INPFILE.txt").write_text("".join(line.ljust(53) .rstrip() + "\n" for line in lines))
    values = ",".join(f"('{t}','{d}')" for t, d in seed)
    (inp / "db" / "seed.sql").write_text(f"INSERT INTO carddemo.transaction_type (tr_type, tr_description) VALUES {values};\n")
    (inp.parent / "README.md").write_text(readme.strip() + "\n")


def dynamic_case(name: str, readme: str, sql: list[str], updates: list[str], seed: list[tuple[str, str]]) -> None:
    inp = FIXTURES / "trantype-dynamic" / name / "input"
    (inp / "db").mkdir(parents=True, exist_ok=True)
    (inp / "DYNSQL.txt").write_text("".join(line + "\n" for line in sql))
    (inp / "INPFILE.txt").write_text("".join(line + "\n" for line in updates))
    values = ",".join(f"('{t}','{d}')" for t, d in seed)
    (inp / "db" / "seed.sql").write_text(f"INSERT INTO carddemo.transaction_type (tr_type, tr_description) VALUES {values};\n")
    (inp.parent / "README.md").write_text(readme.strip() + "\n")


def monthly_interest(balance: str, rate: str, rounding) -> Decimal:
    return (Decimal(balance) * Decimal(rate) / 1200).quantize(Decimal("0.01"), rounding=rounding)


def assert_tie(balance: str, rate: str, is_tie: bool) -> None:
    trunc = monthly_interest(balance, rate, ROUND_DOWN)
    half_even = monthly_interest(balance, rate, ROUND_HALF_EVEN)
    assert (trunc != half_even) == is_tie, f"{balance} @ {rate}: trunc {trunc} vs half-even {half_even}"


def main() -> None:
    # ---------------------------------------------------------------- posting
    posting_case("p01-baseline-mixed", """
# p01-baseline-mixed
Two active accounts, four valid transactions (purchase, purchase, payment, refund).
Everything posts; CBTRN02C ends with RC 0 and the TRANREPT report lists all four.
""", [account(1001, "500.00", "5000.00", "GOLD"), account(1002, "1200.50", "3000.00", "STANDARD")],
        [xref(1001), xref(1002)],
        [tcatbal(1001, "01", 1, "500.00"), tcatbal(1001, "02", 1, "0"), tcatbal(1002, "01", 1, "1200.50"),
         tcatbal(1002, "05", 1, "0")],
        [tran(1, "01", 1, "125.00", card(1001), "Bookshop purchase"),
         tran(2, "01", 1, "80.25", card(1002), "Grocery purchase", "Elbmarkt Lebensmittel", "Hamburg"),
         tran(3, "02", 1, "-200.00", card(1001), "Payment received - bank transfer", "", ""),
         tran(4, "05", 1, "-15.75", card(1002), "Refund - returned merchandise")])

    posting_case("p02-invalid-card", """
# p02-invalid-card
One transaction carries a card number with no CARDXREF entry.
CBTRN02C rejects it with reason 100 (INVALID CARD NUMBER FOUND) and ends with RC 4;
the other transaction posts normally.
""", [account(1001, "500.00", "5000.00", "GOLD")], [xref(1001)], [tcatbal(1001, "01", 1, "500.00")],
        [tran(1, "01", 1, "42.00", card(1001), "Valid purchase"),
         tran(2, "01", 1, "99.99", "4000999999999999", "Card unknown to the estate")])

    posting_case("p03-account-missing", """
# p03-account-missing
CARDXREF maps the card to account 1099, which has no ACCTDATA record.
CBTRN02C rejects the transaction with reason 101 (ACCOUNT RECORD NOT FOUND), RC 4.
""", [account(1001, "500.00", "5000.00", "GOLD")], [xref(1001), xref(1099)], [tcatbal(1001, "01", 1, "500.00")],
        [tran(1, "01", 1, "10.00", card(1099), "Orphaned card"),
         tran(2, "01", 1, "20.00", card(1001), "Valid purchase")])

    posting_case("p04-overlimit-reject", """
# p04-overlimit-reject
Account 1003 has a 1000.00 credit limit and 950.00 already in the current cycle.
The first purchase (50.00) lands exactly on the limit and is accepted (CBTRN02C
accepts when limit >= cycle credit - cycle debit + amount); the second (100.00)
is rejected with reason 102 (OVERLIMIT TRANSACTION), RC 4.
""", [account(1003, "950.00", "1000.00", "STANDARD", cyc_credit="950.00")], [xref(1003)],
        [tcatbal(1003, "01", 1, "950.00")],
        [tran(1, "01", 1, "50.00", card(1003), "Purchase exactly at the limit"),
         tran(2, "01", 1, "100.00", card(1003), "Purchase over the limit")])

    posting_case("p05-expired-account", """
# p05-expired-account
Account 1004 expired on 2022-01-31, before the transaction's origin date; CBTRN02C
rejects with reason 103 (TRANSACTION RECEIVED AFTER ACCT EXPIRATION), RC 4.
Account 1005 expires on the origin date itself (2022-07-04) and is accepted
(the test is expiry >= origin date).
""", [account(1004, "100.00", "2000.00", "GOLD", expiry="2022-01-31"),
      account(1005, "100.00", "2000.00", "GOLD", expiry="2022-07-04")], [xref(1004), xref(1005)],
        [tcatbal(1004, "01", 1, "100.00"), tcatbal(1005, "01", 1, "100.00")],
        [tran(1, "01", 1, "30.00", card(1004), "Purchase on expired account"),
         tran(2, "01", 1, "30.00", card(1005), "Purchase on account expiring today")])

    posting_case("p06-credits-new-category", """
# p06-credits-new-category
Negative amounts (payment and credit) flow into ACCT-CURR-CYC-DEBIT, and the
(account, type 03, category 0001) balance record does not exist yet, so CBTRN02C
creates it ("TCATBAL record not found ... Creating").  RC 0.
""", [account(1006, "800.00", "4000.00", "STANDARD", cyc_credit="800.00")], [xref(1006)],
        [tcatbal(1006, "01", 1, "800.00")],
        [tran(1, "02", 1, "-300.00", card(1006), "Payment received", "", ""),
         tran(2, "03", 1, "-25.50", card(1006), "Goodwill credit", "", ""),
         tran(3, "01", 2, "60.00", card(1006), "Cash advance at ATM", "Stadtbank ATM 17", "Bremen")])

    posting_case("p07-empty-day", """
# p07-empty-day
No daily transactions at all.  CBTRN02C ends with RC 0, writes nothing to TRANSACT
or DALYREJS, and TRANREPT produces a report with headers and zero detail lines.
""", [account(1001, "500.00", "5000.00", "GOLD")], [xref(1001)], [tcatbal(1001, "01", 1, "500.00")], [])

    # --------------------------------------------------------------- interest
    assert_tie("1000.00", "12.00", False)
    assert_tie("2400.00", "18.50", False)
    interest_case("i01-baseline", """
# i01-baseline
Two accounts in groups GOLD (12.00 %) and STANDARD (18.50 %).
1000.00 * 12.00 / 1200 = 10.00 and 2400.00 * 18.50 / 1200 = 37.00; both exact.
CBACT04C writes one interest transaction per account and adds it to ACCT-CURR-BAL.
""", [account(2001, "1000.00", "5000.00", "GOLD", cyc_credit="1000.00"),
      account(2002, "2400.00", "6000.00", "STANDARD", cyc_credit="2400.00")],
        [xref(2001), xref(2002)], [tcatbal(2001, "01", 1, "1000.00"), tcatbal(2002, "01", 1, "2400.00")])

    assert_tie("1235.50", "12.00", True)
    assert_tie("1234.50", "12.00", False)
    interest_case("i02-tie-half-cent", """
# i02-tie-half-cent  (the rounding-tie case)
Account 2003: 1235.50 * 12.00 / 1200 = 12.355 — exactly half a cent.
CBACT04C's COMPUTE has no ROUNDED phrase, so COBOL truncates to **12.35**;
a half-even (banker's) reference rounds to **12.36**.  Account 2004: 1234.50 gives
12.345, where half-even also yields 12.34, so only account 2003 diverges.
`make parity-naive CASE=i02-tie-half-cent` must fail on TRAN-AMT and ACCT-CURR-BAL
for account 2003 only.
""", [account(2003, "1235.50", "5000.00", "GOLD", cyc_credit="1235.50"),
      account(2004, "1234.50", "5000.00", "GOLD", cyc_credit="1234.50")],
        [xref(2003), xref(2004)], [tcatbal(2003, "01", 1, "1235.50"), tcatbal(2004, "01", 1, "1234.50")])

    assert_tie("1000.00", "12.00", False)
    interest_case("i03-zero-rate-skipped", """
# i03-zero-rate-skipped
Account 2005 has a payment-category balance (type 02) whose disclosure rate is 0.00 %,
and a purchase balance at 12.00 %.  CBACT04C skips the zero-rate category (no
transaction written) and posts interest only for the purchase category.
""", [account(2005, "700.00", "5000.00", "GOLD", cyc_credit="1000.00", cyc_debit="300.00")], [xref(2005)],
        [tcatbal(2005, "01", 1, "1000.00"), tcatbal(2005, "02", 1, "-300.00")])

    assert_tie("1000.00", "21.00", False)
    interest_case("i04-default-group-fallback", """
# i04-default-group-fallback
Account 2006 belongs to group PLATINUM, which has no DISCGRP rows.  CBACT04C reports
"DISCLOSURE GROUP RECORD MISSING / TRY WITH DEFAULT GROUP CODE" and uses the DEFAULT
group: 1000.00 * 21.00 / 1200 = 17.50.
""", [account(2006, "1000.00", "5000.00", "PLATINUM", cyc_credit="1000.00")], [xref(2006)],
        [tcatbal(2006, "01", 1, "1000.00")])

    assert_tie("1000.00", "12.00", False)
    assert_tie("500.00", "18.00", False)
    interest_case("i05-multi-category", """
# i05-multi-category
Account 2007 (GOLD) carries two balances: purchases 1000.00 at 12.00 % (10.00) and
cash advances 500.00 at 18.00 % (7.50).  Two interest transactions are written and
the account balance grows by the 17.50 total.
""", [account(2007, "1500.00", "5000.00", "GOLD", cyc_credit="1500.00")], [xref(2007)],
        [tcatbal(2007, "01", 1, "1000.00"), tcatbal(2007, "01", 2, "500.00")])

    assert_tie("-300.00", "12.00", False)
    interest_case("i06-negative-balance", """
# i06-negative-balance
Account 2008 has a credit (negative) purchase balance of -300.00 at 12.00 %.
CBACT04C computes -3.00 and writes a negative interest transaction, reducing the
account balance.
""", [account(2008, "-300.00", "5000.00", "GOLD", cyc_debit="300.00")], [xref(2008)],
        [tcatbal(2008, "01", 1, "-300.00")])

    assert_tie("1000.33", "18.50", False)
    assert_tie("777.77", "21.00", False)
    interest_case("i07-nontie-rounding", """
# i07-nontie-rounding
Sub-cent remainders that are not ties: 1000.33 * 18.50 / 1200 = 15.42175 and
777.77 * 21.00 / 1200 = 13.610975.  Truncation and half-even both give 15.42 and 13.61,
so this case passes under `make parity` and `make parity-naive`.
""", [account(2009, "1000.33", "5000.00", "STANDARD", cyc_credit="1000.33"),
      account(2010, "777.77", "5000.00", "DEFAULT", cyc_credit="777.77")],
        [xref(2009), xref(2010)], [tcatbal(2009, "01", 1, "1000.33"), tcatbal(2010, "01", 1, "777.77")])

    # --------------------------------------------------------- trantype-update
    seed = [("01", "Purchase"), ("02", "Payment"), ("03", "Credit")]
    trantype_case("t01-add", "# t01-add\nTwo new transaction types are inserted.",
                  ["A04Authorization", "A05Refund"], seed)
    trantype_case("t02-update", "# t02-update\nThe description of type 02 is updated.",
                  ["U02Payment - bank transfer"], seed)
    trantype_case("t03-delete", "# t03-delete\nType 03 is deleted.", ["D03"], seed)
    trantype_case("t04-comments", "# t04-comments\nComment lines (`*`) are ignored; one add is applied.",
                  ["*  Nightly maintenance batch", "A06Fee", "*  end"], seed)
    trantype_case("t05-delete-missing", """
# t05-delete-missing
Deleting type 09, which does not exist.  Db2 returns SQLCODE +100; COBTUPDT only tests
for negative SQLCODE, so it still prints RECORD DELETED SUCCESSFULLY and the table is
unchanged.  This legacy quirk is captured in the golden SYSOUT.
""", ["D09"], seed)
    trantype_case("t06-duplicate-add", """
# t06-duplicate-add
Inserting type 01 again violates the primary key.  Db2 would return SQLCODE -803; via
Open COBOL ESQL PostgreSQL returns a negative SQLCODE as well.  COBTUPDT reports the
error and the table is unchanged.
""", ["A01Purchase"], seed)
    trantype_case("t07-mixed-batch", "# t07-mixed-batch\nAdd, update, delete and a comment in one input file.",
                  ["A07Chargeback", "U01Purchase - card present", "*  skip", "D02"], seed)

    # --------------------------------------------------------- trantype-dynamic
    dynamic_case("d01-update-then-apply", """
# d01-update-then-apply
CBTRNDYN prepares and executes the UPDATE read from DYNSQL (dynamic SQL), then
dynamically CALLs COBTUPDT (PARMS('COBTUPDT')) which applies one add from INPFILE.
Both changes are committed together at end of task.
""", ["-- ad-hoc fix requested by operations", "UPDATE TRANSACTION_TYPE", "   SET TR_DESCRIPTION = 'Payment - SEPA'",
          " WHERE TR_TYPE = '02'"], ["A04Authorization"], seed)
    dynamic_case("d02-delete-statement", "# d02-delete-statement\nDynamic DELETE of type 03; the update file is empty.",
                 ["DELETE FROM TRANSACTION_TYPE WHERE TR_TYPE = '03'"], [], seed)
    dynamic_case("d03-bad-statement", """
# d03-bad-statement
The dynamic statement names a column that does not exist.  EXECUTE fails with SQLCODE
-400 through Open COBOL ESQL, CBTRNDYN sets RC 8 and still calls COBTUPDT.  Under
PostgreSQL the failed statement has aborted the whole unit of work, so COBTUPDT's
UPDATE also fails (SQLCODE -400) and the table is unchanged.  Db2 would have rolled
back only the failed statement and applied the update: a genuine semantic gap for the
migration team to decide on (savepoint per statement, or fail the job).
""", ["UPDATE TRANSACTION_TYPE SET NO_SUCH_COLUMN = 'x' WHERE TR_TYPE = '01'"], ["U01Purchase - updated"], seed)
    print("fixtures generated under", FIXTURES)


if __name__ == "__main__":
    main()
