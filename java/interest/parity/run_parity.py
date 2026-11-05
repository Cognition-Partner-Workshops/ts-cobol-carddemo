#!/usr/bin/env python3
"""Parity for the Java INTCALC/CBACT04C port.

For every case it runs the Java job on the case inputs, then
  1. runs the repository comparator (tools/parity/compare.py) unchanged: field-level
     equality via the copybooks, plus SYSOUT and rc.json, and
  2. computes a row hash per output record on both sides: SHA-256 over the decoded,
     non-filler copybook fields (``NAME=value`` joined by ``|``) and over the raw record
     bytes space-filled to LRECL.  A record matches when both hashes agree.

    python3 java/interest/parity/run_parity.py --jar java/interest/target/carddemo-interest.jar \
        [--characterization] [--report work/parity/report-java.md] [case ...]

Exit code 1 when any case has a field difference or a row-hash mismatch.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
sys.path.insert(0, str(ROOT))
from tools.parity import compare  # noqa: E402
from tools.parity.copybook import decode_record, parse_copybook  # noqa: E402

CHAIN = "interest"
CLOCK = "2022-07-05 10:00:00.00"
PARM = "2022071800"
GOLDEN = ROOT / "fixtures"
CHARACTERIZATION = ROOT / "java" / "interest" / "characterization" / "cases"


def records(path: Path, lrecl: int) -> list[bytes]:
    if not path.exists():
        return []
    return [line.rstrip(b"\r").ljust(lrecl)[:lrecl] for line in path.read_bytes().split(b"\n")[:-1]]


def row_hash(layout: str, record: bytes) -> tuple[str, str]:
    fields = decode_record(layout, record)
    canonical = "|".join(f"{k}={v}" for k, v in fields.items())
    return hashlib.sha256(canonical.encode()).hexdigest(), hashlib.sha256(record).hexdigest()


def key_of(layout: str, record: bytes) -> str:
    first = next(f for f in parse_copybook(layout) if not f.is_filler)
    return record[first.offset:first.offset + first.length].decode("latin-1")


def run_java(jar: Path, case_dir: Path, out: Path) -> int:
    cmd = ["java", "-jar", str(jar), "--input", str(case_dir / "input"), "--out", str(out),
           "--parm", PARM, "--clock", CLOCK]
    return subprocess.run(cmd, check=False).returncode


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("cases", nargs="*")
    ap.add_argument("--jar", type=Path, required=True)
    ap.add_argument("--characterization", action="store_true",
                    help="run the COBOL-recorded characterization cases instead of the golden fixtures")
    ap.add_argument("--report", type=Path, default=ROOT / "work" / "parity" / "report-java.md")
    ap.add_argument("--out", type=Path, default=ROOT / "work" / "parity-java")
    args = ap.parse_args()
    args.report = args.report.resolve()

    fixtures = CHARACTERIZATION if args.characterization else GOLDEN
    compare.FIXTURES = fixtures
    cases = args.cases or sorted(p.name for p in (fixtures / CHAIN).iterdir() if (p / "expected").is_dir())
    summary, rows, failed = [], [], False
    args.report.parent.mkdir(parents=True, exist_ok=True)
    detail = args.report.with_name(args.report.stem + "-fields.md")
    detail.unlink(missing_ok=True)

    for case in cases:
        case_dir = fixtures / CHAIN / case
        out = args.out / CHAIN / case
        java_rc = run_java(args.jar, case_dir, out)
        diffs, checked = compare.compare_case(CHAIN, case, out)
        compare.write_report(detail, CHAIN, case, "java", diffs, checked)
        manifest = json.loads((case_dir / "expected" / "manifest.json").read_text())
        matched = total = 0
        for dsn, meta in sorted(manifest.items()):
            exp = records(case_dir / "expected" / "datasets" / f"{dsn}.txt", meta["lrecl"])
            act = records(out / "datasets" / f"{dsn}.txt", meta["lrecl"])
            for i in range(max(len(exp), len(act))):
                e = row_hash(meta["layout"], exp[i]) if i < len(exp) else ("-", "-")
                a = row_hash(meta["layout"], act[i]) if i < len(act) else ("-", "-")
                ok = e == a and e[0] != "-"
                total += 1
                matched += ok
                key = key_of(meta["layout"], (exp[i] if i < len(exp) else act[i]))
                rows.append(f"| {case} | {dsn.split('.')[2]} | {i + 1} | `{key}` | `{e[0][:16]}` | `{a[0][:16]}` "
                            f"| `{e[1][:12]}` | `{a[1][:12]}` | {'MATCH' if ok else 'MISMATCH'} |")
        verdict = not diffs and matched == total
        failed |= not verdict
        summary.append(f"| {case} | {'PASS' if verdict else 'FAIL'} | {len(diffs)} | {checked['records']} | "
                       f"{checked['fields']} | {matched}/{total} | {checked['sysout']} | {java_rc} |")
        print(f"{CHAIN}/{case} [java]: {'PASS' if verdict else 'FAIL'} "
              f"({len(diffs)} field differences, {matched}/{total} row hashes match, RC {java_rc})")

    title = "characterization cases (recorded from CBACT04C under GnuCOBOL)" if args.characterization \
        else "golden interest cases"
    lines = [f"# INTCALC / CBACT04C Java parity: {title}", "",
             "| Case | Result | Field diffs | Records | Fields compared | Row hashes | SYSOUT files | Java RC |",
             "|---|---|---|---|---|---|---|---|", *summary, "",
             "Row hash = SHA-256 of decoded non-filler copybook fields (`NAME=value|...`); "
             "raw = SHA-256 of the record bytes space-filled to LRECL. Expected = legacy COBOL golden output.", "",
             "| Case | Dataset | Rec | Key | Expected row hash | Java row hash | Expected raw | Java raw | Row |",
             "|---|---|---|---|---|---|---|---|---|", *rows, ""]
    args.report.write_text("\n".join(lines))
    print(f"report: {args.report.relative_to(ROOT)} (field detail: {detail.relative_to(ROOT)})")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
