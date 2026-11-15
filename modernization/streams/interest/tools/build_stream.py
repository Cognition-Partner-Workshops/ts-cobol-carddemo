#!/usr/bin/env python3
"""Build the interest stream manifest (manifest.json) from the mftb store and the sources.

Run from the repository root:  python3 modernization/streams/interest/tools/build_stream.py
Every source citation is checked against the file: the cited line must hold the expected token.
The page is rendered from the manifest by render_stream.py.
"""
import json
import re
import subprocess
import sys
from pathlib import Path

repoRoot = Path(__file__).resolve().parents[4]
outDir = repoRoot / "modernization/streams/interest"
modules = ["CARDDEMO", "AUTH", "TRANTYPE", "VSAMMQ"]
rootJob = "INTCALC"
folderName = "MONTHLY-InterestCalculation"
hlq = "AWS.M2.CARDDEMO."
citeErrors = []
fileCache = {}


def lines(path):
    if path not in fileCache:
        fileCache[path] = (repoRoot / path).read_text(errors="replace").splitlines()
    return fileCache[path]


def cite(path, first, last=None, token=None):
    """Return 'path:first[-last]' after checking that token occurs in the cited lines."""
    last = last or first
    body = lines(path)
    if first < 1 or last > len(body):
        citeErrors.append(f"{path}:{first}-{last} out of range")
    elif token and not any(token in ln for ln in body[first - 1:last]):
        citeErrors.append(f"{path}:{first}-{last} lacks {token!r}")
    return f"{path}:{first}" if last == first else f"{path}:{first}-{last}"


def find(path, pattern, after=0):
    """Line number of the first line matching pattern after line `after` (0 = none found)."""
    rx = re.compile(pattern)
    for idx, ln in enumerate(lines(path), 1):
        if idx > after and rx.search(ln):
            return idx
    citeErrors.append(f"{path}: pattern {pattern!r} not found")
    return 0


def load(module, name):
    path = repoRoot / "store" / module / name
    return json.loads(path.read_text()) if path.exists() else None


def git(*args):
    return subprocess.run(["git", *args], cwd=repoRoot, capture_output=True, text=True).stdout.strip()


cbl = "app/cbl/CBACT04C.cbl"
jcl = "app/jcl/INTCALC.jcl"
ctm = "app/scheduler/CardDemo.controlm"
ca7 = "app/scheduler/CardDemo.ca7"
chainsCfg = "tools/runjcl/chains.json"
chainGraph = "docs/chain-graph.md"

store = {m: {n: load(m, n + ".json") for n in
             ["chains", "connections", "online", "jobs", "programs", "schedule", "operations", "dyncalls"]}
         for m in modules}
main = store["CARDDEMO"]
chain = next(c for c in main["chains"]["chains"] if c["table"] == folderName)
chainJob = next(j for j in chain["jobs"] if j["job"] == rootJob)
chainStep = chainJob["steps"][0]
program = next(p for p in main["programs"]["programs"] if p["id"] == "CBACT04C")
dsIndex = {d["dsn"]: d for d in main["connections"]["datasets"]}

# ---------------------------------------------------------------- in-scope set
jobs = [{
    "name": rootJob, "role": "stream", "jcl": jcl,
    "jobCard": cite(jcl, 1, token="//INTCALC JOB"),
    "scheduler": {"export": ctm, "folder": folderName, "definition": cite(ctm, 69, token='JOBNAME="INTCALC"'),
                  "memname": "INTCALC"},
    "description": "Reads the transaction-category balances and writes one interest transaction per balance "
                   "with a non-zero rate, adding the account's interest to its current balance.",
    "descriptionBasis": [cite(jcl, 20, token="compute interest"), cite(cbl, 188, 222, token="1300-COMPUTE-INTEREST")],
}]
steps = [{
    "job": rootJob, "step": "STEP15", "execPgm": "CBACT04C", "parm": "2022071800",
    "source": cite(jcl, 22, token="EXEC PGM=CBACT04C,PARM='2022071800'"),
    "steplib": {"dd": "STEPLIB", "dsn": "AWS.M2.CARDDEMO.LOADLIB", "source": cite(jcl, 23, 24, token="LOADLIB")},
    "sysout": [{"dd": "SYSPRINT", "source": cite(jcl, 25, token="SYSPRINT")},
               {"dd": "SYSOUT", "source": cite(jcl, 26, token="SYSOUT")}],
}]
programs = [{
    "id": "CBACT04C", "kind": "COBOL batch program", "inScope": True, "path": cbl,
    "programId": cite(cbl, 23, token="PROGRAM-ID.    CBACT04C"), "lines": program["lines"],
    "entry": cite(cbl, 180, token="PROCEDURE DIVISION USING EXTERNAL-PARMS"),
    "notes": [
        {"text": "Interest is computed as (TRAN-CAT-BAL * DIS-INT-RATE) / 1200 with no ROUNDED phrase.",
         "evidence": [cite(cbl, 464, 465, token="1200")]},
        {"text": "1400-COMPUTE-FEES is an empty paragraph marked 'To be implemented'.",
         "evidence": [cite(cbl, 518, 520, token="To be implemented")]},
        {"text": "1050-UPDATE-ACCOUNT runs when the account id changes; the ELSE branch that would run it after "
                 "the last record sits inside a loop that ends as soon as END-OF-FILE is 'Y'. docs/BASELINE.md "
                 "records this as legacy behaviour to keep.",
         "evidence": [cite(cbl, 188, 222, token="PERFORM 1050-UPDATE-ACCOUNT"),
                      cite("docs/BASELINE.md", 108, token="Last account never updated")]},
        {"text": "Any file error path ends in 9999-ABEND-PROGRAM, which calls CEE3ABD with ABCODE 999.",
         "evidence": [cite(cbl, 628, 632, token="CEE3ABD")]},
    ],
}, {
    "id": "CEE3ABD", "kind": "runtime routine (not in the estate)", "inScope": False, "path": None,
    "programId": None, "lines": None, "entry": None,
    "notes": [{"text": "Called by literal name from CBACT04C; no source member of this name is in the scanned "
                       "estate. The off-host harness supplies a stub (tools/stubs/CEE3ABD.cbl); it is not part "
                       "of the customer estate.",
               "evidence": [cite(cbl, 632, token="CALL 'CEE3ABD'"), cite("tools/stubs/CEE3ABD.cbl", 1)]}],
}]


def picLength(pic):
    total = 0
    for sym, rep in re.findall(r"([X9])(?:\((\d+)\))?", pic.replace("S", "").replace("V", "")):
        total += int(rep) if rep else 1
    return total


def copybook(name, fileSelect, fdRecord, includeLine, usedAt, usedToken):
    path = f"app/cpy/{name}.cpy"
    fields, offset, record, group = [], 0, None, None
    for idx, ln in enumerate(lines(path), 1):
        text = ln[6:72] if len(ln) > 6 else ""
        if text.lstrip().startswith("*") or ln[6:7] == "*":
            continue
        m = re.match(r"\s*(\d\d)\s+([A-Z0-9-]+)(?:\s+PIC\s+(\S+?))?\.\s*$", text)
        if not m:
            continue
        level, fieldName, pic = int(m.group(1)), m.group(2), m.group(3)
        if level == 1:
            record = fieldName
            continue
        length = picLength(pic) if pic else None
        fields.append({"level": level, "name": fieldName, "pic": pic, "offset": offset if pic else None,
                       "length": length, "line": idx})
        if pic:
            offset += length
    commentLine = find(path, r"RECLN")
    return {
        "name": name, "path": path, "record": record, "recordLength": offset,
        "recordLengthBasis": "sum of the PIC lengths below (all fields are USAGE DISPLAY; a signed field keeps "
                             "its sign in the last byte)",
        "statedLength": cite(path, commentLine, token="RECLN"),
        "includedAt": cite(cbl, includeLine, token=f"COPY {name}"),
        "file": fileSelect, "fdRecord": fdRecord,
        "usedAt": cite(cbl, usedAt, token=usedToken), "fields": fields,
    }


