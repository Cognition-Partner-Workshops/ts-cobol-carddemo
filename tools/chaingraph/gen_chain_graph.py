#!/usr/bin/env python3
"""Code-derived discovery graph: JCL -> program -> copybook -> dataset / table.

    gen_chain_graph.py --out docs/chain-graph.md     regenerate
    gen_chain_graph.py --check docs/chain-graph.md   exit 1 if the file is stale

Nothing here is hand-maintained: every edge comes from parsing app/jcl,
app/proc, app/app-transaction-type-db2/jcl, app/cbl, app/app-transaction-type-db2/cbl,
app/pli and app/asm.  Dynamic CALLs are shown with the variable name and, when
the JCL PARM names the target, resolved from the JCL.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
APP = ROOT / "app"
JCL_DIRS = [APP / "jcl", APP / "app-transaction-type-db2" / "jcl"]
PROC_DIR = APP / "proc"
SRC = {"COBOL": [APP / "cbl", APP / "app-transaction-type-db2" / "cbl"], "PL/I": [APP / "pli"], "HLASM": [APP / "asm"]}
UTILITIES = {"IDCAMS", "SORT", "IEFBR14", "IEBGENER", "IKJEFT01", "SDSF", "DSNTEP2", "DSNTIAD", "ICETOOL"}


def jcl_statements(text: str) -> list[tuple[str, str, str]]:
    """Return (name, operation, operands) for each logical JCL statement."""
    out, pending = [], None
    for raw in text.splitlines():
        line = raw[:72].rstrip()
        if not line.startswith("//") or line.startswith("//*"):
            continue
        body = line[2:]
        if pending is not None:
            pending = (pending[0], pending[1], pending[2] + body.strip())
        else:
            parts = body.split(None, 2)
            if len(parts) >= 2 and parts[1] in ("JOB", "EXEC", "DD", "PROC", "PEND", "SET"):
                pending = (parts[0], parts[1], parts[2] if len(parts) > 2 else "")
            elif parts and parts[0] in ("EXEC", "DD", "PROC", "PEND", "SET"):
                pending = ("", parts[0], parts[1] if len(parts) > 1 else "")
            else:
                continue
        if pending[2].endswith(","):
            continue
        out.append(pending)
        pending = None
    return out


def parse_jobs() -> dict[str, dict]:
    procs = {p.stem.upper(): p for p in PROC_DIR.glob("*.prc")} if PROC_DIR.exists() else {}
    jobs: dict[str, dict] = {}
    for d in JCL_DIRS:
        for path in sorted(d.glob("*.jcl")):
            text = path.read_text(errors="replace")
            steps = parse_steps(text, procs, text)
            jobs[path.stem.upper()] = {"path": path.relative_to(ROOT).as_posix(), "steps": steps}
    return jobs


def parse_steps(text: str, procs: dict, jobtext: str, prefix: str = "") -> list[dict]:
    steps: list[dict] = []
    for name, op, operands in jcl_statements(text):
        if op == "EXEC":
            m = re.match(r"PGM=([A-Z0-9#@$&]+)", operands)
            if m:
                parm = re.search(r"PARM='([^']*)'", operands)
                steps.append({"step": prefix + name, "program": m.group(1), "parm": parm.group(1) if parm else None, "dd": []})
            else:
                pm = re.match(r"(?:PROC=)?([A-Z0-9#@$]+)", operands)
                proc = pm.group(1) if pm else "?"
                if proc in procs:
                    steps.extend(parse_steps(procs[proc].read_text(errors="replace"), procs, jobtext, prefix=f"{name}."))
                else:
                    steps.append({"step": prefix + name, "program": f"PROC {proc}", "parm": None, "dd": []})
        elif op == "DD" and steps and name:
            m = re.search(r"DSN=([A-Z0-9#@$.&()+-]+)", operands)
            if m:
                steps[-1]["dd"].append((name, m.group(1)))
    for step in steps:
        if step["program"] == "IKJEFT01":
            for m in re.finditer(r"RUN\s+PROGRAM\(([A-Z0-9#@$]+)\)(?:[^\n]*?PARMS?\('([^']*)'\))?", jobtext, re.I):
                step["dsn_run"] = m.group(1).upper()
                step["parm"] = m.group(2)
    return steps


def parse_programs() -> dict[str, dict]:
    programs: dict[str, dict] = {}
    for lang, dirs in SRC.items():
        for d in dirs:
            for path in sorted(d.iterdir()) if d.exists() else []:
                if path.suffix.lower() not in (".cbl", ".cob", ".pli", ".asm"):
                    continue
                text = path.read_text(errors="replace")
                info = {"language": lang, "path": path.relative_to(ROOT).as_posix(), "copybooks": [], "dd": [],
                        "static_calls": [], "dynamic_calls": [], "tables": [], "cics": False, "sql": False}
                if lang == "COBOL":
                    code = "\n".join(l[6:72] for l in text.splitlines() if len(l) > 6 and l[6] not in "*/")
                    info["copybooks"] = sorted(set(re.findall(r"\bCOPY\s+([A-Z0-9-]+)", code)) |
                                               set(re.findall(r"EXEC\s+SQL\s+INCLUDE\s+([A-Z0-9-]+)", code)))
                    info["dd"] = sorted(set(re.findall(r"ASSIGN\s+TO\s+([A-Z0-9-]+)", code)))
                    info["static_calls"] = sorted(set(re.findall(r"\bCALL\s+'([A-Z0-9$-]+)'", code)))
                    info["dynamic_calls"] = sorted(set(re.findall(r"\bCALL\s+([A-Z][A-Z0-9-]+)\b", code)))
                    info["cics"] = "EXEC CICS" in code
                    info["sql"] = "EXEC SQL" in code
                    info["dynamic_sql"] = bool(re.search(r"EXEC\s+SQL\s+(PREPARE|EXECUTE)", code))
                    sql = " ".join(re.findall(r"EXEC\s+SQL(.*?)END-EXEC", code, re.S))
                    info["tables"] = sorted(set(t.upper() for t in re.findall(
                        r"\b(?:FROM|UPDATE|INSERT\s+INTO|DELETE\s+FROM|DECLARE)\s+([A-Z_][A-Z0-9_.]*)", sql, re.I)
                        if t.upper() not in ("SQLCA", "TABLE") and not t.startswith(":")))
                elif lang == "PL/I":
                    info["copybooks"] = sorted(set(re.findall(r"%INCLUDE\s+([A-Z0-9_]+)", text)))
                    info["static_calls"] = sorted(set(re.findall(r"\bCALL\s+([A-Z0-9_]+)", text)))
                    m = re.search(r"^\s*([A-Z0-9_]+):\s*PROC", text, re.M)
                    info["entry"] = m.group(1) if m else path.stem.upper()
                programs[path.stem.upper()] = info
    return programs


def render(jobs: dict, programs: dict, chains: dict) -> str:
    out = ["# Chain graph (generated - do not edit)", "",
           "Generated by `make chain-graph` from the JCL, procs and source under `app/`.",
           "`make chain-graph-check` fails CI when this file is stale.", ""]
    out += ["## Runnable chains", ""]
    for cname, chain in chains.items():
        out += [f"### `{cname}` - {chain['description']}", "", "```mermaid", "flowchart LR"]
        for job in chain.get("setup", []) + chain["jobs"]:
            for s in jobs.get(job, {}).get("steps", []):
                prog = s.get("dsn_run") or s["program"]
                node = f"{job}_{s['step']}".replace(".", "_")
                out.append(f'    {node}["{job} {s["step"]}<br/>{prog}"]')
                if prog in programs:
                    out.append(f"    {node} --> {prog}[{prog}]")
                    for dd, dsn in s["dd"]:
                        if dd in programs[prog]["dd"] or dd in ("SYSIN", "SORTIN", "SORTOUT"):
                            out.append(f'    {prog} -.{dd}.- {dsn_id(dsn)}[("{dsn}")]')
                    for tbl in programs[prog]["tables"]:
                        out.append(f'    {prog} -.SQL.- {dsn_id(tbl)}[("{tbl}")]')
                    for target in programs[prog]["static_calls"] + ([s["parm"]] if s.get("parm") and programs[prog]["dynamic_calls"] and s["parm"] in programs else []):
                        if target in programs:
                            out.append(f"    {prog} ==CALL==> {target}[{target}]")
                else:
                    for dd, dsn in s["dd"]:
                        out.append(f'    {node} -.{dd}.- {dsn_id(dsn)}[("{dsn}")]')
        out += ["```", ""]
    out += ["## Jobs", "", "| Job | Step | Program | PARM | DD -> dataset |", "|---|---|---|---|---|"]
    for job, j in sorted(jobs.items()):
        for s in j["steps"]:
            prog = s["program"] + (f" -> {s['dsn_run']}" if s.get("dsn_run") else "")
            dds = "<br>".join(f"`{dd}` {dsn}" for dd, dsn in s["dd"]) or "-"
            out.append(f"| [{job}]({j['path']}) | {s['step']} | {prog} | {s['parm'] or '-'} | {dds} |")
    out += ["", "## Programs", "",
            "| Program | Language | Copybooks / includes | DD names | Static CALLs | Dynamic CALLs | SQL tables | Notes |",
            "|---|---|---|---|---|---|---|---|"]
    callers = {}
    for job, j in jobs.items():
        for s in j["steps"]:
            callers.setdefault(s.get("dsn_run") or s["program"], set()).add(job)
    for name, p in sorted(programs.items()):
        notes = []
        if p["cics"]:
            notes.append("CICS (analysis-only)")
        if p.get("dynamic_sql"):
            notes.append("dynamic SQL PREPARE/EXECUTE")
        if p["language"] != "COBOL":
            notes.append(f"{p['language']} (analysis-only)")
        if name in callers:
            notes.append("jobs: " + ", ".join(sorted(callers[name])))
        out.append(f"| [{name}]({p['path']}) | {p['language']} | {', '.join(p['copybooks']) or '-'} | {', '.join(p['dd']) or '-'} | "
                   f"{', '.join(p['static_calls']) or '-'} | {', '.join(p['dynamic_calls']) or '-'} | {', '.join(p['tables']) or '-'} | {'; '.join(notes) or '-'} |")
    out += ["", "## Cross-language edges", ""]
    for name, p in sorted(programs.items()):
        for target in p["static_calls"]:
            if target in programs and programs[target]["language"] != "COBOL":
                out.append(f"* `{name}` (COBOL) -> `{target}` ({programs[target]['language']}, `{programs[target]['path']}`)")
    for name, p in sorted(programs.items()):
        for var in p["dynamic_calls"]:
            out.append(f"* `{name}` -> dynamic `CALL {var}` (target resolved from JCL PARM where present)")
    return "\n".join(out) + "\n"


def dsn_id(dsn: str) -> str:
    return "ds_" + re.sub(r"[^A-Za-z0-9]", "_", dsn)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--out", type=Path)
    parser.add_argument("--check", type=Path)
    args = parser.parse_args()
    chains = json.loads((ROOT / "tools" / "runjcl" / "chains.json").read_text())
    text = render(parse_jobs(), parse_programs(), chains)
    if args.check:
        if not args.check.exists() or args.check.read_text() != text:
            print(f"{args.check} is stale: run `make chain-graph`", file=sys.stderr)
            return 1
        print(f"{args.check} is up to date")
        return 0
    args.out.write_text(text)
    print(f"wrote {args.out}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
