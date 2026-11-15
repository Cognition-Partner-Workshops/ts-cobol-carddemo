#!/usr/bin/env python3
"""Whole-estate call graph from the mftb stores (store/<MODULE>/*.json + store/catalog.json).

Reads scan results only; writes modernization/legacy/call-graph.json.
PROVEN = scanner certainty 'proven' (static, cited). CANDIDATE = 'candidate' or 'derived'
(dynamic target narrowed by the scan, or inferred). OPEN = target not settled: an open ask.
Run from the repository root: python3 modernization/legacy/tools/build_graph.py
"""
import json, re, os, collections, subprocess

BASE_SHA = "a5da4ee6bb4261392ac0a44a0756b82914b97fe8"
MODS = [m["id"] for m in json.load(open("store/modules.json"))["modules"]]
CAT = json.load(open("store/catalog.json"))
ST = {m: {f: json.load(open(f"store/{m}/{f}.json")) for f in
          ("programs", "traces", "connections", "jobs", "schedule") if os.path.exists(f"store/{m}/{f}.json")} for m in MODS}
STATUS = {"proven": "PROVEN", "candidate": "CANDIDATE", "derived": "CANDIDATE", "open": "OPEN"}

# ---- nodes: programs per module, leaf members from the catalogue
prog = {}
for m in MODS:
    for p in ST[m]["programs"]["programs"]:
        if p["kind"] == "program":
            prog[p["id"]] = {"id": p["id"], "module": m, "path": p["path"], "lang": "COBOL", "leaf": False}
for e in CAT["entries"]:
    if e["kind"] in ("assembler-program", "unsupported-source"):
        for d in e["declares"]:
            prog[d["name"]] = {"id": d["name"], "module": "CARDDEMO", "path": e["path"],
                               "lang": "HLASM" if e["kind"] == "assembler-program" else "PL/I",
                               "leaf": True, "decl": d["evidence"]}
bclass = {}
for m in MODS:
    for n in ST[m]["traces"]["nodes"]:
        if n["kind"] == "boundary":
            bclass.setdefault(n["id"], n["class"])

def tkind(t):
    if t in prog:
        return prog[t]["lang"] + (" leaf" if prog[t]["leaf"] else " program")
    c = bclass.get(t)
    return {"runtime-routine": "runtime routine", "mq": "MQ API stub", "ims-interface": "IMS interface",
            "utility": "utility", "launcher": "launcher"}.get(c, "not in the estate")

edges = []
def add(**k):
    k.setdefault("note", ""); k.setdefault("dir", ""); k.setdefault("xmod", False)
    edges.append(k)

# ---- A. program to program
REL = {"static-call": "CALL", "cics-xctl": "XCTL", "cics-link": "LINK", "dynamic-call": "CALL (dynamic)"}
agg = collections.OrderedDict()
for m in MODS:
    for e in ST[m]["traces"]["edges"]:
        if e["kind"] not in REL:
            continue
        st = STATUS[e["certainty"]]
        to = e["to"]
        if st == "OPEN":
            to = "(not settled: %s)" % e.get("variable", to.lstrip("?"))
        key = (e["from"], to, REL[e["kind"]], st)
        a = agg.setdefault(key, {"cites": [], "detail": e["detail"], "mod": m})
        if e["source"] not in a["cites"]:
            a["cites"].append(e["source"])
for (f, t, rel, st), a in agg.items():
    tm = prog.get(t, {}).get("module")
    add(type="call", frm=f, to=t, rel=rel, status=st, cite=a["cites"], mod=a["mod"], tomod=tm or "",
        tkind=tkind(t) if st != "OPEN" else "open ask", xmod=bool(tm and tm != a["mod"]), note=a["detail"])

