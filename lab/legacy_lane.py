#!/usr/bin/env python3
"""Legacy lane of the replay lab. Runs inside the `estate` container.

    legacy_lane.py <cases root> <case> <out dir>

Runs the interest chain (INTCALC -> CBACT04C, GnuCOBOL) on <cases root>/interest/<case>/input with
the recorder of tools/parity and writes its outputs to <out dir>. The cases root is `fixtures` for a
golden case and `work/lab/cases` for an edge probe or a custom input; nothing is written under the
cases root.
"""
import sys
from pathlib import Path

repoRoot = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(repoRoot))
from tools.parity import record  # noqa: E402


def main():
    casesRoot, caseName, outDir = Path(sys.argv[1]), sys.argv[2], Path(sys.argv[3])
    record.FIXTURES = casesRoot if casesRoot.is_absolute() else repoRoot / casesRoot
    maxRc = record.run_case("interest", caseName, outDir if outDir.is_absolute() else repoRoot / outDir)
    print(f"legacy lane: interest/{caseName} MAXRC={maxRc}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