copybooks = [
    copybook("CVTRA01Y", "TCATBAL-FILE", "FD-TRAN-CAT-BAL-RECORD", 97, 326, "INTO TRAN-CAT-BAL-RECORD"),
    copybook("CVACT03Y", "XREF-FILE", "FD-XREFFILE-REC", 102, 394, "INTO CARD-XREF-RECORD"),
    copybook("CVTRA02Y", "DISCGRP-FILE", "FD-DISCGRP-REC", 107, 416, "INTO DIS-GROUP-RECORD"),
    copybook("CVACT01Y", "ACCOUNT-FILE", "FD-ACCTFILE-REC", 112, 373, "INTO ACCOUNT-RECORD"),
    copybook("CVTRA05Y", "TRANSACT-FILE", "FD-TRANFILE-REC", 117, 500, "FROM TRAN-RECORD"),
]
cpyByFile = {c["file"]: c for c in copybooks}


def definition(dsn):
    d = dsIndex[dsn]
    de = d.get("definition") or {}
    out = {"type": d["type"], "definedBy": f'{de.get("job")}.{de.get("step")}', "source": de.get("source")}
    for key in ("keys", "recordsize", "limit", "relate", "pathentry", "shareoptions"):
        if de.get(key):
            out[key] = de[key]
    return out


def dataset(dd, dsn, generation, disp, ddLine, ddToken, select, direction, access, selectLine, fdLine):
    base = dsn
    entry = {
        "dd": dd, "dsn": base, "generation": generation, "disp": disp,
        "ddSource": cite(jcl, ddLine, ddLine + 1, token=ddToken), "catalog": definition(base),
        "cobolFile": select, "direction": direction, "access": access,
        "selectSource": cite(cbl, selectLine, token=f"ASSIGN TO") if selectLine else None,
        "fdSource": cite(cbl, fdLine, token="FD ") if fdLine else None,
    }
    layout = cpyByFile.get(select)
    if layout:
        entry["recordLayout"] = {"copybook": layout["name"], "record": layout["record"],
                                 "recordLength": layout["recordLength"], "boundAt": layout["usedAt"]}
    return entry


datasets = [
    dataset("TCATBALF", hlq + "TCATBALF.VSAM.KSDS", None, "SHR", 27, "TCATBALF.VSAM.KSDS", "TCATBAL-FILE",
            "read", "INDEXED, SEQUENTIAL access", 28, 61),
    dataset("XREFFILE", hlq + "CARDXREF.VSAM.KSDS", None, "SHR", 29, "CARDXREF.VSAM.KSDS", "XREF-FILE",
            "read", "INDEXED, RANDOM access, read by alternate key FD-XREF-ACCT-ID", 34, 69),
    dataset("XREFFIL1", hlq + "CARDXREF.VSAM.AIX.PATH", None, "SHR", 31, "CARDXREF.VSAM.AIX.PATH", None,
            "read (candidate)", "no SELECT names this DD", None, None),
    dataset("ACCTFILE", hlq + "ACCTDATA.VSAM.KSDS", None, "SHR", 33, "ACCTDATA.VSAM.KSDS", "ACCOUNT-FILE",
            "read and rewrite", "INDEXED, RANDOM access", 41, 84),
    dataset("DISCGRP", hlq + "DISCGRP.VSAM.KSDS", None, "SHR", 35, "DISCGRP.VSAM.KSDS", "DISCGRP-FILE",
            "read", "INDEXED, RANDOM access", 47, 76),
]
sysTran = dataset("TRANSACT", hlq + "SYSTRAN", "+1", "(NEW,CATLG,DELETE)", 37, "TRANSACT DD", "TRANSACT-FILE",
                  "write", "SEQUENTIAL", 53, 89)
sysTran["ddSource"] = cite(jcl, 37, 41, token="SYSTRAN(+1)")
sysTran["dcb"] = {"value": "RECFM=F,LRECL=350,BLKSIZE=0", "source": cite(jcl, 39, token="LRECL=350")}
sysTran["gdg"] = {"base": hlq + "SYSTRAN", "relative": "+1", "limit": dsIndex[hlq + "SYSTRAN"]["definition"]["limit"],
                  "definedAt": cite("app/jcl/DEFGDGB.jcl", 48, 51, token="SYSTRAN"),
                  "note": "(+1) is a new generation cataloged at the end of the job; LIMIT(5) SCRATCH keeps five."}
datasets.append(sysTran)
datasets[2]["recordLayout"] = {"copybook": "CVACT03Y", "record": "CARD-XREF-RECORD", "recordLength": 50,
                               "boundAt": "candidate - the path returns base-cluster records of CARDXREF"}
streamDsns = sorted({d["dsn"] for d in datasets})

# ---------------------------------------------------------------- edges
edges = []


def edge(edgeId, kind, source, target, label, status, basis, evidence, direction=None):
    edges.append({"id": edgeId, "kind": kind, "from": source, "to": target, "label": label,
                  "direction": direction, "status": status, "basis": basis, "evidence": evidence})


dynSites = [s for s in main["dyncalls"]["sites"] if "CBACT04C" in json.dumps(s)]
edge("C1", "call", "INTCALC.STEP15", "load module CBACT04C", "EXEC PGM=", "PROVEN",
     "JCL EXEC PGM=CBACT04C, fetched from STEPLIB AWS.M2.CARDDEMO.LOADLIB.",
     [steps[0]["source"], steps[0]["steplib"]["source"]])
edge("C2", "call", "load module CBACT04C", "CBACT04C (app/cbl/CBACT04C.cbl)", "built from", "CANDIDATE",
     "Name match only: the JCL names a load-library member and the source declares PROGRAM-ID CBACT04C, but no "
     "link-edit step, binder listing or library directory in the estate ties the member to this source. The "
     "sample compile job is generic (MEMNAME=BATCHPGM) and names no program.",
     [steps[0]["source"], programs[0]["programId"], cite("samples/jcl/BATCMP.jcl", 33, token="MEMNAME"),
      cite("samples/jcl/BATCMP.jcl", 42, token="MEM=&MEMNAME")])
edge("C3", "call", "CBACT04C", "CEE3ABD", "CALL 'CEE3ABD' USING ABCODE, TIMING", "PROVEN",
     "Static CALL by literal in 9999-ABEND-PROGRAM (error path only). The target is not in the estate.",
     [cite(cbl, 628, 632, token="CALL 'CEE3ABD'")])
for num, cpy in enumerate(copybooks, 1):
    edge(f"I{num}", "copy", "CBACT04C", cpy["name"], f"COPY {cpy['name']}", "PROVEN",
         f"COPY statement; member found at {cpy['path']}.", [cpy["includedAt"], cite(cpy["path"], 4, token="01")])