# ---- B. job to step to program
jobs = {}
for m in MODS:
    for j in ST[m]["jobs"]["jobs"]:
        jobs[j["name"]] = {"name": j["name"], "module": m, "path": j["path"], "steps": []}
        for s in j["steps"]:
            run = s.get("program") or s.get("pgm") or ""
            jobs[j["name"]]["steps"].append(s["step"])
            add(type="job", frm=j["name"], to="%s.%s" % (j["name"], s["step"]), rel="has step", status="PROVEN",
                cite=[s["source"]], mod=m)
            if s.get("proc"):
                add(type="job", frm="%s.%s" % (j["name"], s["step"]), to=s["proc"], rel="EXEC PROC", status="PROVEN",
                    cite=[s["source"]], mod=m, note="proc step %s" % (s.get("proc_step") or "-"))
            if s.get("pgm"):
                add(type="job", frm="%s.%s" % (j["name"], s["step"]), to=s["pgm"], rel="EXEC PGM", status="PROVEN",
                    cite=[s["source"]], mod=m, tkind=tkind(s["pgm"]) if s["pgm"] in prog or s["pgm"] in bclass else s.get("kind", ""),
                    note=("PARM=%s" % s["parm"]) if s.get("parm") else "")
            if s.get("program") and s["program"] != s.get("pgm"):
                add(type="job", frm="%s.%s" % (j["name"], s["step"]), to=s["program"], rel="runs (under %s)" % s.get("pgm"),
                    status="PROVEN", cite=[s["source"]], mod=m, tkind=tkind(s["program"]),
                    note="resolution: %s" % s.get("resolution"))

# ---- C. program to data
def dirof(modes, ops):
    w = {"OUTPUT", "EXTEND"} & set(modes); io = "I-O" in modes
    if io or ({"REWRITE", "DELETE"} & set(ops)):
        return "updates"
    if w and "INPUT" in modes:
        return "reads+writes"
    return "writes" if w else "reads"
