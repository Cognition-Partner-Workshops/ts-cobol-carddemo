"""Regenerate the overlay records and the daily transaction file.

The output is committed. Rerun only when the seeded cases change:
    python3 spec/fixtures/make_fixtures.py
"""
import random
import sys
from decimal import Decimal
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent))

from harness.datasets import INPUTS  # noqa: E402
from harness.inputs import SEED_DIR, read_lines  # noqa: E402
from harness.records import decode_record, encode_record  # noqa: E402

D = Decimal
rng = random.Random(2610)


def acct(acct_id, limit, cyc_credit, expires, group="A000000000", bal="0"):
    return {
        "acct_id": acct_id, "acct_active_status": "Y", "acct_curr_bal": D(bal),
        "acct_credit_limit": D(limit), "acct_cash_credit_limit": D(limit) / 2,
        "acct_open_date": "2019-04-01", "acct_expiraion_date": expires,
        "acct_reissue_date": expires, "acct_curr_cyc_credit": D(cyc_credit),
        "acct_curr_cyc_debit": D(0), "acct_addr_zip": "10001", "acct_group_id": group,
    }


def card(n):
    return f"40000026100000{n:02d}"


ACCOUNTS = [
    acct(51, "5000.00", "0", "2027-03-31", group="GOLDPROMO1", bal="1500.00"),
    acct(52, "3000.00", "0", "2022-06-30", bal="250.00"),
    acct(53, "500.00", "421.37", "2026-09-30", bal="421.37"),
    acct(54, "100.00", "90.00", "2022-05-31", bal="90.00"),
]
XREF = [{"xref_card_num": card(n), "xref_cust_id": 900 + n, "xref_acct_id": n} for n in (51, 52, 53, 54, 99)]
TCATBAL = [
    {"trancat_acct_id": 51, "trancat_type_cd": "01", "trancat_cd": 1, "tran_cat_bal": D("1200.00")},
    {"trancat_acct_id": 51, "trancat_type_cd": "07", "trancat_cd": 1, "tran_cat_bal": D("300.00")},
    {"trancat_acct_id": 52, "trancat_type_cd": "01", "trancat_cd": 1, "tran_cat_bal": D("250.00")},
    {"trancat_acct_id": 53, "trancat_type_cd": "01", "trancat_cd": 1, "tran_cat_bal": D("421.37")},
    {"trancat_acct_id": 54, "trancat_type_cd": "01", "trancat_cd": 1, "tran_cat_bal": D("90.00")},
]

MERCHANTS = [
    ("Harbor Grocery", "Portland", "97201"), ("Elm Street Pharmacy", "Dayton", "45402"),
    ("Northside Fuel", "Omaha", "68102"), ("Blue Kettle Cafe", "Tucson", "85701"),
    ("Corner Hardware", "Albany", "12207"), ("Lakeview Books", "Madison", "53703"),
]
KINDS = [
    ("01", 1, "POS TERM", "Purchase at {m}", (1, 150), 1),
    ("01", 2, "POS TERM", "Cash advance at {m}", (20, 120), 1),
    ("02", 2, "OPERATOR", "Electronic payment", (25, 200), -1),
    ("03", 1, "OPERATOR", "Credit to account", (5, 60), -1),
    ("05", 1, "OPERATOR", "Refund from {m}", (2, 80), -1),
]


def amount(lo, hi):
    return D(rng.randint(lo * 100, hi * 100)) / 100


def daily(n, card_num, kind=0, amt=None, day="2022-07-17"):
    type_cd, cat_cd, source, desc, (lo, hi), sign = KINDS[kind]
    m, city, zip_code = rng.choice(MERCHANTS)
    value = amt if amt is not None else amount(lo, hi) * sign
    ts = f"{day} {rng.randint(0, 23):02d}:{rng.randint(0, 59):02d}:{rng.randint(0, 59):02d}.000000"
    return {
        "dalytran_id": f"2610{n:012d}", "dalytran_type_cd": type_cd, "dalytran_cat_cd": cat_cd,
        "dalytran_source": source, "dalytran_desc": desc.format(m=m), "dalytran_amt": D(value),
        "dalytran_merchant_id": rng.randint(100000000, 999999999), "dalytran_merchant_name": m,
        "dalytran_merchant_city": city, "dalytran_merchant_zip": zip_code,
        "dalytran_card_num": card_num, "dalytran_orig_ts": ts, "dalytran_proc_ts": "",
    }


def main():
    xref_layout = INPUTS["cardxref"].layout
    base_cards = [decode_record(line, xref_layout)["xref_card_num"] for line in read_lines(SEED_DIR / "cardxref.txt")]
    rows = []
    for _ in range(176):
        kind = rng.choices(range(len(KINDS)), weights=[70, 8, 8, 7, 7])[0]
        rows.append((rng.choice(base_cards), kind, None, "2022-07-17"))
    rows += [(card(51), rng.choice([0, 0, 1, 4]), None, "2022-07-17") for _ in range(6)]
    rows += [(f"40000026100090{n:02d}", 0, None, "2022-07-17") for n in range(1, 6)]
    rows += [(card(99), 0, None, "2022-07-17") for _ in range(4)]
    rows += [(card(53), 0, D(v), "2022-07-17") for v in ("95.00", "30.00", "48.63", "0.01")]
    rows += [(card(52), 0, None, "2022-07-17") for _ in range(3)]
    rows += [(card(54), 0, D("50.00"), "2022-07-17"), (card(52), 0, D("12.50"), "2022-06-30")]
    seeded = rows[176:]
    plain = rows[:176]
    rng.shuffle(plain)
    # keep the over-limit sequence in order so the boundary case lands as written
    ordered, cursor = [], 0
    for i, row in enumerate(plain):
        ordered.append(row)
        if i % 8 == 7 and cursor < len(seeded):
            ordered.append(seeded[cursor])
            cursor += 1
    ordered += seeded[cursor:]
    records = [daily(n, c, k, a, d) for n, (c, k, a, d) in enumerate(ordered, start=1)]

    def write(path, layout, recs):
        path.write_text("".join(encode_record(r, layout) + "\n" for r in recs))

    write(HERE / "dailytran.txt", INPUTS["dailytran"].layout, records)
    overlay = HERE / "overlay"
    overlay.mkdir(exist_ok=True)
    write(overlay / "acctdata.txt", INPUTS["accounts"].layout, ACCOUNTS)
    write(overlay / "cardxref.txt", xref_layout, XREF)
    write(overlay / "tcatbal.txt", INPUTS["tcatbal"].layout, TCATBAL)
    print(f"dailytran: {len(records)} records")


if __name__ == "__main__":
    main()