edge("D1", "data", hlq + "TCATBALF.VSAM.KSDS", "CBACT04C", "DD TCATBALF", "PROVEN",
     "SELECT TCATBAL-FILE ASSIGN TO TCATBALF, OPEN INPUT, sequential READ; the JCL binds DD TCATBALF.",
     [cite(cbl, 28, token="ASSIGN TO TCATBALF"), cite(cbl, 236, token="OPEN INPUT TCATBAL-FILE"),
      cite(cbl, 326, token="READ TCATBAL-FILE"), datasets[0]["ddSource"]], "read")
edge("D2", "data", hlq + "CARDXREF.VSAM.KSDS", "CBACT04C", "DD XREFFILE", "PROVEN",
     "SELECT XREF-FILE ASSIGN TO XREFFILE, OPEN INPUT, READ ... KEY IS FD-XREF-ACCT-ID; the JCL binds DD XREFFILE.",
     [cite(cbl, 34, 38, token="ASSIGN TO   XREFFILE"), cite(cbl, 254, token="OPEN INPUT XREF-FILE"),
      cite(cbl, 394, 395, token="KEY IS FD-XREF-ACCT-ID"), datasets[1]["ddSource"]], "read")
edge("D3", "data", hlq + "CARDXREF.VSAM.AIX.PATH", "CBACT04C", "DD XREFFIL1", "CANDIDATE",
     "Inferred. No SELECT assigns XREFFIL1. XREF-FILE declares ALTERNATE RECORD KEY FD-XREF-ACCT-ID and is read "
     "by that key, and the JCL allocates the alternate-index path to a DD whose name is XREFFILE with its last "
     "character replaced by 1; the alternate index is KEYS(11,25), the offset and length of FD-XREF-ACCT-ID. "
     "That the run time opens the path through this DD is a platform convention, not a statement in the source.",
     [cite(cbl, 38, token="ALTERNATE RECORD KEY IS FD-XREF-ACCT-ID"), cite(cbl, 394, 395, token="KEY IS FD-XREF-ACCT-ID"),
      datasets[2]["ddSource"], cite("app/jcl/XREFFILE.jcl", 72, 74, token="KEYS(11,25)"),
      cite("app/jcl/XREFFILE.jcl", 90, 92, token="PATHENTRY")], "read")
edge("D4", "data", hlq + "ACCTDATA.VSAM.KSDS", "CBACT04C", "DD ACCTFILE", "PROVEN",
     "SELECT ACCOUNT-FILE ASSIGN TO ACCTFILE, OPEN I-O, random READ by FD-ACCT-ID; the JCL binds DD ACCTFILE.",
     [cite(cbl, 41, token="ASSIGN TO ACCTFILE"), cite(cbl, 291, token="OPEN I-O ACCOUNT-FILE"),
      cite(cbl, 373, token="READ ACCOUNT-FILE"), datasets[3]["ddSource"]], "read")
edge("D5", "data", "CBACT04C", hlq + "ACCTDATA.VSAM.KSDS", "DD ACCTFILE", "PROVEN",
     "REWRITE FD-ACCTFILE-REC FROM ACCOUNT-RECORD after adding WS-TOTAL-INT to ACCT-CURR-BAL and zeroing the "
     "cycle credit and debit.",
     [cite(cbl, 350, 356, token="REWRITE FD-ACCTFILE-REC"), datasets[3]["ddSource"]], "write")
edge("D6", "data", hlq + "DISCGRP.VSAM.KSDS", "CBACT04C", "DD DISCGRP", "PROVEN",
     "SELECT DISCGRP-FILE ASSIGN TO DISCGRP, OPEN INPUT, random READ, and a second READ with group 'DEFAULT' "
     "on status 23; the JCL binds DD DISCGRP.",
     [cite(cbl, 47, token="ASSIGN TO DISCGRP"), cite(cbl, 272, token="OPEN INPUT DISCGRP-FILE"),
      cite(cbl, 416, token="READ DISCGRP-FILE"), cite(cbl, 436, 444, token="DEFAULT"), datasets[4]["ddSource"]], "read")
edge("D7", "data", "CBACT04C", hlq + "SYSTRAN(+1)", "DD TRANSACT", "PROVEN",
     "SELECT TRANSACT-FILE ASSIGN TO TRANSACT, OPEN OUTPUT, WRITE FD-TRANFILE-REC FROM TRAN-RECORD; the JCL "
     "binds DD TRANSACT to a new generation of the GDG AWS.M2.CARDDEMO.SYSTRAN.",
     [cite(cbl, 53, token="ASSIGN TO TRANSACT"), cite(cbl, 309, token="OPEN OUTPUT TRANSACT-FILE"),
      cite(cbl, 500, token="WRITE FD-TRANFILE-REC"), sysTran["ddSource"]], "write")
edge("D8", "data", "JCL PARM '2022071800'", "CBACT04C", "PARM-DATE", "PROVEN",
     "PROCEDURE DIVISION USING EXTERNAL-PARMS; PARM-DATE is strung with a six-digit counter into TRAN-ID of "
     "every record written.",
     [steps[0]["source"], cite(cbl, 176, 180, token="PARM-DATE"), cite(cbl, 474, 480, token="STRING PARM-DATE")], "read")
edge("D9", "data", "system clock", "CBACT04C", "FUNCTION CURRENT-DATE", "PROVEN",
     "Z-GET-DB2-FORMAT-TIMESTAMP moves FUNCTION CURRENT-DATE into the timestamp written to TRAN-ORIG-TS and "
     "TRAN-PROC-TS.",
     [cite(cbl, 613, 614, token="FUNCTION CURRENT-DATE"), cite(cbl, 496, 498, token="TRAN-PROC-TS")], "read")
edge("D10", "data", "CBACT04C", "SYSOUT (DD SYSOUT)", "DISPLAY", "CANDIDATE",
     "Inferred. The program issues DISPLAY with no UPON phrase and the step allocates DD SYSOUT; that DISPLAY "
     "output goes to that DD depends on the compiler/run-time output DD option, which is not in the estate.",
     [cite(cbl, 181, token="DISPLAY 'START OF EXECUTION"), steps[0]["sysout"][1]["source"]], "write")

# ---------------------------------------------------------------- setup jobs
cfgLines = lines(chainsCfg)
interestLine = find(chainsCfg, r'^\s*"interest"')
setupLine = find(chainsCfg, r'"setup"', interestLine)
setupEnd = find(chainsCfg, r"\]", setupLine)
jobsLine = find(chainsCfg, r'"jobs"', setupEnd)
chainsDef = json.loads((repoRoot / chainsCfg).read_text())["interest"]
jobIndex = {j["name"]: j for j in main["jobs"]["jobs"]}
tablesOf = {}
dialectOf = {t["name"]: t["dialect"] for t in main["schedule"]["tables"]}
for tbl in main["chains"]["chains"]:
    label = f'{tbl["table"]} [{dialectOf.get(tbl["table"], "not a scheduler table")}]'
    for j in tbl["jobs"]:
        tablesOf.setdefault(j["memname"] or j["job"], set()).add(label)
        tablesOf.setdefault(j["job"], set()).add(label)