for m in MODS:
    C = ST[m]["connections"]
    for f in C["files"]:
        d = dirof(f.get("open_modes") or [], f.get("ops") or {})
        if f.get("bindings"):
            for b in f["bindings"]:
                add(type="data", frm=f["program"], to=b.get("dsn") or "DD %s (%s)" % (f["dd"], b.get("kind")), rel="dataset", dir=d,
                    status="PROVEN", cite=[f["source"], b["source"]], mod=m, store="dataset",
                    note="DD %s in %s.%s, %s" % (f["dd"], b["job"], b["step"], f["organization"]))
        else:
            add(type="data", frm=f["program"], to="DD %s (dataset not bound by any scanned JCL)" % f["dd"], rel="dataset", dir=d,
                status="OPEN", cite=[f["source"]], mod=m, store="dataset", note="binding: %s" % f.get("binding_status"))
    for ds in C["datasets"]:
        for s in ds["steps"]:
            if s.get("step_kind") != "program":
                add(type="data", frm="%s.%s (%s)" % (s["job"], s["step"], s.get("program")), to=ds["dsn"], rel="dataset",
                    dir="DISP=%s" % (s.get("disp") or "-"), status="PROVEN", cite=[s["source"]], mod=m, store="dataset",
                    note="utility step, DD %s; %s" % (s["dd"], ds["type"]))
    cx = C["cics"]
    RD = {"READ", "STARTBR", "READNEXT", "READPREV", "ENDBR", "RESETBR"}
    def cics(items, namekey, store, dirfn):
        g = collections.OrderedDict()
        for it in items:
            for u in it["uses"]:
                k = (u["program"], it[namekey], dirfn(u["command"]), "PROVEN" if u.get("resolution") in ("literal", "constant") else "CANDIDATE")
                g.setdefault(k, {"c": [], "cmd": set()}); g[k]["c"].append(u["source"]); g[k]["cmd"].add(u["command"])
        for (p, n, d, st), v in g.items():
            if not n:
                n, st = "(%s name not settled in the source)" % store, "OPEN"
            add(type="data", frm=p, to=n, rel=store, dir=d, status=st, cite=v["c"], mod=m, store=store,
                note="EXEC CICS " + ", ".join(sorted(v["cmd"])))
    cics(cx["files"], "name", "CICS file", lambda c: "reads" if c in RD else ("writes" if c == "WRITE" else "updates"))
    cics(cx["td_queues"], "name", "TD queue", lambda c: "writes" if "WRITE" in c else "reads")
    cics(cx["ts_queues"], "name", "TS queue", lambda c: "writes" if "WRITE" in c else ("reads" if "READ" in c else "updates"))
    cics(cx["maps"], "mapset", "map", lambda c: "writes (SEND)" if "SEND" in c else "reads (RECEIVE)")
    for it in cx["files"]:
        cs = it.get("csd") or {}
        dsn = (cs.get("attrs") or {}).get("DSNAME")
        if dsn:
            add(type="data", frm=it["name"], to=dsn, rel="CSD FILE to dataset", dir="defined as", status="PROVEN",
                cite=[cs["source"]], mod=m, store="dataset")
    for s in cx.get("submits", []):
        add(type="data", frm=s["program"], to="job %s (via TD queue %s, %s)" % (s.get("job"), s["queue"], s.get("internal_reader_ddname")),
            rel="submits job", dir="writes", status="PROVEN" if s.get("status") == "complete" else "CANDIDATE",
            cite=[s["source"], s["csd_source"]], mod=m, store="TD queue",
            note="scan status %s; executes %s" % (s.get("status"), ", ".join(x["name"] for x in s.get("executes", []))))
    for t in C["db2"]["tables"]:
        for p in t["programs"]:
            ops = set(p["ops"]) - {"DCLGEN", "INCLUDE"}
            if not ops:
                continue
            d = "updates" if ops & {"UPDATE", "DELETE"} else ("writes" if "INSERT" in ops else "reads")
            if d != "reads" and ops & {"SELECT", "FETCH", "OPEN", "DECLARE"}:
                d = "reads+" + d
            if "INSERT" in ops and "updates" in d:
                d = d.replace("updates", "writes+updates")
            add(type="data", frm=p["program"], to=t["name"], rel="Db2 table", dir=d, status="PROVEN", cite=p["sources"], mod=m,
                store="Db2 table", note="SQL " + ", ".join(sorted(ops)))
    g = collections.OrderedDict()
    for p in C["ims"]["programs"]:
        for c in p["commands"]:
            if not c.get("segment"):
                continue
            d = "reads" if c["command"][:2] in ("GU", "GN", "GH") else ("writes" if c["command"] == "ISRT" else "updates")
            k = (p["program"], c["segment"], d); g.setdefault(k, {"c": [], "cmd": set()})
            g[k]["c"].append(c["source"]); g[k]["cmd"].add(c["command"])
    for (p, sgm, d), v in g.items():
        add(type="data", frm=p, to="IMS segment " + sgm, rel="IMS segment", dir=d, status="PROVEN", cite=v["c"], mod=m,
            store="IMS segment", note="DL/I " + ", ".join(sorted(v["cmd"])))
    g = collections.OrderedDict()
    for q in C["external"]["mq"]:
        d = {"MQGET": "reads", "MQPUT": "writes", "MQPUT1": "writes"}.get(q["api"])
        if d:
            g.setdefault((q["program"], q["api"], d), []).append(q["source"])
    for (p, api, d), c in g.items():
        add(type="data", frm=p, to="MQ queue (name not captured by the scan)", rel="MQ queue", dir=d, status="OPEN", cite=c, mod=m,
            store="MQ queue", note="CALL '%s' is static and cited; which queue it addresses is an open ask" % api)

# ---- D. copybooks
cb = collections.OrderedDict()
for m in MODS:
    for p in ST[m]["programs"]["programs"]:
        if p["kind"] != "program":
            continue
        for inc in p["includes"]:
            cb.setdefault(inc["name"], []).append({"program": p["id"], "module": m, "cite": "%s:%s" % (p["path"], inc["line"]),
                                                    "mech": inc.get("mechanism")})
cbpath = {}
for e in CAT["entries"]:
    if e["path"].lower().endswith((".cpy", ".dcl")) or e["kind"] == "cobol-copybook":
        cbpath.setdefault(e["member"].upper(), e["path"])
