"""python3 -m harness <command> <run dir>   (run from the spec/ directory)"""
import argparse
import json
import shutil
import sys
from pathlib import Path

from . import SPEC_DIR, compare, inputs, site
from .cobol import CobolRun
from .jcl import batch_programs, manifest

GOLDEN = SPEC_DIR / "golden"


def default_jobs():
    return ",".join(e["job"] for e in manifest()["batch_path"] if not e.get("optional"))


def main(argv=None):
    p = argparse.ArgumentParser(prog="harness")
    sub = p.add_subparsers(dest="cmd", required=True)
    sub.add_parser("programs", help="list the batch path programs from the manifest and JCL").add_argument("--all", action="store_true")
    for name in ("inputs", "cobol", "freeze", "golden", "compare", "site", "all"):
        sp = sub.add_parser(name)
        sp.add_argument("run_dir")
        sp.add_argument("--jobs", default=default_jobs())
        sp.add_argument("--left", default="cobol")
        sp.add_argument("--right", default="java")
        sp.add_argument("--label", default="CardDemo batch path")
        sp.add_argument("--expect-fail", action="store_true", help="exit 0 only if the comparison fails (negative control)")
    a = p.parse_args(argv)

    if a.cmd == "programs":
        print(json.dumps(batch_programs(a.all), indent=2))
        return 0
    run = Path(a.run_dir)
    if a.cmd in ("inputs", "all"):
        print(json.dumps({"inputs": inputs.build(run)}))
    if a.cmd in ("cobol", "all"):
        summary = CobolRun(run, run_ts=manifest()["run"]["current_date"]).execute(a.jobs.split(","))
        print(json.dumps({"cobol": summary["counts"], "stages": summary["stages"]}))
    if a.cmd == "freeze":
        GOLDEN.mkdir(exist_ok=True)
        for f in (run / "cobol").glob("*.jsonl"):
            shutil.copy(f, GOLDEN / f.name)
        meta = json.loads((run / "cobol" / "run.json").read_text())
        meta.pop("stages", None)
        (GOLDEN / "run.json").write_text(json.dumps(meta, indent=2) + "\n")
        print(f"froze {run / 'cobol'} into {GOLDEN}")
    if a.cmd == "golden":
        (run / "cobol").mkdir(parents=True, exist_ok=True)
        for f in GOLDEN.glob("*"):
            shutil.copy(f, run / "cobol" / f.name)
        print(f"copied the frozen COBOL outputs into {run / 'cobol'}")
    rc = 0
    if a.cmd == "compare" and not ((run / a.left).exists() and (run / a.right).exists()):
        print(f"compare needs both {run / a.left} and {run / a.right}", file=sys.stderr)
        return 1
    if a.cmd in ("compare", "all") and (run / a.right).exists():
        report = compare.compare(run, a.left, a.right)
        print(json.dumps({d["dataset"]: d["verdict"] for d in report["datasets"]} | {"verdict": report["verdict"]}))
        failed = report["verdict"] != "pass"
        rc = (0 if failed else 1) if a.expect_fail else (1 if failed else 0)
    if a.cmd in ("site", "all", "compare"):
        print(json.dumps({"measures": site.build(run, a.label)}))
    return rc


if __name__ == "__main__":
    sys.exit(main())
