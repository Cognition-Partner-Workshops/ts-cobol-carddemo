#!/usr/bin/env python3
"""Minimal local JCL runner for the CardDemo batch estate.

Executes the real members under app/jcl against GnuCOBOL programs in
./loadlib, mapping every DD statement to a GnuCOBOL ``DD_<name>``
environment variable and every DSN to a file under ./datasets.  It
understands the subset of JCL the estate actually uses:

* JOB / EXEC PGM= / EXEC PROC= (with symbolic parameters and DD overrides)
* DD with DSN, DISP, DCB/LRECL, SYSOUT, DUMMY, instream data (* / DATA),
  GDG relative generations ``(+1)`` / ``(0)`` / ``(-1)``
* COND= on EXEC and IF/THEN/ELSE/ENDIF on step return codes
* utilities: IDCAMS (DEFINE CLUSTER/AIX/PATH/GDG, DELETE, REPRO, BLDINDEX),
  SORT/ICEMAN (SORT FIELDS, INCLUDE COND, SYMNAMES), IEFBR14, IEBGENER,
  IKJEFT01 (``DSN ... RUN PROGRAM(x)`` -> cobcrun DSNRUN x)
* PARM= on EXEC PGM (passed through the PARMDRV stand-in)

VSAM KSDS clusters become GnuCOBOL indexed (Berkeley DB) files created by
generated loader programs; a JSON catalog under datasets/ remembers
cluster geometry, alternate indexes, paths and GDG generations.
"""
from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JCL_DIR = ROOT / "app" / "jcl"
PROC_DIRS = [ROOT / "app" / "proc", ROOT / "app" / "app-transaction-type-db2" / "proc"]
CTL_DIRS = [ROOT / "app" / "ctl", ROOT / "app" / "app-transaction-type-db2" / "ctl"]
LOADLIB = ROOT / "loadlib"
DATASETS = ROOT / "datasets"
CATALOG_FILE = DATASETS / ".catalog.json"
WORK = ROOT / "work"


class JclError(Exception):
    pass


# --------------------------------------------------------------------------
# catalog
# --------------------------------------------------------------------------
class Catalog:
    def __init__(self) -> None:
        self.data: dict = {"datasets": {}, "paths": {}, "gdg": {}}
        if CATALOG_FILE.exists():
            self.data = json.loads(CATALOG_FILE.read_text())

    def save(self) -> None:
        DATASETS.mkdir(parents=True, exist_ok=True)
        CATALOG_FILE.write_text(json.dumps(self.data, indent=1, sort_keys=True))

    def entry(self, dsn: str) -> dict | None:
        return self.data["datasets"].get(dsn)

    def set(self, dsn: str, **attrs) -> dict:
        entry = self.data["datasets"].setdefault(dsn, {})
        entry.update(attrs)
        return entry

    def remove(self, dsn: str) -> None:
        self.data["datasets"].pop(dsn, None)
        self.data["paths"].pop(dsn, None)
        self.data["gdg"].pop(dsn, None)

    def is_ksds(self, dsn: str) -> bool:
        entry = self.entry(dsn)
        return bool(entry and entry.get("type") == "ksds")

    def lrecl(self, dsn: str) -> int | None:
        entry = self.entry(dsn)
        return entry.get("lrecl") if entry else None

    def resolve_gdg(self, base: str, rel: int, pending: dict[str, int]) -> str:
        gdg = self.data["gdg"].setdefault(base, {"current": 0})
        if rel > 0:
            if base not in pending:
                pending[base] = gdg["current"] + rel
            number = pending[base]
        else:
            number = gdg["current"] + rel
        if number <= 0:
            raise JclError(f"GDG {base}({rel:+d}) has no such generation")
        return f"{base}.G{number:04d}V00"

    def commit_gdg(self, pending: dict[str, int]) -> None:
        for base, number in pending.items():
            self.data["gdg"][base]["current"] = max(self.data["gdg"][base]["current"], number)
        pending.clear()


def dataset_path(dsn: str) -> Path:
    return DATASETS / dsn


# --------------------------------------------------------------------------
# JCL parsing
# --------------------------------------------------------------------------
@dataclass
class DD:
    name: str
    params: dict
    instream: list[str] | None = None
    concat: list[dict] = field(default_factory=list)


@dataclass
class Step:
    name: str
    program: str | None
    parm: str | None
    cond: str | None
    dds: dict[str, DD] = field(default_factory=dict)
    proc_step: str | None = None   # name of the proc step this came from
    guard: list[str] = field(default_factory=list)  # enclosing IF conditions


def split_params(text: str) -> list[str]:
    parts, depth, quote, current = [], 0, False, ""
    for ch in text:
        if ch == "'" and not quote:
            quote = True
        elif ch == "'" and quote:
            quote = False
        if not quote:
            if ch == "(":
                depth += 1
            elif ch == ")":
                depth -= 1
        if ch == "," and depth == 0 and not quote:
            parts.append(current)
            current = ""
        else:
            current += ch
    if current:
        parts.append(current)
    return parts


def parse_operands(text: str) -> dict:
    params: dict = {"_positional": []}
    for part in split_params(text):
        if "=" in part and not part.startswith("'"):
            key, _, value = part.partition("=")
            params[key.upper()] = value.strip("'") if not value.startswith("(") else value
        elif part:
            params["_positional"].append(part)
    return params


def substitute(text: str, symbols: dict[str, str]) -> str:
    def repl(match: re.Match) -> str:
        name = match.group(1)
        if name in symbols:
            return symbols[name]
        return match.group(0)
    return re.sub(r"&([A-Z0-9#@$]+)\.?", repl, text)


