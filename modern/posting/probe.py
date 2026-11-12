#!/usr/bin/env python3
"""Compare scratch edge-case runs byte-for-byte without writing any golden files.

Run in estate after make modern-build: python3 modern/posting/probe.py.
"""
from pathlib import Path
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[2]
PREFIX = "AWS.M2.CARDDEMO."
WORK = ROOT / "work" / "posting-probes"


def probe(name, change):
    case = ROOT / "fixtures" / "posting" / (".probe-" + name)
    if case.exists():
        raise RuntimeError(f"Scratch case already exists: {case}")
    out = WORK / name
    out.mkdir(parents=True, exist_ok=True)
    try:
        shutil.copytree(ROOT / "fixtures/posting/p01-baseline-mixed/input", case / "input")
        change(case / "input")
        with (out / "legacy.log").open("w") as log:
            subprocess.run(["python3", "-B", "tools/parity/record.py", "posting", case.name,
                            "--out", str(out / "legacy")], cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, check=True)
        with (out / "java.log").open("w") as log:
            subprocess.run(["modern/run-candidate.sh", "posting", case.name, str(out / "java")],
                           cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, check=True)
        expected = {p.relative_to(out / "legacy"): p.read_bytes() for p in (out / "legacy").rglob("*") if p.is_file()}
        actual = {p.relative_to(out / "java"): p.read_bytes() for p in (out / "java").rglob("*") if p.is_file()}
        different = [str(p) for p in expected.keys() | actual.keys() if expected.get(p) != actual.get(p)]
        if different:
            raise AssertionError(f"{name}: differences in {', '.join(sorted(different))}; see {out}")
        print(f"{name}: PASS (all recorded files byte-identical)", flush=True)
    finally:
        shutil.rmtree(case)


def file(directory, suffix):
    return directory / (PREFIX + suffix + ".txt")


def duplicate_daily(d):
    p = file(d, "DALYTRAN.PS")
    rows = p.read_text().splitlines()
    p.write_text("\n".join([rows[0], rows[0], *rows[1:]]) + "\n")


def omit_lookup(d, suffix):
    p = file(d, suffix)
    p.write_text("\n".join(s for s in p.read_text().splitlines() if not s.startswith("01")) + "\n")


def fillers(d):
    for suffix, offset, width in [("ACCTDATA.PS", 122, 178), ("TCATBALF.PS", 28, 22)]:
        p = file(d, suffix)
        p.write_text("\n".join(s.ljust(offset)[:offset] + "AUDIT".ljust(width) for s in p.read_text().splitlines()) + "\n")


def duplicate_repro(d):
    for suffix, offset, replacement in [
            ("ACCTDATA.PS", 11, "N"), ("TCATBALF.PS", 17, "0000009999{"),
            ("CARDXREF.PS", 25, "99999999999"), ("TRANTYPE.PS", 2, "IGNORED"),
            ("TRANCATG.PS", 6, "IGNORED"), ("DISCGRP.PS", 16, "99999{")]:
        p = file(d, suffix)
        rows = p.read_text().splitlines()
        if rows:
            duplicate = rows[0][:offset] + replacement + rows[0][offset + len(replacement):]
            p.write_text("\n".join([*rows, duplicate]) + "\n")
    p = file(d, "DALYTRAN.PS.INIT")
    row = file(d, "DALYTRAN.PS").read_text().splitlines()[0]
    row = "9000000000000001" + row[16:]
    p.write_text(row + "\n" + row + "\n")


def pagination(d):
    p = file(d, "DALYTRAN.PS")
    row = p.read_text().splitlines()[0]
    p.write_text("\n".join(f"{i:016d}" + row[16:] for i in range(1,31)) + "\n")


if __name__ == "__main__":
    WORK.mkdir(parents=True, exist_ok=True)
    probe("duplicate-daily-abend", duplicate_daily)
    probe("missing-report-type", lambda d: omit_lookup(d,"TRANTYPE.PS"))
    probe("missing-report-category", lambda d: omit_lookup(d,"TRANCATG.PS"))
    probe("rewrite-fillers", fillers)
    probe("duplicate-repro", duplicate_repro)
    probe("pagination", pagination)
    probe("date-excluded", lambda d: file(d,"DATEPARM").write_text("2022-07-06 2022-07-06\n"))
    probe("empty-dateparm", lambda d: file(d,"DATEPARM").write_text(""))