def idcamsAction(step):
    text = " ".join((dd.get("inline_text") or "") for dd in step["dds"])
    verbs = []
    for verb in ("DELETE", "DEFINE CLUSTER", "DEFINE ALTERNATEINDEX", "DEFINE PATH", "DEFINE GENERATIONDATAGROUP",
                 "DEFINE GDG", "REPRO", "BLDINDEX"):
        if verb in text.upper() and verb not in verbs:
            verbs.append(verb)
    return ", ".join(verbs) or step["pgm"]


setupNeeds = {
    "DEFGDGB": "Defines the GDG base AWS.M2.CARDDEMO.SYSTRAN that DD TRANSACT writes generation (+1) of. The job "
               "defines five other GDG bases the stream does not use.",
    "ACCTFILE": "Deletes, defines and loads AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS from ACCTDATA.PS (DD ACCTFILE).",
    "XREFFILE": "Deletes, defines and loads AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS from CARDXREF.PS, then defines "
                "and builds its alternate index and path (DDs XREFFILE, XREFFIL1).",
    "TCATBALF": "Deletes, defines and loads AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS from TCATBALF.PS (DD TCATBALF).",
    "DISCGRP": "Deletes, defines and loads AWS.M2.CARDDEMO.DISCGRP.VSAM.KSDS from DISCGRP.PS (DD DISCGRP).",
}
setup = []
for order, name in enumerate(chainsDef["setup"], 1):
    job = jobIndex[name]
    listLine = find(chainsCfg, f'"{name}"', setupLine)
    setup.append({
        "job": name, "role": "setup", "order": order, "jcl": job["path"],
        "listedAt": cite(chainsCfg, listLine, token=name),
        "steps": [{"step": s["step"], "pgm": s["pgm"], "action": idcamsAction(s), "source": s["source"],
                   "datasets": sorted({dd["dsn"] for dd in s["dds"] if dd.get("dsn")})} for s in job["steps"]],
        "provides": setupNeeds[name],
        "scheduledIn": sorted(tablesOf.get(name, [])),
    })
setupMeta = {
    "source": cite(chainsCfg, setupLine, setupEnd, token="setup"),
    "streamJobs": cite(chainsCfg, jobsLine, jobsLine + 2, token="INTCALC"),
    "note": "The setup list is the off-host harness's order for building the files a fresh run needs. It is not "
            "scheduler evidence: the Control-M folder of the stream names none of these jobs.",
    "inputs": chainsDef["inputs"],
}

# ---------------------------------------------------------------- outside-stream dependencies
outside = []
openRx = {}


def openLine(path, cobolFile):
    key = (path, cobolFile)
    if key not in openRx:
        rx = re.compile(r"OPEN\s+(INPUT|OUTPUT|I-O|EXTEND)\s+" + re.escape(cobolFile) + r"\b")
        openRx[key] = next((i for i, ln in enumerate(lines(path), 1) if rx.search(ln)), 0)
    return openRx[key]


modeDirection = {"INPUT": "read", "OUTPUT": "write", "I-O": "read-write", "EXTEND": "write"}
chainSteps = {}
for tbl in main["chains"]["chains"]:
    for j in tbl["jobs"]:
        for s in j["steps"]:
            for way, key in (("read", "reads"), ("write", "writes")):
                for io in s[key]:
                    chainSteps.setdefault((j["memname"] or j["job"], s["step"], io["dd"], io["dsn"]), []).append(
                        (way, io["basis"], io["source"]))
noteDirections = {}
for tblNotes in (load("CARDDEMO", "chainnotes.json") or {}).get("tables", {}).values():
    for note in tblNotes.get("directions", []):
        noteDirections[(note["job"], note["step"], note["dd"])] = note
streamRoles = {"CLOSEFIL": "predecessor", "COMBTRAN": "successor", "OPENFIL": "later in folder",
               "WAITSTEP": "later in folder"}
setupNames = set(chainsDef["setup"])


def addOutside(dsn, actorKind, actor, module, via, direction, status, basis, evidence, program=None, extra=None):
    row = {"dataset": dsn, "actorKind": actorKind, "actor": actor, "module": module, "via": via,
           "program": program, "direction": direction, "status": status, "basis": basis, "evidence": evidence}
    if actorKind == "batch job step":
        jobName = actor.split(".")[0]
        row["scheduledIn"] = sorted(tablesOf.get(jobName, []))
        row["relation"] = ("setup" if jobName in setupNames else streamRoles.get(jobName, "other"))
    row.update(extra or {})
    outside.append(row)


for dsn in streamDsns:
    ds = dsIndex[dsn]
    de = ds.get("definition") or {}
    if de:
        addOutside(dsn, "batch job step", f'{de["job"]}.{de["step"]}', "CARDDEMO", "IDCAMS SYSIN", "define",
                   "PROVEN", f'IDCAMS DEFINE ({de["kind"]}) names the dataset', [de["source"]], "IDCAMS")
    for dl in ds.get("deleted_by", []):
        addOutside(dsn, "batch job step", f'{dl["job"]}.{dl["step"]}', "CARDDEMO", "IDCAMS SYSIN", "delete",
                   "PROVEN", "IDCAMS DELETE names the dataset", [dl["source"]], "IDCAMS")
    for st in ds["steps"]:
        if st["job"] == rootJob:
            continue
        binds = [p for p in ds["programs"] if p["job"] == st["job"] and p["dd"] == st["dd"]]
        utils = [u for u in ds["utility_use"] if u["job"] == st["job"] and u["step"] == st["step"]]
        chainIo = chainSteps.get((st["job"], st["step"], st["dd"], st["dsn"].split("(")[0]), [])
        gen = re.search(r"\(([+-]?\d+)\)$", st["dsn"])
        via = f'DD {st["dd"]}' + (f' generation ({gen.group(1)})' if gen else "")
        if binds:
            for b in binds:
                found = openLine(b["source"].split(":")[0], b["file"])
                ev = [b["source"], st["source"]]
                if found:
                    ev.insert(1, f'{b["source"].split(":")[0]}:{found}')
                for mode in b["open_modes"]:
                    addOutside(dsn, "batch job step", f'{st["job"]}.{st["step"]}', "CARDDEMO", via,
                               modeDirection.get(mode, "unknown"), "PROVEN",
                               f'{b["program"]} SELECT {b["file"]} ASSIGN TO {b["dd"]}, OPEN {mode}', ev,
                               b["program"] if b["program"] == st["program"] else f'{st["program"]} -> {b["program"]}')
        elif utils:
            for u in utils:
                addOutside(dsn, "batch job step", f'{st["job"]}.{st["step"]}', "CARDDEMO", via,
                           "write" if u["role"] == "repro-to" else "read", "PROVEN",
                           f'{u["utility"]} {"OUTFILE" if u["role"] == "repro-to" else "INFILE"} names DD {st["dd"]}',
                           [u["source"], st["source"]], st["program"])
        elif st["program"] == "SORT" and st["dd"] in ("SORTIN", "SORTOUT"):
            addOutside(dsn, "batch job step", f'{st["job"]}.{st["step"]}', "CARDDEMO", via,
                       "read" if st["dd"] == "SORTIN" else "write", "PROVEN",
                       f'SORT utility convention: {st["dd"]} is the sort {"input" if st["dd"] == "SORTIN" else "output"}',
                       [st["source"]], "SORT")
        elif noteDirections.get((st["job"], st["step"], st["dd"])):
            note = noteDirections[(st["job"], st["step"], st["dd"])]
            addOutside(dsn, "batch job step", f'{st["job"]}.{st["step"]}', "CARDDEMO", via,
                       {"R": "read", "W": "write", "RW": "read-write"}[note["direction"]], "CANDIDATE",
                       f'{note["basis"]} (intake-step chain note). Candidate: the control member is read from '
                       "&CNTLLIB(REPROCT); that app/ctl/REPROCT.ctl is that member rests on the estate map, not "
                       "on a library listing", note["evidence"] + [st["source"]], st["program"])
        elif chainIo:
            firm = [c for c in chainIo if not c[1].startswith("disp-only")]
            for way, basis, src in sorted(set(firm or chainIo)):
                addOutside(dsn, "batch job step", f'{st["job"]}.{st["step"]}', "CARDDEMO", via, way,
                           "PROVEN" if not basis.startswith("disp-only") else "CANDIDATE", basis, [src], st["program"])
        else:
            addOutside(dsn, "batch job step", f'{st["job"]}.{st["step"]}', "CARDDEMO", via, "unknown", "CANDIDATE",
                       f'DD allocation only (DISP={st["disp"]}); no program or utility binding proves a direction',
                       [st["source"]], st["program"])