def read_statements(lines: list[str]) -> list[tuple[str, str, str, list[str] | None]]:
    """Return (name, operation, operands, instream) tuples."""
    statements = []
    i = 0
    while i < len(lines):
        raw = lines[i].rstrip("\n")[:72].rstrip()
        i += 1
        if raw.startswith("//*") or not raw.startswith("//"):
            continue
        if raw.strip() == "//":
            continue
        body = raw[2:]
        match = re.match(r"^(\S*)\s+(\S+)(?:\s+(.*))?$", body)
        if not match:
            # continuation of operands already handled; stray line
            continue
        name, op, operands = match.group(1), match.group(2).upper(), match.group(3) or ""
        if op in ("IF", "ELSE", "ENDIF") or (name and op in ("IF",)):
            pass
        # join continuation lines
        while operands.rstrip().endswith(",") and i < len(lines):
            nxt = lines[i].rstrip("\n")[:72]
            if nxt.startswith("//*"):
                i += 1
                continue
            if not nxt.startswith("//"):
                break
            i += 1
            operands = operands.rstrip() + nxt[2:].strip()
        # IF statements may continue without trailing comma until THEN
        if op == "IF":
            while "THEN" not in operands.upper() and i < len(lines) and lines[i].startswith("//"):
                operands += " " + lines[i][2:].strip()
                i += 1
        instream = None
        if op == "DD":
            first = split_params(operands)[0].strip() if operands.strip() else ""
            if first in ("*", "DATA") or first.startswith("DATA,") or first.startswith("*,"):
                instream = []
                dlm = "/*"
                m = re.search(r"DLM=(\S\S)", operands)
                if m:
                    dlm = m.group(1)
                while i < len(lines):
                    data = lines[i].rstrip("\n")
                    if data.startswith(dlm) or (first.startswith("*") and data.startswith("//")):
                        if data.startswith(dlm):
                            i += 1
                        break
                    instream.append(data[:80].ljust(80))
                    i += 1
        statements.append((name, op, operands, instream))
    return statements


def load_proc(name: str) -> list[str]:
    for directory in PROC_DIRS:
        for ext in (".prc", ".proc", ".jcl"):
            path = directory / f"{name}{ext}"
            if path.exists():
                return path.read_text(errors="replace").splitlines()
    raise JclError(f"PROC {name} not found")


def _cond_expr_rc(expr: str, rcs: dict[str, int]) -> bool:
    """Evaluate a JCL IF relational expression on step return codes."""
    expr = expr.strip()
    expr = re.sub(r"^\((.*)\)$", r"\1", expr.strip())
    for joiner, fn in ((" OR ", any), (" AND ", all)):
        if joiner in expr.upper():
            parts = re.split(joiner, expr, flags=re.IGNORECASE)
            return fn(_cond_expr_rc(p, rcs) for p in parts)
    m = re.match(r"^\(?\s*(?:([A-Z0-9#@$]+)\.)?(RC|ABEND|RUN)\s*(=|EQ|NE|¬=|GT|GE|LT|LE|>|<|>=|<=)\s*(\d+)?\s*\)?$",
                 expr, re.IGNORECASE)
    if not m:
        raise JclError(f"unsupported IF expression: {expr}")
    stepname, kind, op, value = m.groups()
    kind = kind.upper()
    if kind == "ABEND":
        return False
    if kind == "RUN":
        return stepname is None or stepname in rcs
    rc = rcs.get(stepname, 0) if stepname else max(rcs.values(), default=0)
    value = int(value)
    ops = {"=": rc == value, "EQ": rc == value, "NE": rc != value, "¬=": rc != value,
           "GT": rc > value, ">": rc > value, "GE": rc >= value, ">=": rc >= value,
           "LT": rc < value, "<": rc < value, "LE": rc <= value, "<=": rc <= value}
    return ops[op.upper()]


def cond_skip(cond: str | None, rcs: dict[str, int]) -> bool:
    """EXEC COND=(code,op[,step]) -> True when the step must be bypassed."""
    if not cond:
        return False
    text = cond.strip()
    if text.upper() in ("EVEN", "ONLY"):
        return False
    text = text[1:-1] if text.startswith("(") else text
    tests = split_params(text) if text.startswith("(") else [text]
    tests = [t.strip().strip("()") for t in tests]
    for test in tests:
        bits = [b.strip() for b in test.split(",")]
        if len(bits) < 2 or bits[0].upper() in ("EVEN", "ONLY"):
            continue
        code, op = int(bits[0]), bits[1].upper()
        candidates = [rcs[bits[2]]] if len(bits) > 2 and bits[2] in rcs else list(rcs.values())
        for rc in candidates:
            if {"GT": code > rc, "GE": code >= rc, "EQ": code == rc, "NE": code != rc,
                    "LT": code < rc, "LE": code <= rc}[op]:
                return True
    return False


class Job:
    def __init__(self, path: Path, symbols: dict[str, str] | None = None) -> None:
        self.path = path
        self.name = path.stem
        self.symbols = dict(symbols or {})
        self.steps: list[Step] = []
        self._parse(read_statements(path.read_text(errors="replace").splitlines()))

    def _parse(self, statements) -> None:
        current: Step | None = None
        guards: list[str] = []
        pending_overrides: dict[str, DD] = {}
        for name, op, operands, instream in statements:
            operands = substitute(operands, self.symbols)
            if op == "JOB":
                self.name = name or self.name
            elif op == "SET":
                for part in split_params(operands):
                    key, _, value = part.partition("=")
                    self.symbols[key.strip()] = value.strip("'")
            elif op == "IF":
                guards.append(operands.upper().split("THEN")[0].strip())
            elif op == "ELSE":
                if guards:
                    guards[-1] = f"NOT {guards[-1]}"
            elif op == "ENDIF":
                if guards:
                    guards.pop()
            elif op == "EXEC":
                params = parse_operands(operands)
                proc = params.get("PROC") or (params["_positional"][0] if params["_positional"] else None)
                if "PGM" in params:
                    current = Step(name, params["PGM"].upper(), params.get("PARM"), params.get("COND"),
                                   guard=list(guards))
                    self.steps.append(current)
                else:
                    current = None
                    self._expand_proc(name, proc, params, guards)
            elif op == "DD":
                params = parse_operands(operands)
                dd = DD(name.upper(), params, instream)
                if not name:
                    # concatenation: attach to previous DD
                    if self.steps and self.steps[-1].dds:
                        last = list(self.steps[-1].dds.values())[-1]
                        last.concat.append(params)
                    continue
                if current is not None:
                    current.dds[dd.name] = dd
                else:
                    # DD override for the proc just expanded
                    self._apply_override(dd)
            # JCLLIB, OUTPUT, PEND, INCLUDE etc. are ignored

    def _expand_proc(self, callname: str, proc: str, params: dict, guards: list[str]) -> None:
        lines = load_proc(proc)
        statements = read_statements(lines)
        symbols = dict(self.symbols)
        for key, value in params.items():
            if key not in ("_positional", "PROC", "COND", "PARM"):
                symbols[key] = value.strip("'")
        self._last_proc_steps = []
        current: Step | None = None
        for name, op, operands, instream in statements:
            if op == "PROC":
                for part in split_params(operands):
                    key, _, value = part.partition("=")
                    symbols.setdefault(key.strip(), value.strip("'"))
                continue
            operands = substitute(operands, symbols)
            if op == "PEND":
                break
            if op == "EXEC":
                p = parse_operands(operands)
                current = Step(f"{callname}.{name}", p["PGM"].upper(), p.get("PARM"), params.get("COND") or p.get("COND"),
                               proc_step=name, guard=list(guards))
                self.steps.append(current)
                self._last_proc_steps.append(current)
            elif op == "DD" and current is not None:
                if name:
                    current.dds[name.upper()] = DD(name.upper(), parse_operands(operands), instream)
                elif current.dds:
                    list(current.dds.values())[-1].concat.append(parse_operands(operands))

    def _apply_override(self, dd: DD) -> None:
        steps = getattr(self, "_last_proc_steps", [])
        if not steps:
            return
        stepname, _, ddname = dd.name.partition(".")
        if ddname:
            targets = [s for s in steps if s.proc_step == stepname]
        else:
            ddname, targets = stepname, steps[:1]
        for step in targets:
            if ddname in step.dds:
                step.dds[ddname].params.update({k: v for k, v in dd.params.items() if k != "_positional"})
                if dd.instream is not None:
                    step.dds[ddname].instream = dd.instream
            else:
                step.dds[ddname] = DD(ddname, dd.params, dd.instream)


