#!/usr/bin/env python3
"""Legacy lane of the replay lab. Runs inside the `estate` container.

    legacy_lane.py <cases root> <case> <out dir> [<runs>]

Runs the interest chain (INTCALC -> CBACT04C, GnuCOBOL) on <cases root>/interest/<case>/input with
the recorder of tools/parity and writes its outputs to <out dir>. The cases root is `fixtures` for a
golden case and `work/lab/cases` for an edge probe or a custom input; nothing is written under the
cases root.

With <runs> 2 the job INTCALC is submitted a second time without reloading anything: the second run
reads the account file the first run rewrote and catalogs the next generation of SYSTRAN. The
outputs of each run go to <out dir>/run1 and <out dir>/run2.
"""
import json
import shutil
import sys
from pathlib import Path

repoRoot = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(repoRoot))
from tools.parity import record  # noqa: E402
from tools.runjcl import runjcl  # noqa: E402


def runRepeated(caseName, outDir, runs):
    """The steps of record.run_case, with the chain's job submitted <runs> times."""
    chain = record.CHAINS["interest"]
    caseDir = record.FIXTURES / "interest" / caseName
    if not caseDir.exists():
        raise SystemExit(f"no case {caseDir}")
    for directory in (runjcl.DATASETS, outDir):
        if directory.exists():
            shutil.rmtree(directory)
        directory.mkdir(parents=True)
    runner = runjcl.Runner(clock=chain["clock"], joblog=runjcl.WORK / "joblog" / "interest" / caseName)
    record.materialise_inputs(chain, caseDir, runner.catalog)
    for job in chain["setup"]:
        if runner.run_job(runjcl.find_jcl(job)) > 8:
            raise SystemExit(f"setup job {job} failed")
    worst = 0
    for number in range(1, runs + 1):
        out = outDir / f"run{number}"
        out.mkdir()
        # rc.json keeps the recorder's shape: the steps of the setup jobs stay in the entry (FI-2 finding)
        returnCodes = {}
        for job in chain["jobs"]:
            maxRc = runner.run_job(runjcl.find_jcl(job))
            returnCodes[job] = {"maxrc": maxRc, "steps": dict(runner.rcs)}
            worst = max(worst, maxRc)
        (out / "rc.json").write_text(json.dumps(returnCodes, indent=1, sort_keys=True))
        (out / "sysout").mkdir()
        for ref in chain["sysout"]:
            job, step = ref.split("/")
            source = runner.joblog / job / step.replace(".", "_") / "SYSOUT.txt"
            (out / "sysout" / f"{job}.{step}.txt").write_text(source.read_text() if source.exists() else "")
        for dsn, layout in chain["outputs"].items():
            record.unload(runner, dsn, layout, out)
    return worst


def main():
    casesRoot, caseName, outDir = Path(sys.argv[1]), sys.argv[2], Path(sys.argv[3])
    runs = int(sys.argv[4]) if len(sys.argv) > 4 else 1
    record.FIXTURES = casesRoot if casesRoot.is_absolute() else repoRoot / casesRoot
    outDir = outDir if outDir.is_absolute() else repoRoot / outDir
    if runs == 1:
        maxRc = record.run_case("interest", caseName, outDir)
    else:
        maxRc = runRepeated(caseName, outDir, runs)
    print(f"legacy lane: interest/{caseName} runs={runs} MAXRC={maxRc}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
