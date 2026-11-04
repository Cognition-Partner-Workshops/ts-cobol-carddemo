#!/usr/bin/env python3
"""Field-level parity comparison of recorded chain outputs.

    compare.py <chain> <case> --actual <dir> [--report <file>] [--label name]

Compares fixtures/<chain>/<case>/expected (golden) with <dir> (candidate):

* datasets with a copybook layout are decoded record by record with
  tools/parity/copybook.py and compared field by field (COMP-3, zoned
  overpunch and EBCDIC aware), reporting field name, expected and actual;
* text datasets (reports) and SYSOUT are compared line by line;
* rc.json and database CSV snapshots must match exactly.

Writes a Markdown report (default work/parity/report.md, appended) and
exits 1 on any difference.  Never edit fixtures or COBOL to make it pass.
"""
from __future__ import annotations

import argparse
import difflib
import json
import sys
from decimal import Decimal
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))
from tools.parity.copybook import decode_record, parse_copybook  # noqa: E402

FIXTURES = ROOT / "fixtures"
MAX_ROWS = 40


def read_lines(path: Path) -> list[str]:
    return path.read_text(encoding="latin-1").splitlines() if path.exists() else []


def compare_dataset(name: str, layout: str, lrecl: int, expected: Path, actual: Path, encoding: str) -> list[dict]:
    diffs: list[dict] = []
    exp, act = read_lines(expected), read_lines(actual)
    if not actual.exists():
        return [{"where": name, "field": "(file)", "expected": f"{len(exp)} records", "actual": "missing"}]
    if layout == "text":
        for i, (e, a) in enumerate(zip(exp, act)):
            if e != a:
                diffs.append({"where": f"{name} line {i + 1}", "field": "(text)", "expected": e.strip(), "actual": a.strip()})
        if len(exp) != len(act):
            diffs.append({"where": name, "field": "(line count)", "expected": str(len(exp)), "actual": str(len(act))})
        return diffs
    keyfield = parse_copybook(layout)[0].name
    if len(exp) != len(act):
        diffs.append({"where": name, "field": "(record count)", "expected": str(len(exp)), "actual": str(len(act))})
    for i, (e, a) in enumerate(zip(exp, act)):
        erec = decode_record(layout, e.ljust(lrecl).encode("latin-1"), encoding)
        arec = decode_record(layout, a.ljust(lrecl).encode("latin-1"), encoding)
        key = erec.get(keyfield, i + 1)
        for field, ev in erec.items():
            av = arec.get(field)
            if ev != av:
                diffs.append({"where": f"{name} rec {i + 1} ({keyfield}={key})", "field": field,
                              "expected": _fmt(ev), "actual": _fmt(av)})
    return diffs


def _fmt(value) -> str:
    if isinstance(value, Decimal):
        return format(value, "f")
    return repr(value) if value is None else str(value)


def compare_text(name: str, expected: Path, actual: Path) -> list[dict]:
    exp, act = read_lines(expected), read_lines(actual)
    if exp == act:
        return []
    diff = list(difflib.unified_diff(exp, act, "expected", "actual", lineterm="", n=0))
    return [{"where": name, "field": "(text)", "expected": "", "actual": "\n".join(diff[2:2 + MAX_ROWS])}]


def compare_case(chain: str, case: str, actual: Path, encoding: str = "ascii") -> tuple[list[dict], dict]:
    expected = FIXTURES / chain / case / "expected"
    if not expected.exists():
        raise SystemExit(f"no golden files at {expected}; run `make record CHAIN={chain} CASE={case}` first")
    manifest = json.loads((expected / "manifest.json").read_text()) if (expected / "manifest.json").exists() else {}
    diffs: list[dict] = []
    checked = {"datasets": 0, "fields": 0, "records": 0, "sysout": 0, "db": 0}
    for dsn, meta in sorted(manifest.items()):
        checked["datasets"] += 1
        checked["records"] += meta["records"]
        if meta["layout"] != "text":
            checked["fields"] += meta["records"] * len([f for f in parse_copybook(meta["layout"]) if not f.is_filler])
        diffs += compare_dataset(dsn, meta["layout"], meta["lrecl"],
                                 expected / "datasets" / f"{dsn}.txt", actual / "datasets" / f"{dsn}.txt", encoding)
    for path in sorted((expected / "sysout").glob("*.txt")):
        checked["sysout"] += 1
        diffs += compare_text(f"SYSOUT {path.stem}", path, actual / "sysout" / path.name)
    diffs += compare_text("rc.json", expected / "rc.json", actual / "rc.json")
    for path in sorted((expected / "db").glob("*.csv")) if (expected / "db").exists() else []:
        checked["db"] += 1
        diffs += compare_text(f"table {path.stem}", path, actual / "db" / path.name)
    return diffs, checked


def write_report(report: Path, chain: str, case: str, label: str, diffs: list[dict], checked: dict) -> None:
    report.parent.mkdir(parents=True, exist_ok=True)
    verdict = "PASS" if not diffs else f"FAIL ({len(diffs)} difference{'s' if len(diffs) != 1 else ''})"
    lines = [f"## {chain} / {case} — {label}: **{verdict}**", "",
             f"Checked {checked['datasets']} datasets, {checked['records']} records, {checked['fields']} decoded fields, "
             f"{checked['sysout']} SYSOUT files, {checked['db']} table snapshots, return codes.", ""]
    if diffs:
        lines += ["| Where | Field | Expected (legacy) | Actual (candidate) |", "|---|---|---|---|"]
        for d in diffs[:MAX_ROWS]:
            exp = d["expected"].replace("|", "\\|").replace("\n", "<br>")
            act = d["actual"].replace("|", "\\|").replace("\n", "<br>")
            lines.append(f"| {d['where']} | `{d['field']}` | {exp} | {act} |")
        if len(diffs) > MAX_ROWS:
            lines.append(f"| … | | {len(diffs) - MAX_ROWS} more | |")
        fields = sorted({d["field"] for d in diffs})
        lines += ["", f"Fields differing: {', '.join(f'`{f}`' for f in fields)}"]
    lines.append("")
    with report.open("a") as fh:
        fh.write("\n".join(lines) + "\n")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("chain")
    parser.add_argument("case")
    parser.add_argument("--actual", required=True, type=Path)
    parser.add_argument("--report", type=Path, default=ROOT / "work" / "parity" / "report.md")
    parser.add_argument("--label", default="legacy rerun")
    parser.add_argument("--encoding", default="ascii", choices=["ascii", "ebcdic"])
    args = parser.parse_args()
    diffs, checked = compare_case(args.chain, args.case, args.actual, args.encoding)
    write_report(args.report, args.chain, args.case, args.label, diffs, checked)
    status = "PASS" if not diffs else "FAIL"
    print(f"{args.chain}/{args.case} [{args.label}]: {status} "
          f"({checked['records']} records, {checked['fields']} fields, {len(diffs)} differences)")
    return 0 if not diffs else 1


if __name__ == "__main__":
    sys.exit(main())