# CICS: file definitions, programs and transactions
cicsDsn = {}
for f in main["connections"]["cics"]["files"]:
    dsn = ((f.get("csd") or {}).get("attrs") or {}).get("DSNAME")
    if dsn in streamDsns:
        cicsDsn[f["name"]] = (dsn, f["csd"]["source"])
readCommands = {"READ", "READNEXT", "READPREV", "STARTBR", "ENDBR", "RESETBR"}


def cicsDirection(commands):
    reads = any(c in readCommands for c in commands)
    writes = any(c not in readCommands for c in commands)
    return "read-write" if reads and writes else ("write" if writes else "read")


for module in modules:
    conn = store[module]["connections"]
    for f in conn["cics"]["files"]:
        if f["name"] not in cicsDsn:
            continue
        dsn, csdSource = cicsDsn[f["name"]]
        sameModule = bool(f.get("csd"))
        byProgram = {}
        for u in f["uses"]:
            byProgram.setdefault(u["program"], []).append(u)
        for prog, uses in sorted(byProgram.items()):
            commands = sorted({u["command"] for u in uses})
            constant = all(u.get("resolution") == "constant" for u in uses)
            status = "PROVEN" if sameModule and constant else "CANDIDATE"
            basis = f'EXEC CICS {", ".join(commands)} on file {f["name"]}; DEFINE FILE({f["name"]}) DSNAME in {csdSource}'
            if not sameModule:
                basis += f". Candidate: module {module} has no DEFINE FILE of its own for {f['name']}; the binding " \
                         "to this dataset assumes it runs in the region that installs group CARDDEMO"
            if not constant:
                basis += ". Candidate: the file name is not a constant at every site"
            addOutside(dsn, "CICS program", prog, module, f'CICS file {f["name"]}', cicsDirection(commands), status,
                       basis, sorted({u["source"] for u in uses}) + [csdSource], prog,
                       {"commands": commands, "sites": len(uses)})
    online = store[module]["online"]
    for tx in (online or {}).get("transactions", []):
        certainty = {p["id"]: p["certainty"] for p in tx["programs"]}
        for f in tx["resources"]["files"]:
            if f["name"] not in cicsDsn:
                continue
            dsn, csdSource = cicsDsn[f["name"]]
            for wanted in (True, False):
                uses = [u for u in f["uses"] if (certainty.get(u["program"]) == "proven") == wanted]
                if not uses:
                    continue
                progs = sorted({u["program"] for u in uses})
                commands = sorted({u["command"] for u in uses})
                proven = wanted and bool(f.get("csd"))
                basis = (f'transaction {tx["transid"]} (initial program {tx["initial_program"]}) reaches '
                         + ", ".join(f"{p} [{certainty.get(p, 'unknown')}]" for p in progs)
                         + f', which issue EXEC CICS {", ".join(commands)} on file {f["name"]}')
                if not wanted:
                    basis += ". Candidate: the program is reached only through a dynamic XCTL/LINK target"
                elif not proven:
                    basis += f". Candidate: module {module} has no DEFINE FILE of its own for {f['name']}"
                addOutside(dsn, "CICS transaction", tx["transid"], module, f'CICS file {f["name"]}',
                           cicsDirection(commands), "PROVEN" if proven else "CANDIDATE", basis,
                           [tx["definition"]["source"]] + sorted({u["source"] for u in uses})[:6] + [csdSource],
                           ", ".join(progs), {"commands": commands, "sites": len(uses)})

# CLOSEFIL / OPENFIL: CICS file state of the stream's datasets
for jobName, verb, token in (("CLOSEFIL", "CLO", "CLCIFIL"), ("OPENFIL", "OPE", "OPCIFIL")):
    path = f"app/jcl/{jobName}.jcl"
    stepLine = find(path, f"//{token} ")
    for fileName, (dsn, csdSource) in sorted(cicsDsn.items()):
        cmdLine = find(path, rf"CEMT SET FIL\({fileName} ")
        addOutside(dsn, "batch job step", f"{jobName}.{token}", "CARDDEMO", f"CICS file {fileName}", "control",
                   "PROVEN", f"SDSF in-stream command /F CICSAWSA,'CEMT SET FIL({fileName}) {verb}' - changes the "
                   "file's open state in the CICS region; it reads and writes no record. That the region acts on "
                   "the command is not visible in the JCL",
                   [cite(path, cmdLine, token=fileName), cite(path, stepLine, token="PGM=SDSF"), csdSource], "SDSF")

outside.sort(key=lambda r: (r["dataset"], ["batch job step", "CICS program", "CICS transaction"].index(r["actorKind"]),
                            r["actor"], r["direction"]))
for num, row in enumerate(outside, 1):
    row["id"] = f"O{num}"

# ---------------------------------------------------------------- scheduler
ops = next(j for t in main["operations"]["tables"] if t.get("table", t.get("name")) == folderName
           for j in t["jobs"] if j["job"] == rootJob) if main["operations"] else {}