# --------------------------------------------------------------------------
# generated loader / unloader programs for KSDS clusters
# --------------------------------------------------------------------------
def _segments(recsize: int, keys: list[tuple[int, int, bool]]) -> str:
    """Build a record description with named key fields for the given keys."""
    bounds = {0, recsize}
    for length, offset, _ in keys:
        bounds.update({offset, offset + length})
    bounds = sorted(b for b in bounds if 0 <= b <= recsize)
    lines = []
    for start, end in zip(bounds, bounds[1:]):
        name = "FILLER"
        for index, (length, offset, _) in enumerate(keys):
            if offset == start and offset + length == end:
                name = f"REC-KEY{index}"
        lines.append(f"           05  {name:<16} PIC X({end - start}).")
    return "\n".join(lines)


def ksds_module(entry: dict, unload: bool = False) -> str:
    """Return (and compile if needed) the loader/unloader module name."""
    keys = [(entry["keys"][0], entry["keys"][1], False)]
    for aix in entry.get("aix", []):
        if aix.get("built"):
            keys.append((aix["keys"][0], aix["keys"][1], not aix.get("unique", False)))
    recsize = entry["recsize"]
    tag = "".join(f"K{l}O{o}{'D' if d else 'U'}" for l, o, d in keys)
    name = f"{'UL' if unload else 'LD'}{recsize}{tag}"
    module = LOADLIB / f"{name}.so"
    if module.exists():
        return name
    alt = "".join(
        f"\n               ALTERNATE RECORD KEY IS REC-KEY{i}{' WITH DUPLICATES' if d else ''}"
        for i, (_, _, d) in enumerate(keys) if i > 0)
    src = f"""       IDENTIFICATION DIVISION.
       PROGRAM-ID. {name}.
       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT FLAT ASSIGN TO FLATFILE
               ORGANIZATION IS SEQUENTIAL
               FILE STATUS IS WS-FLAT-STATUS.
           SELECT KSDS ASSIGN TO KSDSFILE
               ORGANIZATION IS INDEXED
               ACCESS MODE IS SEQUENTIAL
               RECORD KEY IS REC-KEY0{alt}
               FILE STATUS IS WS-KSDS-STATUS.
       DATA DIVISION.
       FILE SECTION.
       FD  FLAT RECORD CONTAINS {recsize} CHARACTERS.
       01  FLAT-REC                PIC X({recsize}).
       FD  KSDS RECORD CONTAINS {recsize} CHARACTERS.
       01  KSDS-REC.
{_segments(recsize, keys)}
       WORKING-STORAGE SECTION.
       01  WS-FLAT-STATUS          PIC XX.
       01  WS-KSDS-STATUS          PIC XX.
       01  WS-COUNT                PIC 9(9) VALUE 0.
       01  WS-SKIPPED              PIC 9(9) VALUE 0.
       01  WS-EOF                  PIC X VALUE 'N'.
       PROCEDURE DIVISION.
"""
    if unload:
        src += """           OPEN INPUT KSDS
           IF WS-KSDS-STATUS NOT = '00'
               DISPLAY 'IDCAMS UNLOAD OPEN KSDS ' WS-KSDS-STATUS
               MOVE 12 TO RETURN-CODE
               STOP RUN
           END-IF
           OPEN OUTPUT FLAT
           PERFORM UNTIL WS-EOF = 'Y'
               READ KSDS NEXT RECORD
                   AT END MOVE 'Y' TO WS-EOF
                   NOT AT END
                       MOVE KSDS-REC TO FLAT-REC
                       WRITE FLAT-REC
                       ADD 1 TO WS-COUNT
               END-READ
           END-PERFORM
           CLOSE KSDS FLAT
           DISPLAY 'IDC0005I NUMBER OF RECORDS PROCESSED WAS ' WS-COUNT
           STOP RUN.
"""
    else:
        src += """           OPEN OUTPUT KSDS
           IF WS-KSDS-STATUS NOT = '00'
               DISPLAY 'IDCAMS LOAD OPEN KSDS ' WS-KSDS-STATUS
               MOVE 12 TO RETURN-CODE
               STOP RUN
           END-IF
           OPEN INPUT FLAT
           IF WS-FLAT-STATUS NOT = '00'
               DISPLAY 'IDCAMS LOAD OPEN FLAT ' WS-FLAT-STATUS
               MOVE 12 TO RETURN-CODE
               STOP RUN
           END-IF
           PERFORM UNTIL WS-EOF = 'Y'
               READ FLAT
                   AT END MOVE 'Y' TO WS-EOF
                   NOT AT END
                       MOVE FLAT-REC TO KSDS-REC
                       WRITE KSDS-REC
                           INVALID KEY ADD 1 TO WS-SKIPPED
                           NOT INVALID KEY ADD 1 TO WS-COUNT
                       END-WRITE
               END-READ
           END-PERFORM
           CLOSE KSDS FLAT
           DISPLAY 'IDC0005I NUMBER OF RECORDS PROCESSED WAS ' WS-COUNT
           IF WS-SKIPPED > 0
               DISPLAY 'IDC3302I DUPLICATE KEYS SKIPPED ' WS-SKIPPED
               MOVE 4 TO RETURN-CODE
           END-IF
           STOP RUN.
"""
    LOADLIB.mkdir(exist_ok=True)
    scratch = WORK / "gen"
    scratch.mkdir(parents=True, exist_ok=True)
    source = scratch / f"{name}.cbl"
    source.write_text(src)
    result = subprocess.run(["cobc", "-m", "-std=ibm", "-o", str(module), str(source)],
                            capture_output=True, text=True)
    if result.returncode != 0:
        raise JclError(f"cannot compile {name}: {result.stderr}")
    return name


