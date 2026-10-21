"""Compile and run the batch path under GnuCOBOL, then unload every output file."""
import json
import os
import shutil
import subprocess
import time
from pathlib import Path

from . import APP_DIR, SPEC_DIR
from .datasets import INPUTS, OUTPUTS
from .jcl import job_steps
from .records import decode_record

STUBS = SPEC_DIR / "runner" / "stubs"
STUB_SOURCES = ["CEE3ABD.cbl"]

# DD name -> (dataset, organization) for each program, read from the FD and SELECT clauses.
DD = {
    "CBTRN02C": {
        "DALYTRAN": ("dailytran", "seq-in"), "TRANFILE": ("posted", "idx-out"),
        "XREFFILE": ("cardxref", "idx"), "DALYREJS": ("rejects", "seq-out"),
        "ACCTFILE": ("accounts", "idx"), "TCATBALF": ("tcatbal", "idx"),
    },
    "CBACT04C": {
        "TCATBALF": ("tcatbal", "idx"), "XREFFILE": ("cardxref", "idx-alt"),
        "ACCTFILE": ("accounts", "idx"), "DISCGRP": ("discgrp", "idx"),
        "TRANSACT": ("interest", "seq-out"),
    },
}


def _area(text, indent=7):
    return " " * indent + text


def loader_source(name, length, key_len, alt=None, unload=False):
    """A small COBOL program that loads a line file into an indexed file, or unloads it."""
    lines = [
        "IDENTIFICATION DIVISION.", f"PROGRAM-ID. {name}.", "ENVIRONMENT DIVISION.",
        "INPUT-OUTPUT SECTION.", "FILE-CONTROL.",
    ]
    b = [
        "SELECT FLAT-FILE ASSIGN TO FLATFILE",
        "    ORGANIZATION IS " + ("SEQUENTIAL." if unload else "LINE SEQUENTIAL."),
        "SELECT IDX-FILE ASSIGN TO IDXFILE",
        "    ORGANIZATION IS INDEXED",
        "    ACCESS MODE IS " + ("SEQUENTIAL" if unload else "DYNAMIC"),
        "    RECORD KEY IS IDX-KEY",
    ]
    if alt:
        b.append("    ALTERNATE RECORD KEY IS IDX-ALT")
    b.append("    FILE STATUS IS WS-STATUS.")
    out = [_area(x) for x in lines] + [_area(x, 11) for x in b]
    out += [_area(x) for x in ["DATA DIVISION.", "FILE SECTION.", "FD  FLAT-FILE.", f"01  FLAT-REC PIC X({length})."]]
    out += [_area("FD  IDX-FILE."), _area("01  IDX-REC.")]
    if alt:
        off, alen = alt
        rest = length - off - alen
        out += [_area(f"05 IDX-KEY PIC X({key_len}).", 11), _area(f"05 FILLER PIC X({off - key_len}).", 11),
                _area(f"05 IDX-ALT PIC X({alen}).", 11), _area(f"05 FILLER PIC X({rest}).", 11)]
    else:
        out += [_area(f"05 IDX-KEY PIC X({key_len}).", 11), _area(f"05 FILLER PIC X({length - key_len}).", 11)]
    out += [_area("WORKING-STORAGE SECTION."), _area("01  WS-STATUS PIC XX."), _area("01  WS-EOF PIC X VALUE 'N'."),
            _area("PROCEDURE DIVISION.")]
    if unload:
        body = [
            "OPEN INPUT IDX-FILE OUTPUT FLAT-FILE",
            "PERFORM UNTIL WS-EOF = 'Y'",
            "  READ IDX-FILE NEXT",
            "    AT END MOVE 'Y' TO WS-EOF",
            "    NOT AT END WRITE FLAT-REC FROM IDX-REC",
            "  END-READ",
            "END-PERFORM",
            "CLOSE IDX-FILE FLAT-FILE",
            "STOP RUN.",
        ]
    else:
        body = [
            "OPEN INPUT FLAT-FILE OUTPUT IDX-FILE",
            "PERFORM UNTIL WS-EOF = 'Y'",
            "  READ FLAT-FILE",
            "    AT END MOVE 'Y' TO WS-EOF",
            "    NOT AT END",
            "      WRITE IDX-REC FROM FLAT-REC",
            "      IF WS-STATUS NOT = '00'",
            "        DISPLAY 'LOAD FAILED STATUS ' WS-STATUS",
            "        STOP RUN RETURNING 8",
            "      END-IF",
            "  END-READ",
            "END-PERFORM",
            "CLOSE FLAT-FILE IDX-FILE",
            "STOP RUN.",
        ]
    out += [_area(x, 11) for x in body]
    return "\n".join(out) + "\n"