ca7Mentions = sum(1 for ln in lines(ca7) if re.search(r"\b(INTCALC|COMBTRAN)\b", ln))
scheduler = {
    "exports": [
        {"file": ctm, "dialect": "Control-M folder XML", "namesStreamJob": True,
         "folder": cite(ctm, 64, token=f'FOLDER_NAME="{folderName}"')},
        {"file": ca7, "dialect": "CA 7 LJOB listing", "namesStreamJob": False,
         "note": f"No line of the listing names INTCALC or COMBTRAN ({ca7Mentions} matches), so CA 7 states no "
                 "predecessor or successor for the stream. It does define CLOSEFIL, WAITSTEP and OPENFIL with "
                 "different neighbours.",
         "evidence": [cite(ca7, 18, token="LJOB,JOB=CLOSEFIL"), cite(ca7, 43, token="JOB=CBPAUP0J")]},
    ],
    "folderOrder": [
        {"position": i + 1, "job": j["job"], "role": {"INTCALC": "stream"}.get(j["job"], "neighbour"),
         "definition": j["source"]} for i, j in enumerate(chain["jobs"])],
    "predecessors": [
        {"job": "CLOSEFIL", "distance": 1, "status": "PROVEN",
         "basis": "INTCALC waits on IN condition MONTHLY-InterestCalculation-CLOSEFIL (ODAT), which CLOSEFIL adds "
                  "on completion.",
         "evidence": [cite(ctm, 66, token='NAME="MONTHLY-InterestCalculation-CLOSEFIL" ODATE="ODAT" SIGN="+"'),
                      cite(ctm, 70, token='INCOND NAME="MONTHLY-InterestCalculation-CLOSEFIL"')],
         "whatItDoes": "One SDSF step that sends CEMT SET FIL(...) CLO for CICS files TRANSACT, CCXREF, ACCTDAT, "
                       "CXACAIX and USRSEC to region CICSAWSA.",
         "whatEvidence": [cite("app/jcl/CLOSEFIL.jcl", 22, 30, token="CEMT SET FIL")]},
    ],
    "successors": [
        {"job": "COMBTRAN", "distance": 1, "status": "PROVEN",
         "basis": "COMBTRAN waits on IN condition MONTHLY-InterestCalculation-INTCALC (ODAT), which INTCALC adds "
                  "on completion.",
         "evidence": [cite(ctm, 72, token='NAME="MONTHLY-InterestCalculation-INTCALC" ODATE="ODAT" SIGN="+"'),
                      cite(ctm, 76, token='INCOND NAME="MONTHLY-InterestCalculation-INTCALC"')],
         "whatItDoes": "SORT of TRANSACT.BKUP(0) concatenated with SYSTRAN(0) into TRANSACT.COMBINED(+1), then "
                       "IDCAMS REPRO of that generation into TRANSACT.VSAM.KSDS.",
         "whatEvidence": [cite("app/jcl/COMBTRAN.jcl", 22, 26, token="SYSTRAN(0)"),
                          cite("app/jcl/COMBTRAN.jcl", 41, 48, token="REPRO INFILE(TRANSACT) OUTFILE(TRANVSAM)")]},
        {"job": "WAITSTEP", "distance": 2, "status": "PROVEN",
         "basis": "Waits on MONTHLY-InterestCalculation-COMBTRAN.",
         "evidence": [cite(ctm, 82, token='INCOND NAME="MONTHLY-InterestCalculation-COMBTRAN"')],
         "whatItDoes": "Runs COBSWAIT.", "whatEvidence": [cite("app/jcl/WAITSTEP.jcl", 22, token="COBSWAIT")]},
        {"job": "OPENFIL", "distance": 3, "status": "PROVEN",
         "basis": "Waits on MONTHLY-InterestCalculation-WAITSTEP.",
         "evidence": [cite(ctm, 88, token='INCOND NAME="MONTHLY-InterestCalculation-WAITSTEP"')],
         "whatItDoes": "One SDSF step that sends CEMT SET FIL(...) OPE for the same five CICS files.",
         "whatEvidence": [cite("app/jcl/OPENFIL.jcl", 22, 30, token="CEMT SET FIL")]},
    ],
    "runAttributes": {
        "definition": cite(ctm, 69, token='MAXRERUN="5"'),
        "stated": {"window": (ops.get("window") or {}).get("value"), "rerun": (ops.get("rerun") or {}).get("value"),
                   "runAs": (ops.get("owner") or {}).get("value")},
        "notStated": ops.get("unknown", []),
    },
    "unproven": [],
}


def unproven(itemId, text, evidence, ask=None):
    scheduler["unproven"].append({"id": itemId, "text": text, "evidence": evidence, "ask": ask})


unproven("U1", "How often the folder runs. The folder and job descriptions say MONTHLY, but the definition carries "
         "every month flag, no day rule and RULE_BASED_CALENDARS NAME=\"*\"; the calendar is not in the export. "
         "The store records frequency as unknown.", [cite(ctm, 64, token="MONTHLY"), cite(ctm, 73, token="RULE_BASED_CALENDARS")], "A3")
unproven("U2", "INTCALC against the posting job. POSTTRAN (CBTRN02C) opens TCATBALF and ACCTDATA I-O and builds the "
         "category balances INTCALC reads. POSTTRAN is in the CA 7 listing only; INTCALC is in the Control-M export "
         "only. No condition, trigger or dataset trigger orders one against the other.",
         [cite("app/jcl/POSTTRAN.jcl", 39, 42, token="TCATBALF"), cite(ca7, 70, token="JOB=POSTTRAN")], "A6")
unproven("U3", "INTCALC against the reload of its inputs. TCATBALF.STEP05-STEP15 delete, redefine and reload the "
         "category-balance file (CA 7 triggers job TCATBALF after CLOSEFIL2); DISCGRP.STEP05-STEP15 do the same for "
         "the disclosure-group file (Control-M folder DisclosureGroupsRefresh). Neither is ordered against INTCALC.",
         [cite(ca7, 271, token="JOB=TCATBALF"), cite("app/jcl/TCATBALF.jcl", 22, 25, token="DELETE"),
          cite("app/jcl/DISCGRP.jcl", 22, 25, token="DELETE")], "A6")
unproven("U4", "That the CICS files are closed before STEP15 opens the datasets. CLOSEFIL only submits modify "
         "commands; nothing in the JCL or the export waits for, or checks, the region's response. The folder has a "
         "wait job before OPENFIL but none between CLOSEFIL and INTCALC.",
         [cite("app/jcl/CLOSEFIL.jcl", 22, 30, token="CEMT SET FIL"), cite(ctm, 81, token='JOBNAME="WAITSTEP"')], "A7")
unproven("U5", "That COMBTRAN's SYSTRAN(0) is the generation this run of INTCALC wrote. INTCALC is the only scanned "
         "writer of the GDG, and the folder order puts COMBTRAN after it, so it holds for one clean run per order "
         "date. MAXRERUN=5 on INTCALC allows reruns, and each run catalogs another generation.",
         [cite("app/jcl/COMBTRAN.jcl", 25, 26, token="SYSTRAN(0)"), sysTran["ddSource"], cite(ctm, 69, token='MAXRERUN="5"')], "A4")
unproven("U6", "Which export is authoritative. CLOSEFIL, WAITSTEP and OPENFIL are defined in both; in CA 7 CLOSEFIL "
         "triggers CBPAUP0J, in Control-M its condition releases INTCALC. The exports do not say whether both "
         "schedulers run, or which replaced which.",
         [cite(ca7, 43, token="JOB=CBPAUP0J"), cite(ctm, 65, 66, token="CLOSEFIL")], "A6")
unproven("U7", "COMBTRAN's other input. TRANSACT.BKUP(0) is written as (+1) by TRANBKP and by TRANREPT; which ran last "
         "before the folder is not ordered. This concerns the successor, not the stream's own logic.",
         [cite("app/jcl/COMBTRAN.jcl", 23, 24, token="TRANSACT.BKUP(0)")])

