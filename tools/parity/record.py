#!/usr/bin/env python3
"""Run a chain for one fixture case and capture its outputs.

    record.py <chain> <case> --out <dir>

Materialises fixtures/<chain>/<case>/input/*.txt as fixed-length datasets,
runs the chain's setup JCL and job JCL through tools/runjcl, then writes
datasets (one record per line), SYSOUT, return codes and database table
snapshots to <dir>.  ``make record`` points <dir> at the fixture's
expected/ folder (golden files); ``make parity`` points it at work/ and
compares.
"""
from __future__ import annotations

import argparse
import json
import os
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))
from tools.runjcl import runjcl  # noqa: E402

CHAINS = json.loads((ROOT / "tools" / "runjcl" / "chains.json").read_text())
FIXTURES = ROOT / "fixtures"


def psql(sql: str, capture: bool = False) -> str:
    cmd = ["psql", "-v", "ON_ERROR_STOP=1", "-q", "-A", "-t", "-c", sql]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        raise SystemExit(f"psql failed: {result.stderr}")
    return result.stdout


def psql_file(path: Path) -> None:
    result = subprocess.run(["psql", "-v", "ON_ERROR_STOP=1", "-q", "-f", str(path)], capture_output=True, text=True)
    if result.returncode != 0:
        raise SystemExit(f"psql -f {path} failed: {result.stderr}")


def materialise_inputs(chain: dict, case_dir: Path, catalog: runjcl.Catalog) -> None:
    for dsn, lrecl in chain["inputs"].items():
        src = case_dir / "input" / f"{dsn}.txt"
        lines = []
        if src.exists():
            lines = [line.rstrip("\r\n") for line in src.read_text(encoding="latin-1").splitlines()]
        data = b"".join(line.ljust(lrecl)[:lrecl].encode("latin-1") for line in lines)
        runjcl.dataset_path(dsn).write_bytes(data)
        catalog.set(dsn, type="flat", lrecl=lrecl)
    catalog.save()


def reset_database(chain: dict, case_dir: Path) -> None:
    db = chain.get("database")
    if not db:
        return
    for table in db["tables"]:
        psql(f"TRUNCATE TABLE {table} CASCADE")
    seed = case_dir / "input" / db["seed"]
    if seed.exists():
        psql_file(seed)


def snapshot_database(chain: dict, out: Path) -> None:
    db = chain.get("database")
    if not db:
        return
    (out / "db").mkdir(parents=True, exist_ok=True)
    for table, order in db["tables"].items():
        csv = subprocess.run(
            ["psql", "-v", "ON_ERROR_STOP=1", "-c",
             f"COPY (SELECT * FROM {table} ORDER BY {order}) TO STDOUT WITH CSV HEADER"],
            capture_output=True, text=True)
        if csv.returncode != 0:
            raise SystemExit(f"snapshot of {table} failed: {csv.stderr}")
        (out / "db" / f"{table}.csv").write_text(csv.stdout)


def unload(runner: runjcl.Runner, dsn: str, layout: str, out: Path) -> None:
    catalog = runner.catalog
    if "(" in dsn:
        dsn, _ = runner.resolve_dsn(dsn)
    path = runjcl.dataset_path(dsn)
    entry = catalog.entry(dsn) or {}
    if not path.exists():
        data = b""
        lrecl = entry.get("lrecl") or entry.get("recsize") or 80
    elif entry.get("type") == "ksds":
        flat = runjcl.WORK / "gen" / f"{dsn}.unload"
        module = runjcl.ksds_module(entry, unload=True)
        rc = runner._cobcrun([module], {"DD_KSDSFILE": str(path), "DD_FLATFILE": str(flat)}, quiet=True)
        if rc:
            raise SystemExit(f"unload of {dsn} failed")
        data, lrecl = flat.read_bytes(), entry["recsize"]
    else:
        data, lrecl = path.read_bytes(), entry.get("lrecl") or 80
    records = [data[i:i + lrecl] for i in range(0, len(data), lrecl)]
    (out / "datasets").mkdir(parents=True, exist_ok=True)
    target = out / "datasets" / f"{dsn}.txt"
    target.write_bytes(b"".join(r.rstrip(b" ") + b"\n" for r in records))
    manifest = json.loads((out / "manifest.json").read_text()) if (out / "manifest.json").exists() else {}
    manifest[dsn] = {"layout": layout, "lrecl": lrecl, "records": len(records)}
    (out / "manifest.json").write_text(json.dumps(manifest, indent=1, sort_keys=True))


def run_case(chain_name: str, case: str, out: Path, verbose: bool = False) -> int:
    chain = CHAINS[chain_name]
    case_dir = FIXTURES / chain_name / case
    if not case_dir.exists():
        raise SystemExit(f"no fixture {case_dir}")
    if runjcl.DATASETS.exists():
        shutil.rmtree(runjcl.DATASETS)
    runjcl.DATASETS.mkdir(parents=True)
    if out.exists():
        shutil.rmtree(out)
    out.mkdir(parents=True)
    runner = runjcl.Runner(clock=chain["clock"], joblog=runjcl.WORK / "joblog" / chain_name / case, verbose=verbose)
    materialise_inputs(chain, case_dir, runner.catalog)
    reset_database(chain, case_dir)
    rcs: dict[str, dict] = {}
    for job in chain["setup"]:
        rc = runner.run_job(runjcl.find_jcl(job))
        if rc > 8:
            raise SystemExit(f"setup job {job} failed with RC {rc}")
    for job in chain["jobs"]:
        rc = runner.run_job(runjcl.find_jcl(job))
        rcs[job] = {"maxrc": rc, "steps": {k: v for k, v in runner.rcs.items() if "." not in k or k.startswith(job)}}
        runner.rcs = {}
    (out / "rc.json").write_text(json.dumps(rcs, indent=1, sort_keys=True))
    (out / "sysout").mkdir()
    for ref in chain["sysout"]:
        job, step = ref.split("/")
        src = runner.joblog / job / step.replace(".", "_") / "SYSOUT.txt"
        (out / "sysout" / f"{job}.{step}.txt").write_text(src.read_text() if src.exists() else "")
    for dsn, layout in chain["outputs"].items():
        unload(runner, dsn, layout, out)
    snapshot_database(chain, out)
    return max((r["maxrc"] for r in rcs.values()), default=0)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("chain", choices=sorted(CHAINS))
    parser.add_argument("case")
    parser.add_argument("--out", required=True, type=Path)
    parser.add_argument("-v", "--verbose", action="store_true")
    args = parser.parse_args()
    rc = run_case(args.chain, args.case, args.out, args.verbose)
    print(f"{args.chain}/{args.case}: chain MAXRC={rc}, outputs in {args.out}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