# --------------------------------------------------------------------------
# runner
# --------------------------------------------------------------------------
class Runner:
    def __init__(self, clock: str | None = None, joblog: Path | None = None, verbose: bool = False) -> None:
        self.catalog = Catalog()
        self.clock = clock
        self.joblog = joblog or WORK / "joblog"
        self.verbose = verbose
        self.pending_gdg: dict[str, int] = {}
        self.rcs: dict[str, int] = {}
        self.outputs: list[dict] = []      # datasets written by this run
        DATASETS.mkdir(parents=True, exist_ok=True)
        self.joblog.mkdir(parents=True, exist_ok=True)

    # -- environment -------------------------------------------------------
    def base_env(self) -> dict[str, str]:
        env = dict(os.environ)
        env["COB_LIBRARY_PATH"] = f"{LOADLIB}:/usr/local/lib"
        if Path("/usr/local/lib/libocesql.so").exists():
            # ocesql-generated code CALLs the runtime (OCESQLConnect...) dynamically
            env["COB_PRE_LOAD"] = "libocesql"
        env.setdefault("OCDB_NAME", os.environ.get("PGDATABASE", "carddemo"))
        env.setdefault("OCDB_USER", os.environ.get("PGUSER", "carddemo"))
        env.setdefault("OCDB_PASS", os.environ.get("PGPASSWORD", "carddemo"))
        if self.clock:
            env["COB_CURRENT_DATE"] = self.clock
        return env

    def log(self, msg: str) -> None:
        self.joblines.append(msg)
        if self.verbose:
            print(msg)

    # -- DSN resolution ----------------------------------------------------
    def resolve_dsn(self, dsn: str) -> tuple[str, Path | None]:
        """Return (catalogued dsn, path).  Members of control libraries map
        to app/ctl/<member>.ctl."""
        m = re.match(r"^([A-Z0-9.#@$]+)\(([^)]*)\)$", dsn)
        if m:
            base, member = m.groups()
            if re.match(r"^[+-]?\d+$", member):
                gen = self.catalog.resolve_gdg(base, int(member), self.pending_gdg)
                return gen, dataset_path(gen)
            for directory in CTL_DIRS:
                for ext in (".ctl", ".jcl", ".txt", ""):
                    path = directory / f"{member}{ext}"
                    if path.exists():
                        return dsn, path
            raise JclError(f"library member not found: {dsn}")
        return dsn, dataset_path(dsn)

    def allocate(self, step: Step, dd: DD, stepdir: Path) -> tuple[str | None, dict]:
        """Return (path for DD_<name>, info)."""
        params = dd.params
        info = {"dd": dd.name}
        if dd.instream is not None:
            path = stepdir / f"{dd.name}.instream"
            path.write_text("".join(line.rstrip() + "\n" for line in dd.instream))
            info["kind"] = "instream"
            return str(path), info
        pos = params.get("_positional", [])
        if dd.name in ("STEPLIB", "JOBLIB", "DBRMLIB", "SYSUDUMP", "SYSABEND", "CEEDUMP", "ISFOUT", "ISFIN"):
            info["kind"] = "ignored"
            return None, info
        if "DUMMY" in pos or params.get("DSN", "").upper() in ("NULLFILE",):
            info["kind"] = "dummy"
            return "/dev/null", info
        if "SYSOUT" in params:
            path = stepdir / f"{dd.name}.sysout"
            info["kind"] = "sysout"
            info["path"] = str(path)
            return str(path), info
        dsn = params.get("DSN") or params.get("DSNAME")
        if not dsn:
            info["kind"] = "temp"
            return str(stepdir / f"{dd.name}.tmp"), info
        dsn, path = self.resolve_dsn(dsn)
        entry = self.catalog.entry(dsn)
        if entry and entry.get("type") in ("path", "aix"):
            # alternate-index PATH: GnuCOBOL reads the alternate key through
            # the base cluster, so point the DD at the cluster file
            aix_name = entry.get("aix") or dsn
            aix_entry = self.catalog.entry(aix_name) or {}
            base = aix_entry.get("relate")
            info.update(kind="path", dsn=dsn, path=str(dataset_path(base)) if base else None)
            return str(dataset_path(base)) if base else "/dev/null", info
        disp = params.get("DISP", "(NEW,DELETE,DELETE)").strip("()").split(",")
        status = (disp[0] or "NEW").upper()
        info.update(kind="dataset", dsn=dsn, path=str(path), disp=status)
        lrecl = None
        dcb = params.get("DCB", "")
        for source in (dcb.strip("()"), params.get("LRECL", "")):
            m = re.search(r"LRECL=(\d+)", source) if "LRECL=" in source else (re.match(r"^(\d+)$", source))
            if m:
                lrecl = int(m.group(1))
        if status == "NEW" and path is not None and path.parent == DATASETS:
            if path.exists():
                path.unlink()
            for extra in DATASETS.glob(f"{dsn}.[0-9]*"):
                extra.unlink()
            entry = self.catalog.set(dsn, type="flat")
            if lrecl:
                entry["lrecl"] = lrecl
            self.outputs.append({"dsn": dsn, "step": step.name, "dd": dd.name, "lrecl": lrecl})
        elif status == "MOD" and lrecl:
            self.catalog.set(dsn, type="flat", lrecl=lrecl)
        elif status in ("OLD", "SHR") and path is not None and not path.exists() and path.parent == DATASETS:
            if self.catalog.is_ksds(dsn):
                self.create_empty_ksds(dsn)
            else:
                raise JclError(f"{step.name} {dd.name}: dataset {dsn} not found")
        return str(path), info

    def create_empty_ksds(self, dsn: str) -> None:
        entry = self.catalog.entry(dsn)
        module = ksds_module(entry)
        empty = WORK / "gen" / "empty.flat"
        empty.parent.mkdir(parents=True, exist_ok=True)
        empty.write_bytes(b"")
        self._cobcrun([module], {"DD_FLATFILE": str(empty), "DD_KSDSFILE": str(dataset_path(dsn))}, quiet=True)

    # -- step execution ----------------------------------------------------
    def run_job(self, jcl: Path, symbols: dict[str, str] | None = None) -> int:
        self.joblines: list[str] = []
        job = Job(jcl, symbols)
        self.log(f"JOB {job.name} ({jcl.relative_to(ROOT)})")
        maxrc = 0
        jobdir = self.joblog / job.name
        if jobdir.exists():
            shutil.rmtree(jobdir)
        jobdir.mkdir(parents=True)
        for step in job.steps:
            if any(not _cond_expr_rc(g[4:], self.rcs) if g.startswith("NOT ") else not _cond_expr_rc(g, self.rcs)
                   for g in step.guard):
                self.log(f"  {step.name:<16} {step.program:<8} bypassed (IF)")
                continue
            if cond_skip(step.cond, self.rcs):
                self.log(f"  {step.name:<16} {step.program:<8} bypassed (COND)")
                continue
            stepdir = jobdir / step.name.replace(".", "_")
            stepdir.mkdir(parents=True, exist_ok=True)
            rc = self.run_step(step, stepdir)
            self.rcs[step.name.split(".")[-1]] = rc
            self.rcs[step.name] = rc
            maxrc = max(maxrc, rc)
            self.log(f"  {step.name:<16} {step.program:<8} RC={rc:04d}")
            self.catalog.save()
        self.catalog.commit_gdg(self.pending_gdg)
        self.catalog.save()
        (jobdir / "joblog.txt").write_text("\n".join(self.joblines) + "\n")
        (jobdir / "rc.json").write_text(json.dumps({"job": job.name, "maxrc": maxrc, "steps": {
            s.name: self.rcs.get(s.name) for s in job.steps}}, indent=1))
        return maxrc

    def run_step(self, step: Step, stepdir: Path) -> int:
        env = self.base_env()
        infos: dict[str, dict] = {}
        for dd in step.dds.values():
            path, info = self.allocate(step, dd, stepdir)
            infos[dd.name] = info
            if path:
                env[f"DD_{dd.name}"] = path
        program = step.program
        if program == "IDCAMS":
            rc = self.idcams(step, env, infos, stepdir)
        elif program in ("SORT", "ICEMAN", "SYNCSORT", "DFSORT"):
            rc = self.sort(step, env, infos, stepdir)
        elif program == "IEFBR14":
            rc = 0
        elif program == "IEBGENER":
            shutil.copyfile(env["DD_SYSUT1"], env["DD_SYSUT2"])
            rc = 0
        elif program == "IKJEFT01":
            rc = self.ikjeft01(step, env, infos, stepdir)
        else:
            argv = ["PARMDRV", program, step.parm.strip("'")] if step.parm else [program]
            rc = self._cobcrun(argv, env, stepdir=stepdir)
        return rc

    def _cobcrun(self, argv: list[str], extra_env: dict[str, str], stepdir: Path | None = None,
                 quiet: bool = False) -> int:
        env = self.base_env()
        env.update(extra_env)
        result = subprocess.run(["cobcrun", *argv], cwd=ROOT, env=env, capture_output=True, text=True)
        if stepdir is not None:
            stepdir.mkdir(parents=True, exist_ok=True)
            (stepdir / "SYSOUT.txt").write_text(result.stdout + (("\n" + result.stderr) if result.stderr else ""))
        if result.stderr and not quiet:
            self.log("    " + result.stderr.strip().replace("\n", "\n    "))
        return result.returncode

    # -- IKJEFT01: DSN RUN PROGRAM ---------------------------------------
    def ikjeft01(self, step: Step, env: dict, infos: dict, stepdir: Path) -> int:
        text = Path(env["DD_SYSTSIN"]).read_text()
        rc = 0
        for m in re.finditer(r"RUN\s+PROGRAM\(([A-Z0-9#@$]+)\)(?:.*?PARMS?\('([^']*)'\))?", text, re.I | re.S):
            program, parm = m.group(1).upper(), m.group(2)
            self.log(f"    DSN RUN PROGRAM({program})")
            argv = ["DSNRUN", program] + ([parm] if parm else [])
            rc = max(rc, self._cobcrun(argv, env, stepdir=stepdir))
        if "DD_SYSTSPRT" in env and env["DD_SYSTSPRT"] != "/dev/null":
            shutil.copyfile(stepdir / "SYSOUT.txt", env["DD_SYSTSPRT"])
        return rc

    # -- IDCAMS ------------------------------------------------------------
    def idcams(self, step: Step, env: dict, infos: dict, stepdir: Path) -> int:
        text = Path(env["DD_SYSIN"]).read_text()
        text = re.sub(r"/\*.*?\*/", " ", text, flags=re.S)
        commands, current = [], ""
        for raw in text.splitlines():
            line = raw[:72].rstrip()
            if not line.strip():
                continue
            if line.rstrip().endswith(("-", "+")):
                current += " " + line.rstrip()[:-1]
                continue
            current += " " + line
            commands.append(current.strip())
            current = ""
        if current.strip():
            commands.append(current.strip())
        maxcc = 0
        lastcc = 0
        out: list[str] = []
        for command in commands:
            out.append(command)
            verb = command.split()[0].upper()
            try:
                if verb in ("IF", "SET"):
                    m = re.match(r"^(?:IF\s+(LASTCC|MAXCC)\s*(=|EQ|NE|GT|GE|LT|LE|¬=)\s*(\d+)\s+THEN\s+)?SET\s+(MAXCC|LASTCC)\s*=\s*(\d+)",
                                 command, re.IGNORECASE)
                    if m:
                        which, op, value, target, newval = m.groups()
                        current = {"LASTCC": lastcc, "MAXCC": maxcc}.get((which or "").upper(), 0)
                        ok = True
                        if which:
                            value = int(value)
                            ok = {"=": current == value, "EQ": current == value, "NE": current != value,
                                  "¬=": current != value, "GT": current > value, "GE": current >= value,
                                  "LT": current < value, "LE": current <= value}[op.upper()]
                        if ok and target.upper() == "MAXCC":
                            maxcc = int(newval)
                        elif ok:
                            lastcc = int(newval)
                    continue
                elif verb in ("THEN", "ELSE", "LISTCAT", "LISTC", "PRINT", "VERIFY", "DO", "END"):
                    cc = 0
                elif verb in ("DELETE", "DEL"):
                    cc = self._idc_delete(command)
                elif verb in ("DEFINE", "DEF"):
                    cc = self._idc_define(command)
                elif verb == "REPRO":
                    cc = self._idc_repro(command, env, stepdir, out)
                elif verb == "BLDINDEX":
                    cc = self._idc_bldindex(command, env, stepdir, out)
                else:
                    out.append(f"IDC3203I UNSUPPORTED COMMAND {verb} IGNORED")
                    cc = 0
            except JclError as exc:
                out.append(f"IDC3012I {exc}")
                cc = 12
            out.append(f"IDC0001I FUNCTION COMPLETED, HIGHEST CONDITION CODE WAS {cc}")
            lastcc = cc
            maxcc = max(maxcc, cc)
        out.append(f"IDC0002I IDCAMS PROCESSING COMPLETE. MAXIMUM CONDITION CODE WAS {maxcc}")
        sysprint = env.get("DD_SYSPRINT")
        (stepdir / "SYSOUT.txt").write_text("\n".join(out) + "\n")
        if sysprint and sysprint != "/dev/null":
            Path(sysprint).write_text("\n".join(out) + "\n")
        return maxcc

    @staticmethod
    def _idc_params(command: str) -> dict[str, str]:
        """Parse  VERB [pos] KEY(value) KEY(value...)  into a dict (outer level)."""
        params: dict[str, str] = {"_verb": command.split()[0].upper(), "_pos": ""}
        rest = command[len(command.split()[0]):]
        i, depth, key, value = 0, 0, "", ""
        tokens: list[tuple[str, str]] = []
        while i < len(rest):
            ch = rest[i]
            if depth == 0:
                if ch == "(":
                    depth = 1
                    value = ""
                    if not key and tokens and tokens[-1][1] is None:
                        key = tokens.pop()[0]
                elif ch.isspace():
                    if key:
                        tokens.append((key.upper(), None))
                        key = ""
                else:
                    key += ch
            else:
                if ch == "(":
                    depth += 1
                    value += ch
                elif ch == ")":
                    depth -= 1
                    if depth == 0:
                        tokens.append((key.upper(), value))
                        key = ""
                    else:
                        value += ch
                else:
                    value += ch
            i += 1
        if key:
            tokens.append((key.upper(), None))
        for k, v in tokens:
            if v is None:
                params[k] = ""
            elif k in ("CLUSTER", "ALTERNATEINDEX", "AIX", "PATH", "GENERATIONDATAGROUP", "GDG", "DATA", "INDEX", "NONVSAM"):
                params[k] = v
            else:
                params[k] = v.strip()
        return params

    def _idc_delete(self, command: str) -> int:
        params = self._idc_params(command)
        rest = command.split(None, 1)[1]
        name = re.match(r"^\(?\s*([A-Z0-9.#@$]+)", rest.strip()).group(1)
        removed = False
        for path in list(DATASETS.glob(f"{name}*")):
            if path.name == name or re.match(re.escape(name) + r"\.(\d+|G\d{4}V00(\.\d+)?)$", path.name):
                path.unlink()
                removed = True
        for dsn in list(self.catalog.data["datasets"]):
            if dsn == name or dsn.startswith(name + ".G"):
                self.catalog.remove(dsn)
                removed = True
        if name in self.catalog.data["gdg"] or "GDG" in params or "GENERATIONDATAGROUP" in params:
            self.catalog.data["gdg"].pop(name, None)
            removed = True
        self.catalog.data["paths"].pop(name, None)
        for entry in self.catalog.data["datasets"].values():
            if isinstance(entry.get("aix"), list):
                entry["aix"] = [a for a in entry["aix"] if a["name"] != name]
        return 0 if removed else 8

    def _idc_define(self, command: str) -> int:
        params = self._idc_params(command)
        kind = next(k for k in ("CLUSTER", "ALTERNATEINDEX", "AIX", "PATH", "GENERATIONDATAGROUP", "GDG", "NONVSAM")
                    if k in params)
        inner = self._idc_params("X " + params[kind])
        name = inner.get("NAME", "").strip()
        if kind == "CLUSTER":
            keys = [int(x) for x in re.split(r"[\s,]+", inner.get("KEYS", "").strip())] if "KEYS" in inner else None
            rec = [int(x) for x in re.split(r"[\s,]+", inner.get("RECORDSIZE", "").strip())] if "RECORDSIZE" in inner else None
            if "NONINDEXED" in inner or keys is None:
                self.catalog.set(name, type="flat", lrecl=rec[1] if rec else None)
            else:
                self.catalog.set(name, type="ksds", keys=keys, recsize=rec[1] if rec else None, aix=[])
                path = dataset_path(name)
                if path.exists():
                    path.unlink()
        elif kind in ("ALTERNATEINDEX", "AIX"):
            relate = inner["RELATE"].strip()
            cluster = self.catalog.entry(relate)
            if not cluster:
                raise JclError(f"RELATE cluster {relate} not catalogued")
            keys = [int(x) for x in re.split(r"[\s,]+", inner["KEYS"].strip())]
            cluster.setdefault("aix", [])
            cluster["aix"] = [a for a in cluster["aix"] if a["name"] != name]
            cluster["aix"].append({"name": name, "keys": keys, "unique": "UNIQUEKEY" in inner and "NONUNIQUEKEY" not in inner,
                                   "built": False})
            self.catalog.set(name, type="aix", relate=relate)
        elif kind == "PATH":
            self.catalog.data["paths"][name] = {"aix": inner["PATHENTRY"].strip()}
            self.catalog.set(name, type="path", aix=inner["PATHENTRY"].strip())
        elif kind in ("GENERATIONDATAGROUP", "GDG"):
            self.catalog.data["gdg"].setdefault(name, {"current": 0})
            self.catalog.data["gdg"][name]["limit"] = int(inner.get("LIMIT", "99") or 99)
        elif kind == "NONVSAM":
            self.catalog.set(name, type="flat")
        return 0

    def _idc_resolve(self, params: dict, infile: str, indataset: str, env: dict) -> tuple[str, Path]:
        if infile in params:
            ddname = params[infile].strip().upper()
            dd_path = env.get(f"DD_{ddname}")
            if not dd_path:
                raise JclError(f"DD {ddname} not allocated")
            dsn = next((i["dsn"] for i in self._infos.values() if i["dd"] == ddname and i.get("dsn")), ddname)
            return dsn, Path(dd_path)
        dsn, path = self.resolve_dsn(params[indataset].strip())
        return dsn, path

    def _idc_repro(self, command: str, env: dict, stepdir: Path, out: list[str]) -> int:
        params = self._idc_params(command)
        self._infos = getattr(self, "_infos", {})
        src_dsn, src = self._idc_resolve(params, "INFILE", "INDATASET", env)
        dst_dsn, dst = self._idc_resolve(params, "OUTFILE", "OUTDATASET", env)
        src_ksds, dst_ksds = self.catalog.is_ksds(src_dsn), self.catalog.is_ksds(dst_dsn)
        if not src.exists():
            if src_ksds:
                self.create_empty_ksds(src_dsn)
            else:
                raise JclError(f"REPRO input {src_dsn} not found")
        if src_ksds:
            flat = stepdir / f"{src_dsn}.unload"
            cc = self._cobcrun([ksds_module(self.catalog.entry(src_dsn), unload=True)],
                               {"DD_KSDSFILE": str(src), "DD_FLATFILE": str(flat)}, quiet=True)
            if cc:
                raise JclError(f"REPRO unload of {src_dsn} failed")
            source_flat = flat
            lrecl = self.catalog.entry(src_dsn)["recsize"]
        else:
            source_flat = src
            lrecl = self.catalog.lrecl(src_dsn)
        if dst_ksds:
            entry = self.catalog.entry(dst_dsn)
            if dst.exists():
                dst.unlink()
            cc = self._cobcrun([ksds_module(entry)], {"DD_FLATFILE": str(source_flat), "DD_KSDSFILE": str(dst)},
                               stepdir=stepdir / "repro", quiet=True)
            out.extend((stepdir / "repro" / "SYSOUT.txt").read_text().splitlines())
            self.outputs.append({"dsn": dst_dsn, "step": "IDCAMS", "dd": "REPRO", "lrecl": entry["recsize"]})
            return 0 if cc in (0, 4) else cc
        shutil.copyfile(source_flat, dst)
        if lrecl:
            self.catalog.set(dst_dsn, type="flat", lrecl=lrecl)
            size = dst.stat().st_size
            out.append(f"IDC0005I NUMBER OF RECORDS PROCESSED WAS {size // lrecl}")
        self.outputs.append({"dsn": dst_dsn, "step": "IDCAMS", "dd": "REPRO", "lrecl": lrecl})
        return 0

    def _idc_bldindex(self, command: str, env: dict, stepdir: Path, out: list[str]) -> int:
        params = self._idc_params(command)
        self._infos = getattr(self, "_infos", {})
        cluster_dsn, cluster = self._idc_resolve(params, "INFILE", "INDATASET", env)
        aix_dsn, _ = self._idc_resolve(params, "OUTFILE", "OUTDATASET", env)
        entry = self.catalog.entry(cluster_dsn)
        if not entry or entry.get("type") != "ksds":
            raise JclError(f"BLDINDEX base cluster {cluster_dsn} not catalogued")
        aix = next((a for a in entry.get("aix", []) if a["name"] == aix_dsn), None)
        if aix is None:
            raise JclError(f"alternate index {aix_dsn} not defined")
        # rebuild the Berkeley DB file with the alternate key present
        flat = stepdir / f"{cluster_dsn}.unload"
        if cluster.exists():
            cc = self._cobcrun([ksds_module(entry, unload=True)],
                               {"DD_KSDSFILE": str(cluster), "DD_FLATFILE": str(flat)}, quiet=True)
            if cc:
                raise JclError(f"BLDINDEX unload of {cluster_dsn} failed")
            cluster.unlink()
            for extra in DATASETS.glob(f"{cluster_dsn}.[0-9]*"):
                extra.unlink()
        else:
            flat.write_bytes(b"")
        aix["built"] = True
        cc = self._cobcrun([ksds_module(entry)], {"DD_FLATFILE": str(flat), "DD_KSDSFILE": str(cluster)}, quiet=True)
        out.append(f"IDC0652I {aix_dsn} SUCCESSFULLY BUILT")
        return cc if cc not in (0, 4) else 0

    # -- SORT ----------------------------------------------------------------
    def sort(self, step: Step, env: dict, infos: dict, stepdir: Path) -> int:
        control = Path(env["DD_SYSIN"]).read_text()
        symbols: dict[str, tuple] = {}
        if "DD_SYMNAMES" in env:
            for line in Path(env["DD_SYMNAMES"]).read_text().splitlines():
                line = line.split("  ")[0].strip()
                if not line or line.startswith("*"):
                    continue
                name, _, rest = line.partition(",")
                if rest.startswith("C'"):
                    symbols[name] = ("const", rest[2:rest.rindex("'")])
                else:
                    bits = rest.split(",")
                    symbols[name] = ("field", int(bits[0]) - 1, int(bits[1]), bits[2] if len(bits) > 2 else "CH")
        text = " ".join(l[:72].strip() for l in control.splitlines() if l.strip() and not l.startswith("*"))
        src_info = infos.get("SORTIN", {})
        src_dsn = src_info.get("dsn")
        lrecl = self.catalog.lrecl(src_dsn) if src_dsn else None
        if not lrecl:
            lrecl = self.catalog.entry(src_dsn).get("recsize") if src_dsn and self.catalog.entry(src_dsn) else None
        if not lrecl:
            raise JclError(f"SORT: record length of {src_dsn} unknown")
        data = Path(env["DD_SORTIN"]).read_bytes()
        records = [data[i:i + lrecl] for i in range(0, len(data), lrecl)]

        def operand(token: str, rest: list[str]) -> tuple:
            token = token.strip()
            if token in symbols:
                return symbols[token]
            if token.startswith("C'"):
                return ("const", token[2:-1])
            pos = int(token)
            return ("field", pos - 1, int(rest.pop(0)), rest.pop(0))

        def value(rec: bytes, spec: tuple) -> bytes:
            if spec[0] == "const":
                return spec[1].encode("latin-1")
            return rec[spec[1]:spec[1] + spec[2]]

        # INCLUDE / OMIT COND
        keep = lambda rec: True  # noqa: E731
        m = re.search(r"(INCLUDE|OMIT)\s+COND=\((.*?)\)(?:\s|,|$)", text)
        if m:
            kind, cond = m.group(1), m.group(2)
            tokens = [t.strip() for t in split_params(cond)]
            tests, joiners = [], []
            while tokens:
                left = operand(tokens.pop(0), tokens)
                op = tokens.pop(0).upper()
                right = operand(tokens.pop(0), tokens)
                tests.append((left, op, right))
                if tokens:
                    joiners.append(tokens.pop(0).upper())

            def evaluate(rec: bytes) -> bool:
                results = []
                for left, op, right in tests:
                    a, b = value(rec, left), value(rec, right)
                    results.append({"EQ": a == b, "NE": a != b, "GT": a > b, "GE": a >= b,
                                    "LT": a < b, "LE": a <= b}[op])
                result = results[0]
                for joiner, nxt in zip(joiners, results[1:]):
                    result = (result and nxt) if joiner == "AND" else (result or nxt)
                return result
            keep = evaluate if kind == "INCLUDE" else (lambda rec: not evaluate(rec))
        records = [r for r in records if keep(r)]

        m = re.search(r"(?:SORT|MERGE)\s+FIELDS=\((.*?)\)(?:\s|,|$)", text)
        if m and m.group(1).upper() != "COPY":
            tokens = [t.strip() for t in split_params(m.group(1))]
            keys = []
            while tokens:
                spec = operand(tokens.pop(0), tokens)
                order = tokens.pop(0).upper() if tokens and tokens[0].upper() in ("A", "D") else "A"
                keys.append((spec, order))
            for spec, order in reversed(keys):
                records.sort(key=lambda r: value(r, spec), reverse=(order == "D"))
        out_path = Path(env["DD_SORTOUT"])
        out_path.write_bytes(b"".join(records))
        if infos.get("SORTOUT", {}).get("dsn"):
            self.catalog.set(infos["SORTOUT"]["dsn"], type="flat", lrecl=lrecl)
            for o in self.outputs:
                if o["dsn"] == infos["SORTOUT"]["dsn"]:
                    o["lrecl"] = lrecl
        (stepdir / "SYSOUT.txt").write_text(f"ICE000I SORT: {len(records)} RECORDS OUT, LRECL {lrecl}\n")
        if "DD_SYSOUT" in env and env["DD_SYSOUT"] != "/dev/null":
            shutil.copyfile(stepdir / "SYSOUT.txt", env["DD_SYSOUT"])
        return 0

    def run_step(self, step: Step, stepdir: Path) -> int:  # noqa: F811 (override to keep infos)
        env = self.base_env()
        infos: dict[str, dict] = {}
        for dd in step.dds.values():
            path, info = self.allocate(step, dd, stepdir)
            infos[dd.name] = info
            if path:
                env[f"DD_{dd.name}"] = path
        self._infos = infos
        program = step.program
        if program == "IDCAMS":
            return self.idcams(step, env, infos, stepdir)
        if program in ("SORT", "ICEMAN", "SYNCSORT", "DFSORT"):
            return self.sort(step, env, infos, stepdir)
        if program == "IEFBR14":
            return 0
        if program == "SDSF":
            # CICS file open/close through SDSF: no CICS region locally
            self.log(f"    SDSF step treated as no-op (no CICS region)")
            return 0
        if program == "IEBGENER":
            shutil.copyfile(env["DD_SYSUT1"], env["DD_SYSUT2"])
            return 0
        if program == "IKJEFT01":
            return self.ikjeft01(step, env, infos, stepdir)
        argv = ["PARMDRV", program, step.parm.strip("'")] if step.parm else [program]
        return self._cobcrun(argv, env, stepdir=stepdir)


def find_jcl(name: str) -> Path:
    path = Path(name)
    if path.exists():
        return path.resolve()
    for directory in (JCL_DIR, ROOT / "app" / "app-transaction-type-db2" / "jcl"):
        for ext in (".jcl", ".JCL", ""):
            candidate = directory / f"{name}{ext}"
            if candidate.exists():
                return candidate
    raise JclError(f"JCL member {name} not found")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("jobs", nargs="+", help="JCL member names or paths")
    parser.add_argument("--clock", help="value for COB_CURRENT_DATE, e.g. '2022/07/05 10:00:00'")
    parser.add_argument("--sym", action="append", default=[], help="symbol override NAME=value")
    parser.add_argument("-v", "--verbose", action="store_true")
    args = parser.parse_args()
    symbols = dict(s.split("=", 1) for s in args.sym)
    runner = Runner(clock=args.clock, verbose=True)
    worst = 0
    for job in args.jobs:
        try:
            rc = runner.run_job(find_jcl(job), symbols)
        except JclError as exc:
            print(f"JCL ERROR in {job}: {exc}", file=sys.stderr)
            return 16
        worst = max(worst, rc)
    return 0 if worst <= 4 else 8


if __name__ == "__main__":
    sys.exit(main())