def _run(cmd, log, env=None, cwd=None):
    started = time.monotonic()
    proc = subprocess.run(cmd, cwd=cwd, env=env, capture_output=True, text=True)
    log.write(f"$ {' '.join(map(str, cmd))}\n{proc.stdout}{proc.stderr}rc={proc.returncode}\n")
    return proc.returncode, round(time.monotonic() - started, 3), proc.stdout


class CobolRun:
    def __init__(self, run_dir, cobc="cobc", run_ts="2022/07/18 01:00:00.00"):
        self.run_dir = Path(run_dir)
        self.work = self.run_dir / "cobol" / "work"
        self.out = self.run_dir / "cobol"
        self.cobc = cobc
        self.run_ts = run_ts
        self.stages = []
        self.assumptions = []

    def stage(self, name, seconds, **extra):
        self.stages.append({"stage": name, "seconds": seconds, **extra})

    def prepare(self, log):
        if self.work.exists():
            shutil.rmtree(self.work)
        (self.work / "cpy").mkdir(parents=True)
        (self.work / "bin").mkdir()
        (self.work / "data").mkdir()
        # the committed copybooks are tab indented; fixed format needs real columns
        for cpy in (APP_DIR / "cpy").iterdir():
            (self.work / "cpy" / cpy.name).write_text(cpy.read_text().expandtabs(8))

    def compile(self, program, log, with_parm=False):
        src = APP_DIR / "cbl" / f"{program}.cbl"
        stubs = [STUBS / s for s in STUB_SOURCES]
        cmd = [self.cobc, "-fsign=EBCDIC", "-I", str(self.work / "cpy")]
        if with_parm:
            cmd += ["-x", "-o", str(self.work / "bin" / program), str(STUBS / "PARMDRV.cbl"), str(src)]
        else:
            cmd += ["-x", "-o", str(self.work / "bin" / program), str(src)]
        rc, secs, _ = _run(cmd + [str(s) for s in stubs], log)
        self.stage(f"compile {program}", secs, rc=rc)
        if rc != 0:
            raise RuntimeError(f"compile of {program} failed, see {log.name}")
        self.assumptions.append({"program": program, "stub": "CEE3ABD", "why": "Language Environment abend service; stub ends the run with RC 12"})
        if with_parm:
            self.assumptions.append({"program": program, "stub": "PARMDRV", "why": "JCL PARM operand passed as halfword length and text"})

    def build_tool(self, name, source, log):
        path = self.work / f"{name}.cbl"
        path.write_text(source)
        rc, _, _ = _run([self.cobc, "-x", "-o", str(self.work / "bin" / name), str(path)], log)
        if rc != 0:
            raise RuntimeError(f"tool {name} failed to compile")
        return self.work / "bin" / name

    def load_indexed(self, ds_name, log, alt=False):
        ds = INPUTS[ds_name]
        layout = ds.layout
        key_len = sum(layout.field(k).length for k in ds.key)
        alt_spec = None
        if alt:
            f = layout.field(ds.alt_key)
            alt_spec = (f.offset, f.length)
        tool = self.build_tool(f"LD{ds_name[:4].upper()}{'A' if alt else ''}", loader_source(
            f"LD{ds_name[:4].upper()}{'A' if alt else ''}", layout.length, key_len, alt_spec), log)
        target = self.work / "data" / f"{ds_name}{'.alt' if alt else ''}.idx"
        env = dict(os.environ, DD_FLATFILE=str(self.run_dir / "input" / "ascii" / ds.seed), DD_IDXFILE=str(target))
        rc, secs, _ = _run([str(tool)], log, env=env)
        if rc != 0:
            raise RuntimeError(f"load of {ds_name} failed")
        return target

    def unload_indexed(self, ds, path, log):
        layout = ds.layout
        key_len = sum(layout.field(k).length for k in ds.key)
        name = f"UL{ds.name[:4].upper()}"
        tool = self.build_tool(name, loader_source(name, layout.length, key_len, unload=True), log)
        flat = path.with_suffix(".unl")
        env = dict(os.environ, DD_FLATFILE=str(flat), DD_IDXFILE=str(path))
        rc, _, _ = _run([str(tool)], log, env=env)
        if rc != 0:
            raise RuntimeError(f"unload of {ds.name} failed")
        return flat

    def run_jobs(self, jobs, log):
        data = self.work / "data"
        files = {}
        t0 = time.monotonic()
        for ds_name in ("accounts", "tcatbal", "discgrp", "cardxref"):
            files[ds_name] = self.load_indexed(ds_name, log)
        files["cardxref-alt"] = self.load_indexed("cardxref", log, alt=True)
        daily = INPUTS["dailytran"]
        lines = (self.run_dir / "input" / "ascii" / daily.seed).read_text().splitlines()
        files["dailytran"] = data / "dailytran.seq"
        files["dailytran"].write_text("".join(line.ljust(daily.layout.length)[: daily.layout.length] for line in lines))
        files["posted"] = data / "posted.idx"
        files["rejects"] = data / "rejects.seq"
        files["interest"] = data / "interest.seq"
        self.stage("load indexed files", round(time.monotonic() - t0, 3))
        results = []
        for job in jobs:
            for step in job_steps(job):
                program = step["program"]
                env = dict(os.environ, COB_CURRENT_DATE=self.run_ts)
                for dd, (ds_name, org) in DD[program].items():
                    key = "cardxref-alt" if org == "idx-alt" else ds_name
                    env[f"DD_{dd}"] = str(files[key])
                cmd = [str(self.work / "bin" / program)]
                if step.get("parm"):
                    env.update(JCL_PGM=program, JCL_PARM=step["parm"])
                rc, secs, stdout = _run(cmd, log, env=env)
                self.stage(f"run {job} {program}", secs, rc=rc)
                results.append({"job": job, "program": program, "rc": rc, "display": stdout.splitlines()})
                if rc >= 8:
                    raise RuntimeError(f"{job} {program} ended with RC {rc}")
        return files, results

    def unload(self, files, log):
        t0 = time.monotonic()
        counts = {}
        for name, ds in OUTPUTS.items():
            path = files[name]
            if not path.exists():
                rows = []
            else:
                if name in ("posted", "accounts", "tcatbal"):
                    flat = self.unload_indexed(ds, path, log)
                else:
                    flat = path
                raw = flat.read_bytes().decode("latin-1")
                n = ds.layout.length
                rows = [decode_record(raw[i : i + n], ds.layout) for i in range(0, len(raw), n) if raw[i : i + n].strip()]
            with open(self.out / f"{name}.jsonl", "w") as fh:
                for rec in rows:
                    fh.write(json.dumps(rec, sort_keys=True) + "\n")
            counts[name] = len(rows)
        self.stage("unload outputs", round(time.monotonic() - t0, 3))
        return counts

    def execute(self, jobs):
        self.out.mkdir(parents=True, exist_ok=True)
        with open(self.out / "run.log", "w") as log:
            self.prepare(log)
            programs = []
            for job in jobs:
                for step in job_steps(job):
                    if step["program"] not in programs:
                        self.compile(step["program"], log, with_parm=bool(step.get("parm")))
                        programs.append(step["program"])
            files, results = self.run_jobs(jobs, log)
            counts = self.unload(files, log)
        summary = {"impl": "cobol", "jobs": jobs, "run_ts": self.run_ts, "steps": results,
                   "counts": counts, "stages": self.stages, "assumptions": self.assumptions}
        (self.out / "run.json").write_text(json.dumps(summary, indent=2))
        return summary