copybooks = []
for name, us in sorted(cb.items()):
    progs = sorted({u["program"] for u in us}); mods = sorted({u["module"] for u in us})
    copybooks.append({"name": name, "path": cbpath.get(name.upper(), ""), "programs": progs, "modules": mods, "uses": us,
                      "shared": len(progs) > 1, "xmod": len(mods) > 1})
    for u in us:
        add(type="copybook", frm=u["program"], to=name, rel="COPY" if u["mech"] == "copy" else "INCLUDE", status="PROVEN",
            cite=[u["cite"]], mod=u["module"], xmod=len(mods) > 1,
            note="" if cbpath.get(name.upper()) else "copybook member not in the estate")

# ---- E. transactions
trans = []
for m in MODS:
    for t in ST[m]["connections"]["cics"]["transactions"]:
        cs = t.get("csd")
        if cs and cs.get("program"):
            p = cs["program"]
            trans.append({"transid": t["transid"], "program": p, "module": m, "cite": cs["source"], "group": cs.get("group"),
                          "progmod": prog.get(p, {}).get("module", "")})
            add(type="tran", frm=t["transid"], to=p, rel="initial program", status="PROVEN", cite=[cs["source"]], mod=m,
                tomod=prog.get(p, {}).get("module", ""), tkind=tkind(p),
                xmod=bool(prog.get(p) and prog[p]["module"] != m), note="CSD group %s" % cs.get("group"))
        else:
            add(type="tran", frm=t["transid"], to="(no CSD definition in the estate)", rel="initial program", status="OPEN",
                cite=[u["source"] for u in t["uses"]][:3], mod=m, note="transaction id used by %s" % ", ".join(sorted({u["program"] for u in t["uses"]})))

# ---- F. scheduler (same export in every store; take the first)
SCH = next(ST[m]["schedule"] for m in MODS if "schedule" in ST[m])
jline = {}
for m in MODS:
    for e in ST[m]["traces"]["edges"]:
        if e["kind"] == "schedule-job":
            jline[(e["from"].replace("TABLE ", ""), e["job"])] = e["source"]
jclmem = {}
for e in CAT["entries"]:
    if e["kind"] == "jcl-job":
        jclmem[e["member"].upper()] = e["path"]
        for d in e["declares"]:
            jclmem.setdefault(d["name"].upper(), e["path"])
sched = []; scheduled = set()
for t in SCH["tables"]:
    T = {"table": t["name"], "dialect": t["dialect"], "path": t["path"], "jobs": [], "order": []}
    for j in t["jobs"]:
        mem = j.get("memname") or j["name"]; scheduled.add(mem.upper())
        path = jclmem.get(mem.upper())
        cite = jline.get((t["name"], j["name"]), t["path"])
        T["jobs"].append({"job": j["name"], "jcl": path or "", "cite": cite})
        add(type="sched", frm="table " + t["name"], to=j["name"], rel="schedules job", status="PROVEN", cite=[cite], mod="shared",
            note="JCL member " + path if path else "")
        if path:
            add(type="sched", frm=j["name"], to=path, rel="job to JCL member", status="CANDIDATE", cite=[cite, path], mod="shared",
                note="matched by member name across modules; the export names no library")
        else:
            add(type="sched", frm=j["name"], to="(no JCL member of this name in the estate)", rel="job to JCL member", status="OPEN",
                cite=[cite], mod="shared")
    for e in t["edges"]:
        T["order"].append([e["from"], e["to"], e["condition"]])
        add(type="sched", frm=e["from"], to=e["to"], rel="runs before", status="PROVEN", cite=[t["path"]], mod="shared",
            note="table %s, condition %s" % (t["name"], e["condition"]))
    sched.append(T)
for x in SCH["cross_table"]:
    add(type="sched", frm="%s (table %s)" % (", ".join(x["from_jobs"]), x["from_table"]), to="%s (table %s)" % (x["to_job"], x["to_table"]),
        rel="runs before (across tables)", status="PROVEN", cite=["app/scheduler/CardDemo.controlm"], mod="shared", note="condition " + x["condition"])
unscheduled = sorted((n, j["module"], j["path"]) for n, j in jobs.items()
                     if n.upper() not in scheduled and os.path.splitext(os.path.basename(j["path"]))[0].upper() not in scheduled)

