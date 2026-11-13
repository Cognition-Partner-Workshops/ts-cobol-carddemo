#!/usr/bin/env python3
"""Run packaged Java chains against the unchanged estate fixture/JCL contract."""
from __future__ import annotations

import argparse
import json
import re
import shutil
import subprocess
from pathlib import Path

CHAINS = ("posting", "interest", "trantype-update", "trantype-dynamic")


def jcl_parameter(root: Path, relative: str, expression: str) -> str:
    match = re.search(expression, (root / relative).read_text())
    if match is None:
        raise ValueError(f"JCL parameter not found in {relative}: {expression}")
    return match.group(1)


def job_parameters(root: Path, chain: str) -> list[str]:
    config = json.loads((root / "tools/runjcl/chains.json").read_text())
    if chain == "interest":
        return [jcl_parameter(root, "app/jcl/INTCALC.jcl", r"PARM='([^']*)'")]
    if chain == "trantype-update":
        return [config[chain]["clock"]]
    if chain == "trantype-dynamic":
        return [jcl_parameter(root, "app/app-transaction-type-db2/jcl/MNTTRDYN.jcl", r"PARMS\('([^']*)'\)")]
    return [
        config[chain]["clock"],
        jcl_parameter(root, "app/jcl/TRANREPT.jcl", r"PARM-START-DATE,C'([^']*)'"),
        jcl_parameter(root, "app/jcl/TRANREPT.jcl", r"PARM-END-DATE,C'([^']*)'"),
    ]


def run_cases(root: Path, jars: Path, output: Path, chains: list[str], case: str | None,
              compare: bool, report: Path) -> int:
    failed = False
    output = output.resolve()
    report = report.resolve()
    for chain in chains:
        fixture_root = root / "fixtures" / chain
        cases = [case] if case else sorted(path.name for path in fixture_root.iterdir() if path.is_dir())
        for name in cases:
            fixture = fixture_root / name / "input"
            if not fixture.is_dir():
                raise ValueError(f"no fixture input directory: {fixture}")
            actual = output / chain / name / "actual"
            # Only clear runner-owned case output, never fixtures or the checkout.
            if actual == root or root in actual.parents and (root / "work") not in actual.parents:
                raise ValueError(f"output must be outside the checkout or under work/: {actual}")
            if actual.exists():
                shutil.rmtree(actual)
            actual.mkdir(parents=True)
            command = ["java", "-jar", str(jars / f"{chain}.jar"), str(fixture), str(actual),
                       *job_parameters(root, chain)]
            print(f"\n=== {chain}/{name} — Java container ===", flush=True)
            print(" ".join(command), flush=True)
            result = subprocess.run(command, cwd=root, check=False)
            if result.returncode:
                print(f"{chain}/{name}: Java process exited {result.returncode}", flush=True)
                failed = True
            if compare:
                result = subprocess.run([
                    "python3", str(root / "tools/parity/compare.py"), chain, name,
                    "--actual", str(actual), "--report", str(report), "--label", "Java container",
                ], cwd=root, check=False)
                failed = failed or result.returncode != 0
    return int(failed)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--chain", choices=CHAINS)
    parser.add_argument("--case")
    parser.add_argument("--out", type=Path, default=Path("/estate/work/parity-container"))
    parser.add_argument("--compare", action="store_true", help="run the unchanged field-level comparator")
    parser.add_argument("--report", type=Path, default=Path("/estate/work/parity-container/report.md"))
    args = parser.parse_args()
    if args.case and (Path(args.case).name != args.case or args.case in (".", "..")):
        parser.error("--case must be a fixture directory name")
    return run_cases(Path("/estate"), Path("/opt/carddemo"), args.out,
                     [args.chain] if args.chain else list(CHAINS), args.case, args.compare, args.report)


if __name__ == "__main__":
    raise SystemExit(main())
