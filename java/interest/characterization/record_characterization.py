#!/usr/bin/env python3
"""Run the legacy INTCALC chain (real CBACT04C under GnuCOBOL) on the characterization cases.

Uses the repository recorder unchanged, pointed at java/interest/characterization/cases
instead of fixtures/, so the golden fixtures are never touched.

    python3 java/interest/characterization/record_characterization.py [case ...]
"""
from __future__ import annotations

import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
sys.path.insert(0, str(ROOT))
from tools.parity import record  # noqa: E402

record.FIXTURES = HERE / "cases"


def main(argv: list[str]) -> int:
    cases = argv or sorted(p.name for p in (record.FIXTURES / "interest").iterdir() if p.is_dir())
    for case in cases:
        out = record.FIXTURES / "interest" / case / "expected"
        rc = record.run_case("interest", case, out)
        print(f"interest/{case}: legacy RC {rc} -> {out.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