# ---- G. uncalled programs, unreferenced members
called = collections.defaultdict(list)
for e in edges:
    if e["type"] == "call" and e["status"] != "OPEN" and e["frm"] != e["to"]:
        called[e["to"]].append("%s %s (%s)" % (e["frm"], e["rel"], e["status"]))
    if e["type"] == "job" and e["rel"] != "has step":
        called[e["to"]].append("step " + e["frm"])
    if e["type"] == "tran" and e["status"] == "PROVEN":
        called[e["to"]].append("transaction " + e["frm"])
uncalled = [{"program": p, "module": v["module"], "path": v["path"], "lang": v["lang"]} for p, v in sorted(prog.items()) if not called.get(p)]
entered = {p: sorted(set(called[p])) for p in prog if called.get(p)}

refnames = collections.Counter()
for e in CAT["entries"]:
    for r in e["references"]:
        refnames[r["name"].upper()] += 1
for c in copybooks:
    refnames[c["name"].upper()] += 1
alldsn = set()
for m in MODS:
    for ds in ST[m]["connections"]["datasets"]:
        alldsn.add(ds["dsn"].upper())
    for j in ST[m]["jobs"]["jobs"]:
        for s in j["steps"]:
            for d in s["dds"]:
                if d.get("dsn"):
                    alldsn.add(d["dsn"].upper())
SKIP = {"document", "empty", "cobol-program", "jcl-job", "scheduler-table", "cics-csd", "assembler-program", "unsupported-source"}
unref = []
for e in CAT["entries"]:
    if e["kind"] in SKIP:
        continue
    names = {e["member"].upper()} | {d["name"].upper() for d in e["declares"]}
    base = os.path.basename(e["path"]).upper()
    if any(refnames.get(n) for n in names) or base in alldsn or any(("(%s)" % n) in d for n in names for d in alldsn):
        continue
    word = e["member"] if e["kind"] != "binary" else os.path.basename(e["path"])
    try:
        out = subprocess.run(["grep", "-rlIwi", "--", word, "app"], capture_output=True, text=True).stdout.split()
    except Exception:
        out = []
    out = [o for o in out if o != e["path"]]
    unref.append({"path": e["path"], "kind": e["kind"], "mentions": len(out), "where": out[:4]})

# ---- H. docs/chain-graph.md
dis = []
cg = open("docs/chain-graph.md").read()
sec = cg.split("## Programs")[1].split("\n## ")[0]
cgp = {}
for line in sec.splitlines():
    c = [x.strip() for x in line.strip().strip("|").split("|")]
    mm = re.match(r"\[(\w+)\]\(([^)]+)\)", c[0]) if c else None
    if mm and len(c) >= 8:
        sp = lambda x: set() if x in ("-", "") else {y.strip() for y in x.split(",")}
        cgp[mm.group(1)] = {"path": mm.group(2), "cpy": sp(c[2]), "dd": sp(c[3]), "static": sp(c[4]), "dyn": sp(c[5]), "sql": sp(c[6]), "notes": c[7]}
for p in sorted(set(prog) - set(cgp)):
    dis.append({"what": "program missing from docs/chain-graph.md", "item": p, "scan": "%s (%s, module %s)" % (prog[p]["path"], prog[p]["lang"], prog[p]["module"]), "doc": "not listed"})
for p in sorted(set(cgp) - set(prog)):
    dis.append({"what": "program in docs/chain-graph.md but not in the scan", "item": p, "scan": "not found", "doc": cgp[p]["path"]})
pinfo = {}
for m in MODS:
    for p in ST[m]["programs"]["programs"]:
        if p["kind"] == "program":
            pinfo[p["id"]] = p
for p in sorted(set(cgp) & set(pinfo)):
    s = pinfo[p]; d = cgp[p]
    scpy = {i["name"] for i in s["includes"]}; sst = {c["target"] for c in s["calls"]["static"]}
    sdd = {f["assign"] for f in s["files"]}; sdyn = {c.get("variable") or c.get("target") for c in s["calls"]["dynamic"]}
    for label, a, b in (("copybooks", scpy, d["cpy"]), ("static CALLs", sst, d["static"]), ("DD names", sdd, d["dd"]), ("dynamic CALLs", sdyn, d["dyn"])):
        if a != b:
            dis.append({"what": label + " differ", "item": p, "scan": "only in scan: %s" % (", ".join(sorted(x for x in a - b if x)) or "-"),
                        "doc": "only in doc: %s" % (", ".join(sorted(b - a)) or "-")})