# ---------------------------------------------------------------- two views
graphLines = lines(chainGraph)
graphSection = find(chainGraph, r"^### `interest`")
graphProgram = find(chainGraph, r"^\| \[CBACT04C\]")
graphJob = find(chainGraph, r"^\| \[INTCALC\]")
graphCombtran = find(chainGraph, r"^\| \[COMBTRAN\].*STEP05R")
graphEdge = find(chainGraph, r"CBACT04C -\.TCATBALF\.-", graphSection)
graphSetup = find(chainGraph, r'DEFGDGB_STEP05\["DEFGDGB STEP05', graphSection)
views = {
    "mftb": {"entryPoint": "batch-chain-analysis (mftb 0.2.1)",
             "command": "python3 -m mftb scan-chains --module CARDDEMO --store store INTCALC",
             "resolved": f"selector INTCALC resolves to table {folderName} (5 jobs, 6 steps); the same record is in "
                         "store/CARDDEMO/chains.json from the intake step",
             "rerun": "Re-run in this step to confirm the selector and the chain record; the chain's jobs, steps, "
                      "reads, writes, lineage and asks came back unchanged. The re-run also refreshed other store "
                      "files (timestamps, module boundary); those changes were discarded so this branch leaves "
                      "store/ as the intake step wrote it."},
    "chainGraph": {"file": chainGraph, "section": cite(chainGraph, graphSection, token="interest"),
                   "generatedBy": "tools/chaingraph/gen_chain_graph.py (make chain-graph)"},
    "agreements": [
        "INTCALC has one step, STEP15, EXEC PGM=CBACT04C with PARM 2022071800.",
        "CBACT04C copies CVACT01Y, CVACT03Y, CVTRA01Y, CVTRA02Y and CVTRA05Y.",
        "CBACT04C makes no dynamic CALL; the DDs TCATBALF, XREFFILE, ACCTFILE, DISCGRP and TRANSACT are bound to "
        "the same five datasets.",
    ],
    "disagreements": [],
}


def disagree(itemId, topic, mftbSays, graphSays, decision, evidence):
    views["disagreements"].append({"id": itemId, "topic": topic, "mftb": mftbSays, "chainGraph": graphSays,
                                   "decision": decision, "evidence": evidence})


disagree("V1", "What the chain contains",
         f"The chain is the Control-M folder {folderName}: CLOSEFIL, INTCALC, COMBTRAN, WAITSTEP, OPENFIL, in that order.",
         "The chain 'interest' is INTCALC plus the five setup jobs DEFGDGB, ACCTFILE, XREFFILE, TCATBALF, DISCGRP; "
         "no scheduler job appears.",
         "Neither set is the stream. The root is INTCALC, so stream logic is INTCALC.STEP15 and what it reaches. "
         "CLOSEFIL and COMBTRAN are the predecessor and successor the Control-M export names; the five setup jobs "
         "come from the harness list and are marked setup.",
         [cite(ctm, 64, 92, token=folderName), setupMeta["source"], cite(chainGraph, graphSetup, token="DEFGDGB")])
disagree("V2", "DD XREFFIL1 (alternate-index path)",
         "Listed as a read of STEP15 with basis 'disp-only SHR (no program binding)', and as a chain input "
         "'produced online: CICS FCT CXACAIX'.",
         "The DD is in the job table, but the program's DD list omits it and the diagram draws no edge for it.",
         "The source reads XREF-FILE by its alternate key, which is what the path serves, but never names "
         "XREFFIL1. Recorded as a CANDIDATE read edge (D3). The path is defined by XREFFILE.STEP25, not produced "
         "online; CICS file CXACAIX is a second user of the same path.",
         [cite(cbl, 38, token="ALTERNATE RECORD KEY"), cite(cbl, 394, 395, token="KEY IS FD-XREF-ACCT-ID"),
          datasets[2]["ddSource"], cite("app/jcl/XREFFILE.jcl", 90, 92, token="PATHENTRY"),
          cite(chainGraph, graphProgram, token="CBACT04C")])
disagree("V3", "CALL 'CEE3ABD'",
         "The chain step lists no reached program (reached: []); programs.json records the call as a system routine.",
         "CEE3ABD is listed under Static CALLs of CBACT04C.",
         "The source has a static CALL by literal, on the abend path only (C3, PROVEN). The target has no source "
         "in the estate, so it is a boundary, not an in-scope program.",
         [cite(cbl, 632, token="CALL 'CEE3ABD'"), cite(chainGraph, graphProgram, token="CEE3ABD")])
disagree("V4", "Direction of the dataset edges",
         "Each DD has a direction taken from the program's OPEN mode: four reads, ACCTFILE read and write, TRANSACT write.",
         "Dataset edges are drawn undirected (-.DD.-).",
         "The OPEN statements and the READ, REWRITE and WRITE verbs in the source give the directions mftb states "
         "(D1, D2, D4-D7).",
         [cite(cbl, 236, token="OPEN INPUT"), cite(cbl, 291, token="OPEN I-O"), cite(cbl, 309, token="OPEN OUTPUT"),
          cite(cbl, 356, token="REWRITE"), cite(chainGraph, graphEdge, token="-.TCATBALF.-")])
disagree("V5", "Who reads SYSTRAN",
         "The chain pass listed only the first dataset of COMBTRAN's SORTIN and filed SYSTRAN as read by no job; "
         "the intake step closed that ask by hand. connections.json does hold the SYSTRAN(0) allocation.",
         "COMBTRAN STEP05R is shown with SORTIN = TRANSACT.BKUP(0) only.",
         "Both generated views miss the concatenation. COMBTRAN.jcl allocates SYSTRAN(0) as the second dataset "
         "of SORTIN, so COMBTRAN reads the GDG INTCALC writes (outside-stream dependency, successor).",
         [cite("app/jcl/COMBTRAN.jcl", 23, 26, token="SYSTRAN(0)"), cite(chainGraph, graphCombtran, token="SORTIN")])
disagree("V6", "Scheduling",
         "Order, conditions, MAXRERUN and window come from the Control-M export; frequency is recorded as unknown.",
         "No scheduler information; the graph is built from JCL, procs and source only.",
         "The Control-M export is the only evidence of order. Used for predecessor and successor; what it does "
         "not prove is listed under Scheduler.",
         [cite(ctm, 69, 74, token="INTCALC"), cite(chainGraph, 3, token="JCL")])
if dynSites:
    citeErrors.append("dyncalls.json has a site for CBACT04C; the manifest assumes none")

# ---------------------------------------------------------------- open asks
asks = []


def ask(askId, severity, owner, question, why, evidence, related):
    asks.append({"id": askId, "severity": severity, "owner": owner, "question": question, "why": why,
                 "evidence": evidence, "related": related})


ask("A1", "medium", "build / library owner",
    "Provide the link-edit (binder) listing or the directory of AWS.M2.CARDDEMO.LOADLIB showing which source "
    "member CBACT04C was built from.",
    "The file name, the PROGRAM-ID and the load-module name are the same string, and nothing else in the estate "
    "connects the executed member to this source.", edges[1]["evidence"], ["C2"])
ask("A2", "medium", "scheduler / application owner",
    "Confirm how the cycle date in INTCALC's PARM is changed for each run (manual edit, scheduler variable, or a "
    "different production member).",
    "The member as scanned passes the literal 2022071800, which becomes the first ten characters of every "
    "interest transaction id; the Control-M definition supplies no variable. Carried over from the mftb chain ask.",
    [steps[0]["source"], cite(cbl, 474, 480, token="STRING PARM-DATE"), cite(ctm, 69, 74, token="INTCALC")], ["D8"])
ask("A3", "medium", "scheduler owner",
    "Provide the calendar or day rule the folder MONTHLY-InterestCalculation runs under.",
    "The export proves order inside the folder, not when or how often the folder is ordered.",
    scheduler["unproven"][0]["evidence"], ["U1"])
