#!/usr/bin/env python3
"""Prepare Db2 COBOL sources for Open COBOL ESQL without editing app/.

The Db2 precompiler on z/OS tolerates three things ocesql 1.4 does not:

1. sequence numbers in columns 73-80 (ocesql reads the whole line);
2. ``EXEC SQL`` / ``INCLUDE SQLCA`` / ``END-EXEC`` split over three lines
   (ocesql only accepts the one-line form);
3. ``EXEC SQL DECLARE <table> TABLE (...) END-EXEC.`` inside a DCLGEN
   member (ocesql has no grammar for DECLARE TABLE; Db2 treats it as a
   compile-time hint only).

This script writes normalised scratch copies under work/esql/ and the
include directory ocesql expects (members named without extension).
Every change is listed in docs/BASELINE.md.
"""
from __future__ import annotations

import re
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DB2_APP = ROOT / "app" / "app-transaction-type-db2"
OUT = ROOT / "work" / "esql"
OCESQL_COPY = Path("/usr/local/share/open-cobol-esql/copy")

INCLUDE_SQLCA = re.compile(
    r"^(\s*)EXEC SQL\s*\n\s*INCLUDE SQLCA\s*\n\s*END-EXEC\.?\s*$", re.MULTILINE)
DECLARE_TABLE = re.compile(r"^\s*EXEC SQL DECLARE .*? TABLE.*?END-EXEC\.\s*?\n", re.S | re.M)


def strip_sequence(text: str) -> str:
    return "\n".join(line[:72].rstrip() for line in text.splitlines()) + "\n"


def prepare_source(src: Path, dest: Path) -> list[str]:
    changes: list[str] = []
    text = strip_sequence(src.read_text())
    if INCLUDE_SQLCA.search(text):
        text = INCLUDE_SQLCA.sub(r"\1EXEC SQL INCLUDE SQLCA END-EXEC.", text)
        changes.append("joined multi-line INCLUDE SQLCA")
    dest.write_text(text)
    return changes


def prepare_include(src: Path, dest: Path) -> list[str]:
    changes: list[str] = []
    text = strip_sequence(src.read_text())
    if DECLARE_TABLE.search(text):
        text = DECLARE_TABLE.sub("", text)
        changes.append("removed EXEC SQL DECLARE ... TABLE")
    dest.write_text(text)
    return changes


def main() -> int:
    inc = OUT / "inc"
    inc.mkdir(parents=True, exist_ok=True)
    report: list[str] = []
    if OCESQL_COPY.exists():
        for path in OCESQL_COPY.iterdir():
            shutil.copyfile(path, inc / path.name)
    for dcl in sorted((DB2_APP / "dcl").glob("*.dcl")):
        changes = prepare_include(dcl, inc / dcl.stem.upper())
        report.append(f"{dcl.relative_to(ROOT)}: {', '.join(changes) or 'columns 73-80 only'}")
    for cpy in sorted((DB2_APP / "cpy").glob("*.cpy")):
        shutil.copyfile(cpy, inc / cpy.stem.upper())
    # CICS online programs need a CICS translator and are not built locally.
    sources = [p for p in sorted((DB2_APP / "cbl").glob("*.cbl"))
               if "EXEC CICS" not in p.read_text(errors="replace")]
    for src in sources:
        changes = prepare_source(src, OUT / src.name)
        report.append(f"{src.relative_to(ROOT)}: {', '.join(changes) or 'columns 73-80 only'}")
    for line in report:
        print(f"esql_prep: {line}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