jsec = cg.split("## Jobs")[1].split("\n## ")[0]
cgj = collections.defaultdict(set); cgrun = collections.defaultdict(set)
member = {os.path.splitext(os.path.basename(j["path"]))[0]: n for n, j in jobs.items()}
for line in jsec.splitlines():
    c = [x.strip() for x in line.strip().strip("|").split("|")]
    mm = re.match(r"\[(\w+)\]", c[0]) if c else None
    if mm and len(c) >= 3:
        cgj[mm.group(1)].add((c[1].split(".")[0], c[2].split(" -> ")[0]))
        if " -> " in c[2]:
            cgrun[mm.group(1)].add((c[1].split(".")[0], c[2].split(" -> ")[1]))
cgj = {member.get(k, k): v for k, v in cgj.items()}; cgrun = {member.get(k, k): v for k, v in cgrun.items()}
for j in sorted(set(jobs) - set(cgj)):
    dis.append({"what": "job missing from docs/chain-graph.md", "item": j, "scan": jobs[j]["path"], "doc": "not listed"})
srun = collections.defaultdict(set)
for e in edges:
    if e["type"] == "job" and e["rel"].startswith("runs (under"):
        srun[e["frm"].split(".")[0]].add((e["frm"].split(".", 1)[1], e["to"]))
for j in sorted(cgrun):
    if cgrun[j] != srun.get(j, set()):
        dis.append({"what": "program run under a launcher differs", "item": j,
                    "scan": "scan resolves: %s" % (", ".join("%s=%s" % x for x in sorted(srun.get(j, set()))) or "nothing"),
                    "doc": "doc resolves: %s" % ", ".join("%s=%s" % x for x in sorted(cgrun[j]))})
for j in sorted(set(cgj) - set(jobs)):
    dis.append({"what": "job in docs/chain-graph.md but not in the scan", "item": j, "scan": "no job of this name", "doc": "listed"})
sj = collections.defaultdict(set)
for e in edges:
    if e["type"] == "job" and e["rel"] == "EXEC PGM":
        sj[e["frm"].split(".")[0]].add((e["frm"].split(".", 1)[1], e["to"]))
for j in sorted(set(cgj) & set(sj)):
    if cgj[j] != sj[j]:
        dis.append({"what": "job steps differ", "item": j, "scan": "only in scan: %s" % (", ".join("%s=%s" % x for x in sorted(sj[j] - cgj[j])) or "-"),
                    "doc": "only in doc: %s" % (", ".join("%s=%s" % x for x in sorted(cgj[j] - sj[j])) or "-")})

for i, e in enumerate(edges):
    e["id"] = i + 1
cnt = collections.Counter((e["type"], e["status"]) for e in edges)
data = {"baseline": BASE_SHA, "tool": CAT["meta"]["tool"], "modules": MODS,
        "programs": sorted(prog.values(), key=lambda p: (p["module"], p["id"])), "edges": edges, "copybooks": copybooks,
        "transactions": trans, "scheduler": sched, "unscheduled": unscheduled, "uncalled": uncalled, "entered": entered,
        "unreferenced": unref, "disagreements": dis, "jobs": sorted(jobs.values(), key=lambda j: j["name"]),
        "counts": {"%s/%s" % k: v for k, v in sorted(cnt.items())}}
json.dump(data, open("modernization/legacy/call-graph.json", "w"), indent=1, default=list)
print(json.dumps(data["counts"], indent=0))
print("programs", len(prog), "jobs", len(jobs), "copybooks", len(copybooks), "shared", sum(c["shared"] for c in copybooks),
      "trans", len(trans), "unscheduled", len(unscheduled), "uncalled", len(uncalled), "unref", len(unref), "disagree", len(dis))