ask("A4", "high", "application owner",
    "State the rerun procedure for INTCALC after a failure or a duplicate submission.",
    "The definition allows five automatic reruns. STEP15 adds interest to ACCT-CURR-BAL with REWRITE as it goes "
    "and catalogs a new SYSTRAN generation per run; nothing in the JCL or the program restores the account file, "
    "skips accounts already updated or removes the earlier generation.",
    [cite(ctm, 69, token='MAXRERUN="5"'), cite(cbl, 350, 356, token="REWRITE"), sysTran["ddSource"]], ["D5", "D7", "U5"])
ask("A5", "low", "application owner",
    "Confirm that DD XREFFIL1 is the alternate-index path CBACT04C uses for the read by FD-XREF-ACCT-ID (a job "
    "log showing the DD opened, or the compile listing).",
    "The program never names the DD; the edge is inferred from the alternate key and the DD name.",
    edges[next(i for i, e in enumerate(edges) if e["id"] == "D3")]["evidence"], ["D3", "V2"])
ask("A6", "high", "scheduler owner",
    "State the required order of INTCALC against POSTTRAN, the TCATBALF and DISCGRP reloads and the other "
    "writers of the account file, and which scheduler export is authoritative.",
    "The stream's inputs are written by jobs in other tables or in no table, and the two exports overlap without "
    "a cross-reference.",
    scheduler["unproven"][1]["evidence"] + scheduler["unproven"][2]["evidence"] + scheduler["unproven"][5]["evidence"],
    ["U2", "U3", "U6"])
ask("A7", "medium", "CICS / operations owner",
    "Confirm that the CICS region closes ACCTDAT, CCXREF and CXACAIX before STEP15 runs, and what happens to an "
    "online update in flight.",
    "Online programs rewrite the account file the stream rewrites; the only separation in evidence is a modify "
    "command whose outcome is not checked.",
    scheduler["unproven"][3]["evidence"], ["U4"])
ask("A8", "low", "application owner",
    "Confirm that fee calculation is not part of this job.",
    "The JCL comment says the step computes interest and fees; 1400-COMPUTE-FEES is empty.",
    [cite(jcl, 20, token="interest and fees"), cite(cbl, 518, 520, token="To be implemented")], [])
ask("A9", "low", "CICS owner",
    "Confirm that the AUTH and VSAMMQ programs that use files ACCTDAT, CCXREF and CXACAIX run in a region where "
    "those names are the CARDDEMO group's definitions.",
    "Those modules' own CSD members do not define the files; their edges to the stream's datasets are CANDIDATE.",
    [src for src in sorted({v[1] for v in cicsDsn.values()})], [])
ask("A10", "low", "build / operations owner",
    "Confirm where DISPLAY output of CBACT04C is written.",
    "The harness compares SYSOUT for this step; the DISPLAY-to-SYSOUT edge rests on a compiler or run-time option "
    "not in the estate.", edges[-1]["evidence"], ["D10"])

# ---------------------------------------------------------------- assemble
excerpts = {}


def collect(node):
    if isinstance(node, dict):
        for v in node.values():
            collect(v)
    elif isinstance(node, list):
        for v in node:
            collect(v)
    elif isinstance(node, str):
        m = re.fullmatch(r"([A-Za-z0-9_./-]+\.[A-Za-z0-9]+):(\d+)(?:-(\d+))?", node)
        if m and (repoRoot / m.group(1)).is_file():
            first, last = int(m.group(2)), int(m.group(3) or m.group(2))
            body = lines(m.group(1))
            last = min(last, len(body), first + 39)
            excerpts[node] = {"path": m.group(1), "first": first,
                              "lines": [ln.rstrip()[:200] for ln in body[first - 1:last]]}


def count(rows, key, value):
    return sum(1 for r in rows if r[key] == value)


baselineSha = git("rev-parse", "origin/demo/01-intake")
manifest = {
    "meta": {
        "stream": "interest", "title": "Interest stream - INTCALC",
        "root": {"job": rootJob, "jcl": jcl, "schedulerFolder": folderName, "schedulerExport": ctm},
        "baseline": {"branch": "demo/01-intake", "sha": baselineSha},
        "repository": "Cognition-Partner-Workshops/ts-cobol-carddemo",
        "generatedBy": "modernization/streams/interest/tools/build_stream.py",
        "inputs": ["store/CARDDEMO (mftb 0.2.1, intake step)", "store/AUTH, store/TRANTYPE, store/VSAMMQ",
                   chainGraph, chainsCfg, ctm, ca7, jcl, cbl, "app/cpy"],
        "statusLegend": {"PROVEN": "static and cited: a statement in a scanned source names both ends",
                         "CANDIDATE": "dynamic or inferred: rests on a naming convention, a run-time option or a "
                                      "target the source does not name"},
        "outOfScope": "The posting chain (POSTTRAN, TRANREPT) is not part of this stream; its jobs appear only "
                      "as outside-stream users of the same datasets. No target code is produced in this step.",
    },
    "scope": {"jobs": jobs, "steps": steps, "programs": programs, "copybooks": copybooks, "datasets": datasets},
    "edges": edges,
    "setup": {"meta": setupMeta, "jobs": setup},
    "outside": outside,
    "scheduler": scheduler,
    "views": views,
    "openAsks": asks,
}
manifest["counts"] = {
    "jobs": len(jobs), "steps": len(steps), "programsInScope": sum(1 for p in programs if p["inScope"]),
    "boundaryRoutines": sum(1 for p in programs if not p["inScope"]),
    "copybooks": len(copybooks), "ddAllocations": len(datasets), "datasets": len(streamDsns),
    "edges": len(edges), "edgesProven": count(edges, "status", "PROVEN"),
    "edgesCandidate": count(edges, "status", "CANDIDATE"),
    "callEdges": count(edges, "kind", "call"), "copyEdges": count(edges, "kind", "copy"),
    "dataEdges": count(edges, "kind", "data"),
    "setupJobs": len(setup), "setupSteps": sum(len(s["steps"]) for s in setup),
    "outside": len(outside), "outsideProven": count(outside, "status", "PROVEN"),
    "outsideCandidate": count(outside, "status", "CANDIDATE"),
    "outsideBatch": count(outside, "actorKind", "batch job step"),
    "outsideCicsPrograms": count(outside, "actorKind", "CICS program"),
    "outsideCicsTransactions": count(outside, "actorKind", "CICS transaction"),
    "outsideJobs": len({r["actor"].split(".")[0] for r in outside if r["actorKind"] == "batch job step"}),
    "outsidePrograms": len({r["actor"] for r in outside if r["actorKind"] == "CICS program"}),
    "outsideTransactions": len({r["actor"] for r in outside if r["actorKind"] == "CICS transaction"}),
    "predecessors": len(scheduler["predecessors"]), "successors": len(scheduler["successors"]),
    "unprovenOrderings": len(scheduler["unproven"]), "disagreements": len(views["disagreements"]),
    "openAsks": len(asks),
}
collect(manifest)
manifest["excerpts"] = dict(sorted(excerpts.items()))
manifest["counts"]["citations"] = len(excerpts)

if citeErrors:
    print("CITATION ERRORS:", *citeErrors, sep="\n  ")
    sys.exit(1)
(outDir / "manifest.json").write_text(json.dumps(manifest, indent=1) + "\n")
print(json.dumps(manifest["counts"], indent=1))
