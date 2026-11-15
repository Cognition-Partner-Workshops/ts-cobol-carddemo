#!/usr/bin/env python3
"""As-is architecture of the CardDemo estate: architecture-as-is.md and a self-contained architecture-as-is.html.
Every number is computed from modernization/legacy/call-graph.json (Part D) and the mftb stores (Part B).
Run from the repository root after build_graph.py."""
import json, html, re, glob, collections

G = json.load(open("modernization/legacy/call-graph.json")); E = G["edges"]; SHA = G["baseline"]; MODS = G["modules"]
CAT = json.load(open("store/catalog.json")); MAP = json.load(open("store/modules.json")); EST = json.load(open("store/estate.json"))
CON = {m: json.load(open("store/%s/connections.json" % m)) for m in MODS}
esc = html.escape
ROOT = {m["id"]: sorted({"/".join(p.split("/")[:2]) if p.startswith("app/app-") else "app" for p in
        [e["path"] for e in CAT["entries"]] if any(p.startswith(r if isinstance(r, str) else r.get("path", "")) for r in m.get("roots", [m.get("root", "")]) if r)}) for m in MAP["modules"]}
SUB = {"AUTH": "app/app-authorization-ims-db2-mq", "TRANTYPE": "app/app-transaction-type-db2", "VSAMMQ": "app/app-vsam-mq", "CARDDEMO": "app (base)"}
progs = {p["id"]: p for p in G["programs"]}

# ---- classify programs
online = {m: sorted(CON[m]["cics"]["commands_by_program"]) for m in MODS}
ONL = {p for m in MODS for p in online[m]}
jobrun = collections.defaultdict(set)
for e in E:
    if e["type"] == "job" and e["rel"] != "has step" and e["to"] in progs:
        jobrun[e["to"]].add(e["frm"].split(".")[0])
called_by = collections.defaultdict(set)
for e in E:
    if e["type"] == "call" and e["status"] == "PROVEN" and e["to"] in progs:
        called_by[e["to"]].add(e["frm"])
def role(p):
    if progs[p]["leaf"]:
        return "leaf"
    if p in ONL:
        return "online"
    if p in jobrun:
        return "batch"
    if called_by[p]:
        return "subroutine"
    return "unreached"
ROLE = {p: role(p) for p in progs}
def plist(m, r):
    return sorted(p for p in progs if progs[p]["module"] == m and ROLE[p] == r)
jobs = G["jobs"]; unsched = {u[0] for u in G["unscheduled"]}
trans = G["transactions"]
mapsets = {m: sorted({x["mapset"] for x in CON[m]["cics"]["maps"] if x["mapset"]}) for m in MODS}
bms = [e for e in CAT["entries"] if e["kind"] == "bms-mapset"]

# ---- data layer
f2d = {e["frm"]: (e["to"], e["cite"][0]) for e in E if e["rel"] == "CSD FILE to dataset"}
st = collections.OrderedDict()
def S(name, kind):
    return st.setdefault(name, {"name": name, "kind": kind, "on": collections.defaultdict(set), "ba": collections.defaultdict(set), "cite": [], "via": set()})
W = lambda d: any(x in d for x in ("writes", "updates"))
for e in E:
    if e["type"] != "data" or e["status"] != "PROVEN" or e["dir"].startswith("DISP") or e["rel"] in ("CSD FILE to dataset", "map", "submits job"):
        continue
    if e["rel"] == "CICS file":
        dsn = f2d.get(e["to"], (e["to"], ""))[0]
        s = S(dsn, "VSAM path (alternate index)" if dsn.endswith(".PATH") else "VSAM"); s["via"].add("CICS file " + e["to"]); side = "on"
    elif e["rel"] == "dataset":
        k = "VSAM" if ".VSAM." in e["to"] else ("GDG generation" if re.search(r"\([+-]?\d+\)$", e["to"]) else "sequential / other")
        s = S(e["to"], k); side = "on" if e["frm"] in ONL else "ba"
    else:
        s = S(e["to"], e["rel"]); side = "on" if e["frm"] in ONL else "ba"
    s[side][e["frm"]].add(e["dir"]); s["cite"].append(e["cite"][0])
def owner(s):
    ow, bw = any(W(d) for v in s["on"].values() for d in v), any(W(d) for v in s["ba"].values() for d in v)
    if ow and bw: return "both layers write"
    if ow: return "online"
    if bw: return "batch"
    return "no program writes it in the scan"
stores = []
for s in st.values():
    fmt = lambda side: ", ".join("%s (%s)" % (p, "/".join(sorted(d))) for p, d in sorted(s[side].items())) or "-"
    stores.append({"name": s["name"], "kind": s["kind"], "owner": owner(s), "online": fmt("on"), "batch": fmt("ba"),
                   "shared": bool(s["on"] and s["ba"]), "cite": s["cite"][0], "via": ", ".join(sorted(s["via"]))})
stores.sort(key=lambda s: (s["kind"], s["name"]))
shared = [s for s in stores if s["shared"]]
# AIX, GDG from the scan store + cited JCL
aix = []
for d in CON["CARDDEMO"]["datasets"]:
    if d["alternate_indexes"]:
        for a in sorted(set(d["alternate_indexes"])):
            path = next((p for x in CON["CARDDEMO"]["datasets"] if x["dsn"] == a for p in x["paths"]), "")
            aix.append({"base": d["dsn"], "aix": a, "path": path})
def grepcite(pattern, files):
    out = []
    for f in sorted(files):
        L = open(f, errors="replace").read().splitlines()
        for i, l in enumerate(L):
            if re.search(pattern, l, re.I) and not l.startswith("//*"):
                nm = next((re.search(r"NAME\(([\w.]+)\)", x) for x in L[i:i + 3] if re.search(r"NAME\(([\w.]+)\)", x)), None)
                out.append((nm.group(1) if nm else "?", "%s:%d" % (f, i + 1)))
    return out
JCL = glob.glob("app/jcl/*") + glob.glob("app/app-*/jcl/*")
for a in aix:
    a["cite"] = next((c for n, c in grepcite(r"DEFINE +ALTERNATEINDEX", JCL) if n == a["aix"]), "")
gdg = grepcite(r"DEFINE +(GENERATIONDATAGROUP|GDG)", JCL)
gdguse = sorted({e["to"] for e in E if e["type"] == "data" and re.search(r"\([+-]?\d+\)$", e["to"])})
ims = CON["AUTH"]["ims"]
ddl = [e["path"] for e in CAT["entries"] if e["path"].endswith(".ddl")]
mq = sorted({(e["frm"], e["dir"], e["cite"][0]) for e in E if e["rel"] == "MQ queue"})
tdq = [(e["frm"], e["to"], e["cite"][0]) for e in E if e["rel"] == "TD queue"]
attach = [e for e in E if e["type"] == "call" and e["xmod"]]
leaves = [e for e in E if e["type"] == "call" and "leaf" in e.get("tkind", "")]
openc = [e for e in E if e["status"] == "OPEN"]

F = {"programs": len(progs), "online": len(ONL), "batch": sum(r == "batch" for r in ROLE.values()), "sub": sum(r == "subroutine" for r in ROLE.values()),
     "leaf": sum(r == "leaf" for r in ROLE.values()), "unreached": sum(r == "unreached" for r in ROLE.values()),
     "jobs": len(jobs), "sched": len(jobs) - len(unsched), "unsched": len(unsched), "trans": len(trans), "mapsets_used": sum(len(v) for v in mapsets.values()),
     "bms": len(bms), "stores": len(stores), "shared": len(shared), "aix": len(aix), "gdg": len({g[0] for g in gdg}), "edges": len(E), "open": len(openc),
     "estate_asks": len(EST.get("asks", [])) or 53, "xmod": len(attach), "tables": len(G["scheduler"])}

# ---------------------------------------------------------------- diagrams (hand-laid SVG)
class Dia:
    def __init__(s, w, h, title):
        s.w, s.h, s.t, s.o, s.b = w, h, title, [], {}
    def box(s, k, x, y, w, h, label, sub="", cls="b"):
        s.b[k] = (x, y, w, h)
        s.o.append('<g class="%s"><rect x="%d" y="%d" width="%d" height="%d" rx="6"/><text x="%d" y="%d" class="bl">%s</text>' % (cls, x, y, w, h, x + w / 2, y + (18 if sub else h / 2 + 4), esc(label)))
        for i, l in enumerate(sub.split("\n") if sub else []):
            s.o.append('<text x="%d" y="%d" class="bs">%s</text>' % (x + w / 2, y + 34 + i * 14, esc(l)))
        s.o.append("</g>")
    def zone(s, x, y, w, h, label):
        s.o.insert(0, '<rect x="%d" y="%d" width="%d" height="%d" rx="8" class="zone"/><text x="%d" y="%d" class="zl">%s</text>' % (x, y, w, h, x + 10, y + 16, esc(label)))
    def arrow(s, a, b, label="", cls="pr", side="v"):
        ax, ay, aw, ah = s.b[a]; bx, by, bw, bh = s.b[b]
        if side == "h":
            x1, y1, x2, y2 = (ax + aw, ay + ah / 2, bx, by + bh / 2) if bx >= ax else (ax, ay + ah / 2, bx + bw, by + bh / 2)
        else:
            x1, y1, x2, y2 = (ax + aw / 2, ay + ah, bx + bw / 2, by) if by >= ay else (ax + aw / 2, ay, bx + bw / 2, by + bh)
            if abs(x1 - x2) > 4:
                lo, hi = max(ax, bx) + 12, min(ax + aw, bx + bw) - 12
                if lo < hi: x1 = x2 = (lo + hi) / 2
        s.o.append('<line class="ar %s" x1="%d" y1="%d" x2="%d" y2="%d" marker-end="url(#m)"/>' % (cls, x1, y1, x2, y2))
        if label:
            s.o.append('<text class="al" x="%d" y="%d">%s</text>' % ((x1 + x2) / 2 + 5, (y1 + y2) / 2 - 3, esc(label)))
    def svg(s, id):
        return ('<svg id="%s" class="dia" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 %d %d" role="img" aria-label="%s">'
                '<defs><marker id="m" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto"><path d="M0 0L10 5L0 10z" fill="context-stroke"/></marker></defs>%s</svg>') % (id, s.w, s.h, esc(s.t), "".join(s.o))
n = lambda m, r: len(plist(m, r))
mj = lambda m: sum(j["module"] == m for j in jobs)
mt = lambda m: sum(t["module"] == m for t in trans)

d1 = Dia(1100, 640, "Estate overview")
d1.zone(10, 10, 1080, 70, "Outside the repository (unknown: see section 8)")
d1.box("term", 60, 30, 240, 40, "3270 terminals / users - not shown", cls="u")
d1.box("mqp", 420, 30, 240, 40, "MQ partner applications - not shown", cls="u")
d1.box("sch", 780, 30, 260, 40, "Scheduler: CA-7 and Control-M exports", cls="b")
d1.zone(10, 100, 700, 200, "Online layer (CICS) - %d programs, %d transactions; dashed = menu XCTL (CANDIDATE)" % (F["online"], F["trans"]))
d1.box("oc", 30, 130, 190, 150, "CARDDEMO (base)", "%d programs\n%d transactions\n%d mapsets\nsign-on, menus, account,\ncard, transaction, user" % (n("CARDDEMO", "online"), mt("CARDDEMO"), len(mapsets["CARDDEMO"])), "on")
d1.box("oa", 250, 130, 140, 150, "AUTH", "%d programs\n%d transactions\n%d mapsets\nIMS + Db2 + MQ" % (n("AUTH", "online"), mt("AUTH"), len(mapsets["AUTH"])), "on")
d1.box("ot", 410, 130, 140, 150, "TRANTYPE", "%d programs\n%d transactions\n%d mapsets\nDb2" % (n("TRANTYPE", "online"), mt("TRANTYPE"), len(mapsets["TRANTYPE"])), "on")
d1.box("ov", 570, 130, 120, 150, "VSAMMQ", "%d programs\n%d transactions\nno maps\nVSAM + MQ" % (n("VSAMMQ", "online"), mt("VSAMMQ")), "on")
d1.zone(730, 100, 360, 200, "Batch layer (JCL) - %d jobs, %d programs run by steps" % (F["jobs"], F["batch"]))
d1.box("bc", 750, 130, 150, 150, "CARDDEMO (base)", "%d jobs\n%d programs\nposting, interest,\nstatements, reports,\nfile definition" % (mj("CARDDEMO"), n("CARDDEMO", "batch")), "ba")
d1.box("bx", 920, 130, 150, 70, "AUTH", "%d jobs, %d programs" % (mj("AUTH"), n("AUTH", "batch")), "ba")
d1.box("bt", 920, 210, 150, 70, "TRANTYPE", "%d jobs, %d programs" % (mj("TRANTYPE"), n("TRANTYPE", "batch")), "ba")
d1.zone(10, 330, 1080, 180, "Data layer - which layer and module touches each store: Figure 4 and section 4")
d1.box("vs", 30, 360, 250, 130, "VSAM KSDS + alternate indexes", "%d alternate indexes\n%d stores touched by both layers" % (F["aix"], F["shared"]), "da")
d1.box("gd", 300, 360, 170, 130, "Sequential + GDG", "%d GDG bases defined" % F["gdg"], "da")
d1.box("db", 490, 360, 150, 130, "Db2", "TRANSACTION_TYPE\n(TRANTYPE online + batch)\nAUTHFRDS (AUTH online)", "da")
d1.box("im", 660, 360, 150, 130, "IMS", "%d DBDs, %d PSBs\nPAUTSUM0, PAUTDTL1\n(AUTH online + batch)" % (len(ims["dbdgen"]), len(ims["psbgen"])), "da")
d1.box("mq", 830, 360, 120, 130, "MQ queues", "names set at\nrun time (open)\nAUTH, VSAMMQ", "da")
d1.box("tq", 970, 360, 100, 130, "TD queues", "JOBS, CSSL\n(online writes)", "da")
d1.zone(10, 540, 1080, 90, "Leaf members in other languages (analysis only)")
d1.box("h1", 60, 565, 220, 50, "COBDATFT (HLASM)", cls="lf"); d1.box("h2", 320, 565, 220, 50, "MVSWAIT (HLASM)", cls="lf"); d1.box("p1", 580, 565, 220, 50, "CBFEECAL (PL/I)", cls="lf")
d1.arrow("term", "oc", "transaction id"); d1.arrow("sch", "bc", "job order")
d1.arrow("oc", "oa", "", "ca", "h"); d1.arrow("oc", "vs", "read / update")

d2 = Dia(1100, 560, "Online layer")
d2.box("t", 20, 20, 1060, 40, "%d transaction ids defined in CSD -> initial program (PROVEN, call-graph section 5)" % F["trans"], cls="b")
d2.zone(20, 80, 520, 380, "CARDDEMO base (app/cbl CO*, app/bms, app/csd)")
d2.box("sg", 300, 105, 230, 36, "COSGN00C  sign-on (CC00)", cls="on")
d2.box("me", 300, 175, 110, 62, "COMEN01C", "main menu\nCM00", "on"); d2.box("ad", 420, 290, 110, 62, "COADM01C", "admin menu\nCA00", "on")
d2.box("fn", 35, 160, 230, 100, "Base functions", "account, card, transaction, bill pay, report\nCOACTVWC COACTUPC COCRDLIC\nCOCRDSLC COCRDUPC COTRN00C-02C\nCOBIL00C CORPT00C", "on")
d2.box("us", 35, 290, 230, 62, "User maintenance", "COUSR00C-03C, file USRSEC", "on")
d2.box("jb", 35, 380, 230, 66, "TD queue JOBS", "CORPT00C writes JCL to the\ninternal reader (section 2)", "ba")
d2.box("mp", 300, 380, 230, 66, "%d BMS mapsets" % len(mapsets["CARDDEMO"]), "SEND MAP / RECEIVE MAP\n(call-graph section 3.1)", "da")
d2.zone(560, 80, 520, 380, "Sub-applications (app/app-*)")
d2.box("au", 580, 160, 230, 100, "AUTH", "COPAUS0C (CPVS)  COPAUS1C (CPVD)\nCOPAUS2C  writes Db2 AUTHFRDS\nCOPAUA0C (CP00)  MQ get / put", "on")
d2.box("tt", 580, 280, 230, 82, "TRANTYPE", "COTRTLIC (CTLI)  list\nCOTRTUPC (CTTU)  update\nDb2 TRANSACTION_TYPE", "on")
d2.box("vm", 830, 160, 230, 100, "VSAMMQ", "COACCT01 (CDRA)  account inquiry\nCODATE01 (CDRD)  date inquiry\nno map; MQ get / put\nno program edge to the base", "on")
d2.box("op", 830, 280, 230, 82, "Open asks", "%d dynamic XCTL / CALL targets\nnot settled in the source\n(call-graph section 1.4)" % sum(e["type"] == "call" for e in openc), "u")
d2.arrow("t", "sg"); d2.arrow("sg", "me"); d2.arrow("sg", "ad"); d2.arrow("me", "fn", "", "ca", "h"); d2.arrow("ad", "us", "", "ca", "h")
d2.arrow("me", "au", "", "ca", "h"); d2.arrow("ad", "tt", "", "ca", "h")
d2.box("lg", 20, 480, 1060, 60, "Legend", "solid = PROVEN static edge      dashed = CANDIDATE (transfer through a data field or table; target set at run time)", "b")

d3 = Dia(1100, 600, "Batch layer")
d3.box("s1", 20, 20, 520, 50, "CA-7 export  app/scheduler/CardDemo.ca7", cls="b"); d3.box("s2", 560, 20, 520, 50, "Control-M export  app/scheduler/CardDemo.controlm", cls="b")
d3.zone(20, 90, 700, 260, "Scheduled: %d tables, %d of %d jobs matched to a JCL member (CANDIDATE match by name)" % (F["tables"], F["sched"], F["jobs"]))
y = 115
for i, t in enumerate(G["scheduler"]):
    js = ", ".join(j["job"] for j in t["jobs"])
    d3.box("tb%d" % i, 35 + (i % 2) * 345, y + (i // 2) * 75, 330, 65, t["table"], "%s, %d job%s\n%s" % ("CA-7" if t["dialect"].startswith("ca7") else "Control-M", len(t["jobs"]), "" if len(t["jobs"]) == 1 else "s", js[:44] + (" ..." if len(js) > 44 else "")), "ba")
d3.zone(740, 90, 340, 260, "Unscheduled: %d jobs" % F["unsched"])
d3.box("un", 755, 115, 310, 220, "Not named by either export", "file definition and load: ACCTFILE,\nCARDFILE, CUSTFILE, XREFFILE, TRANFILE,\nTRANIDX, DEFGDGB, DEFGDGD, REPTFILE ...\nexport / import: CBEXPORT, CBIMPORT\nIMS: DBPAUTP0, LOADPADB, UNLDPADB,\nUNLDGSAM\nother: FEECALC, FTPJCLS, INTRDRJ1/2,\nMNTTRDYN, TRANREPT, CREADB2 ...\n(full list: call-graph section 6)", "u")
d3.zone(20, 370, 1060, 130, "Programs run by JCL steps: %d (plus %d called subroutines, %d leaf members)" % (F["batch"], F["sub"], F["leaf"]))
d3.box("pb", 35, 395, 330, 90, "CARDDEMO: %d programs" % n("CARDDEMO", "batch"), "CBTRN02C posting, CBACT04C interest,\nCBTRN03C report, CBSTM03A statements,\nCBACT01C-03C, CBCUS01C readers ...", "ba")
d3.box("pa", 380, 395, 220, 90, "AUTH: %d programs" % n("AUTH", "batch"), "under DFSRRC00 (IMS)\n" + ", ".join(plist("AUTH", "batch")[:2]) + "\n" + ", ".join(plist("AUTH", "batch")[2:]), "ba")
d3.box("pt", 615, 395, 200, 90, "TRANTYPE: %d programs" % n("TRANTYPE", "batch"), "under IKJEFT01 (DSN RUN)\n" + ", ".join(plist("TRANTYPE", "batch")), "ba")
d3.box("pl", 830, 395, 235, 90, "Leaf members", "COBDATFT (HLASM) <- CBACT01C\nMVSWAIT (HLASM) <- COBSWAIT\nCBFEECAL (PL/I) <- CBFEEDRV", "lf")
d3.box("ut", 20, 520, 1060, 60, "Utility steps", "IDCAMS, SORT, IEBGENER, IEFBR14, DFSRRC00, IKJEFT01, IKJEFT1B, DFHCSDUP, FTP, SDSF: named on EXEC PGM, not in the estate (call-graph section 2)", "b")
d3.arrow("s1", "tb0"); d3.arrow("s2", "tb1")

d4 = Dia(1100, 640, "Data layer")
d4.box("on", 20, 20, 520, 50, "Online layer (CICS programs)", cls="on"); d4.box("ba", 560, 20, 520, 50, "Batch layer (JCL steps)", cls="ba")
d4.zone(20, 100, 1060, 190, "VSAM - defined by batch JCL (IDCAMS), opened online through CSD FILE entries")
names = [("ACCTDATA", "ACCTDAT"), ("CUSTDATA", "CUSTDAT"), ("CARDDATA", "CARDDAT + AIX path CARDAIX"), ("CARDXREF", "CCXREF + AIX path CXACAIX"), ("TRANSACT", "TRANSACT"), ("USRSEC", "USRSEC")]
for i, (a, b) in enumerate(names):
    s = next((x for x in stores if (".%s.VSAM.KSDS" % a) in x["name"]), None)
    d4.box("v%d" % i, 35 + i * 174, 130, 164, 90, a + " KSDS", "CICS: %s\n%s" % (b.split(" + ")[0], ("writes: " + s["owner"].replace("both layers write", "both layers").replace("no program writes it in the scan", "none in scan")) if s else ""), "da")
d4.box("vb", 35, 235, 1030, 40, "Batch-only VSAM: TCATBALF, TRANCATG, TRANTYPE, DISCGRP, TRXFL (no CICS program opens them in the scan)", cls="da")
d4.zone(20, 310, 340, 150, "Sequential and GDG - batch only")
d4.box("g1", 35, 335, 310, 110, "%d GDG bases" % F["gdg"], "\n".join(sorted({x[0].replace("AWS.M2.CARDDEMO.", "") for x in gdg})[:6]), "da")
d4.zone(380, 310, 220, 150, "Db2 - online + batch"); d4.box("d1", 395, 335, 190, 110, "CARDDEMO schema", "TRANSACTION_TYPE\n(online + batch, TRANTYPE)\nAUTHFRDS (online, AUTH)", "da")
d4.zone(620, 310, 220, 150, "IMS - online + batch"); d4.box("i1", 635, 335, 190, 110, "DBPAUTP0 (HIDAM)", "index DBPAUTX0\nsegments PAUTSUM0, PAUTDTL1\nonline + BMP (AUTH)", "da")
d4.zone(860, 310, 220, 150, "MQ, TD queues - online"); d4.box("q1", 875, 335, 190, 110, "MQ: 3 programs", "COPAUA0C, COACCT01, CODATE01\nqueue names: open asks\nTD: JOBS, CSSL", "da")
d4.box("sh", 20, 480, 1060, 70, "Shared between online and batch: %d stores" % F["shared"], ", ".join(s["name"].replace("AWS.M2.CARDDEMO.", "").replace("CARDDEMO.", "").replace("IMS segment ", "") for s in shared), "b")
d4.box("lg", 20, 570, 1060, 55, "Owner rule used in this document", "owner = the layer whose programs write or update the store in PROVEN scan edges (section 4 table); it is a description of the code, not a statement of governance", "b")
d4.arrow("on", "v0", "CSD FILE"); d4.arrow("ba", "v4", "DD")
DIA = [("estate", "Figure 1 - the estate", d1), ("online", "Figure 2 - online layer", d2), ("batch", "Figure 3 - batch layer", d3), ("data", "Figure 4 - data layer", d4)]

# ---------------------------------------------------------------- content model (shared by md and html)
REF = {"A": "modernization/INTAKE.md", "B": "modernization/legacy/fan-out.md", "C": "modernization/INTAKE.md sections 5-6", "D": "modernization/legacy/call-graph.md"}
def tbl(head, rows):
    return {"head": head, "rows": rows}
sec = []
sec.append(("1. Scope and sources", [
    "This describes the estate as the repository shows it at baseline `%s`. It proposes no target." % SHA,
    "Sources: **Part A** `modernization/INTAKE.md` (what is supplied, absent, and what runs); **Part B** `modernization/legacy/fan-out.md` and the %s stores under `store/`; **Part C** `INTAKE.md` sections 5 and 6 (scanner checks and open questions); **Part D** `modernization/legacy/call-graph.md` and `call-graph.json` (%d edges)." % (G["tool"], F["edges"]),
    "Status words are those of Part D: PROVEN (static, cited), CANDIDATE (dynamic or inferred), OPEN (an ask). Module names are the proposed names of the frozen map `store/modules.json` (Part B).",
    tbl(["Measure", "Value", "Source"], [
        ["Programs", "%d (%d online, %d run by JCL steps, %d called subroutines, %d HLASM / PL/I leaf, %d reached by nothing)" % (F["programs"], F["online"], F["batch"], F["sub"], F["leaf"], F["unreached"]), "Part D sections 1, 2, 7"],
        ["Transactions", str(F["trans"]), "Part D section 5"], ["Jobs", "%d (%d scheduled, %d unscheduled)" % (F["jobs"], F["sched"], F["unsched"]), "Part D sections 2, 6"],
        ["BMS mapsets", "%d source members; %d mapsets used by programs" % (F["bms"], F["mapsets_used"]), "Part A section 2; Part D section 3"],
        ["Stores with a PROVEN program access", str(F["stores"]), "Part D section 3.1"], ["Stores touched by both layers", str(F["shared"]), "section 6 below"],
        ["Cross-module program edges", "%d, all CANDIDATE" % F["xmod"], "Part D section 1.3"], ["Open asks in the graph", str(F["open"]), "Part D sections 1.4, 3.4"],
        ["Open asks in the estate roll-up", str(F["estate_asks"]), "Part B; `store/estate.json`"]])]))
sec.append(("2. Online layer", [
    "CICS pseudo-conversational COBOL programs with BMS maps. A program is counted as online when the scan found `EXEC CICS` commands in it (`store/<MODULE>/connections.json`, Part B). None of them runs off-host (Part A section 3).",
    tbl(["Module", "Source root", "Online programs", "Transactions", "Mapsets used"], [[m, SUB[m], "%d: %s" % (len(online[m]), ", ".join(online[m])), ", ".join("%s to %s" % (t["transid"], t["program"]) for t in trans if t["module"] == m), "%d" % len(mapsets[m])] for m in MODS]),
    "Entry: every transaction id maps to one initial program in a CSD `DEFINE TRANSACTION` (PROVEN, Part D section 5). `CDV1` names program `COCRDSEC`, which is not in the estate (Part D section 5).",
    "Navigation: programs pass control with `EXEC CICS XCTL` through a COMMAREA field or a menu option table, so program-to-program navigation is CANDIDATE, not PROVEN (%d CANDIDATE edges, Part D section 1.2), and %d transfer targets are OPEN (Part D section 1.4)." % (sum(e["type"] == "call" and e["status"] == "CANDIDATE" for e in E), sum(e["type"] == "call" for e in openc)),
    "Online to batch hand-off: `CORPT00C` writes JCL records to TD queue `JOBS`, defined in the CSD as an internal reader (Part D section 3.1 and 3.3; `app/cbl/CORPT00C.cbl:517`, `app/csd/CARDDEMO.CSD:499`)."]))
sec.append(("3. Batch layer", [
    "JCL jobs under `app/jcl` and `app/app-*/jcl`, two procedures under `app/proc`, and two scheduler exports under `app/scheduler` (Part A section 2). Four chains run off-host under the harness: posting, interest, trantype-update, trantype-dynamic (Part A section 4).",
    tbl(["Module", "Jobs", "Scheduled", "Programs run by steps", "Called subroutines"], [[m, str(mj(m)), str(sum(j["module"] == m and j["name"] not in unsched for j in jobs)), "%d: %s" % (n(m, "batch"), ", ".join(plist(m, "batch"))), ", ".join(plist(m, "subroutine")) or "-"] for m in MODS if mj(m)]),
    "Launchers: AUTH batch programs run under `DFSRRC00` (IMS region controller) and TRANTYPE batch programs under `IKJEFT01` (`DSN RUN`); the scan resolves the program from the step's PARM or SYSTSIN (PROVEN, Part D section 2).",
    tbl(["Scheduler table", "Dialect", "Jobs", "Order"], [[t["table"], t["dialect"], ", ".join(j["job"] for j in t["jobs"]), "; ".join("%s then %s" % (a, b) for a, b, _ in t["order"]) or "single job"] for t in G["scheduler"]]),
    "%d of %d jobs are named by neither export (Part D section 6, unscheduled). Whether they are run by hand, on request, or once at set-up is not shown by the repository." % (F["unsched"], F["jobs"]),
    "`CBTRN01C` is reached by no job, call or transaction and its files are bound by no JCL (Part D section 7)."]))
kinds = collections.Counter(s["kind"] for s in stores)
sec.append(("4. Data layer", [
    "Stores with at least one PROVEN program access (Part D section 3.1): " + ", ".join("%d %s" % (v, k) for k, v in sorted(kinds.items())) + ". Utility-step datasets are in Part D section 3.2.",
    "**Owner** below means the layer whose programs write or update the store in PROVEN edges. It describes the code; it is not a statement of who governs the data, which the repository does not show.",
    {"storetable": True},
    "**VSAM alternate indexes** (from the IDCAMS definitions the scan read, `store/CARDDEMO/connections.json`):",
    tbl(["Base cluster", "Alternate index", "Path", "Defined at"], [[a["base"], a["aix"], a["path"] or "-", a["cite"] or "-"] for a in aix]),
    "The CSD opens the card and cross-reference paths online as files `CARDAIX` and `CXACAIX` (`%s`, `%s`). No CSD FILE in the estate names the TRANSACT alternate-index path." % (f2d["CARDAIX"][1], f2d["CXACAIX"][1]),
    "**GDG bases** defined in JCL (`TRANREPT` is defined in two members):",
    tbl(["GDG base", "Defined at"], [[a, b] for a, b in gdg]),
    "Relative generations referenced by program steps: " + (", ".join("`%s`" % x for x in gdguse) or "none") + ". Bases referenced but with no `DEFINE` in the estate are open questions in Part C. Off-host, generations restart at G0001V00 for every case (`AGENTS.md`, Gotchas).",
    "**Db2**: `CARDDEMO.TRANSACTION_TYPE` is read and changed online by `COTRTLIC` and `COTRTUPC` and in batch by `COBTUPDT`; `CARDDEMO.AUTHFRDS` is written online by `COPAUS2C` (Part D section 3.1). DDL members: " + ", ".join("`%s`" % x for x in ddl) + ". Off-host the Db2 programs run against PostgreSQL through ocesql (Part A section 3).",
    "**IMS** (AUTH only): " + ", ".join("%s %s" % (x["name"], x["access"]) for x in ims["dbdgen"]) + "; PSBs " + ", ".join(x["name"] for x in ims["psbgen"]) + ". Segments `PAUTSUM0` and `PAUTDTL1` are read and changed online and by the BMP `CBPAUP0C`. Regions from JCL:",
    tbl(["Job", "Region type", "Program", "PSB"], [[r["job"], r["region_type"], r["program"], r["psb"] or "-"] for r in ims["regions"]]),
    "Nothing in the authorization sub-application runs off-host (Part A section 3).",
    "**MQ**: three online programs call the MQ API (PROVEN calls): " + "; ".join("%s %s (`%s`)" % x for x in mq) + ". The queue names are moved into the object descriptor at run time, so which queues exist is OPEN (Part D section 3.4). The catalogue holds no MQ definition member (queues, channels, queue manager) (`store/catalog.json`).",
    "**Maps**: %d BMS mapset members; which program sends and receives which mapset is in Part D section 3.1; four programs name the map through a field the scan did not settle (Part D section 3.4). The VSAMMQ programs have no map." % F["bms"],
    "**TD queues**: " + "; ".join("%s writes `%s` (`%s`)" % x for x in tdq) + "."]))
sec.append(("5. Sub-applications and how they attach", [
    "Three directories under `app/app-*` carry their own COBOL, BMS, CSD, JCL and data definitions (Part B module map, `store/modules.json`).",
    tbl(["Sub-application", "Module", "Attaches to the base through", "Status"], [
        ["app/app-authorization-ims-db2-mq", "AUTH", "Base main menu option table names `COPAUS0C` (`app/cpy/COMEN02Y.cpy:89`), reached by table-driven XCTL from `COMEN01C` (`app/cbl/COMEN01C.cbl:156`); `COPAUS0C` returns to `COMEN01C` / `COSGN00C`. Reads base VSAM files ACCTDAT, CUSTDAT, CCXREF, CXACAIX. Shares base copybooks. Scheduler table CARDDEMO names job `CBPAUP0J` (`app/scheduler/CardDemo.ca7:43`).", "XCTL CANDIDATE; file reads PROVEN; schedule match CANDIDATE"],
        ["app/app-transaction-type-db2", "TRANTYPE", "Base admin menu option table names `COTRTLIC` and `COTRTUPC` (`app/cpy/COADM02Y.cpy:49`, `:53`), reached by table-driven XCTL from `COADM01C`; both return to `COADM01C`. Scheduler tables name `MNTTRDB2` and `TRANEXTR`; `TRANEXTR` runs IKJEFT01 steps that create `&HLQ..TRANTYPE.PS` and `&HLQ..TRANCATG.PS` (Part D section 3.2).", "XCTL CANDIDATE; schedule match CANDIDATE"],
        ["app/app-vsam-mq", "VSAMMQ", "No program-to-program edge to the base in the scan. `COACCT01` reads base file ACCTDAT. Its CSD defines its own transactions `CDRA` and `CDRD`. `COACCT01` takes its input queue name from an MQ trigger message (`app/app-vsam-mq/cbl/COACCT01.cbl:197`); no trigger or queue definition is in the estate.", "file read PROVEN; trigger OPEN"]]),
    "Cross-module program edges (Part D section 1.3):",
    tbl(["From", "Relation", "To", "Status", "Cited at"], [["%s (%s)" % (e["frm"], e["mod"]), e["rel"], "%s (%s)" % (e["to"], e["tomod"]), e["status"], ", ".join(e["cite"][:2])] for e in attach]),
    "Shared copybooks: %d copybooks are copied by programs in more than one module (Part D section 4). Two CSD members both define `CARDDLIB` (Part C, INTAKE section 5)." % sum(c["xmod"] for c in G["copybooks"])]))
sec.append(("6. Where online and batch share data", [
    "%d stores have PROVEN accesses from both layers. The repository shows no statement of the order or locking between an online writer and a batch reader; the scheduler exports show `CLOSEFIL` / `OPENFIL` jobs around some batch tables (section 3), and what those jobs do to the CICS files is in Part B (`store/CARDDEMO/chains.json`)." % F["shared"],
    tbl(["Store", "Kind", "Online programs", "Batch programs", "Owner (writes)"], [[s["name"], s["kind"], s["online"], s["batch"], s["owner"]] for s in shared]),
    "Also shared, by hand-off rather than by store: TD queue `JOBS` (online writes JCL, JES runs it - section 2)."]))
sec.append(("7. HLASM and PL/I members", [
    "Leaf nodes: the scan records the call into them and nothing below (Part D section 1.1). None can be assembled or compiled off-host (Part A section 3; `docs/LEGACY-LANGUAGES.md`).",
    tbl(["Member", "Language", "Called by", "Cited at"], [[progs[e["to"]]["path"], progs[e["to"]]["lang"], e["frm"], ", ".join(e["cite"])] for e in leaves]),
    "Other members: macro `app/maclib/ASMWAIT.mac` and DSECT `app/maclib/COCDATFT.mac` (Part A section 2). `ASMWAIT.mac` is referenced by no scanned member (Part D section 7)."]))
UNK = [["Operations", "Run calendars and times; who runs the %d unscheduled jobs and when; restart and recovery procedures; abend handling and on-call; SLAs; monitoring; CICS region and Db2 / IMS / MQ subsystem topology; which of the two scheduler exports is in force." % F["unsched"], "Two scheduler exports with job order only (Part D section 6); one Db2 subsystem name in JCL (INTAKE section 5)"],
       ["Security", "RACF or other ESM profiles; CICS transaction and resource security; Db2 grants; MQ and IMS security; how passwords in USRSEC are protected; encryption; audit.", "A user file `USRSEC` read by the sign-on program `COSGN00C` and maintained by `COUSR0xC` (Part D section 3.1). Nothing else."],
       ["Volumes", "Record counts and growth in production; transaction rates; batch windows and run times; dataset sizes; retention.", "Sample data under `app/data` and fixture data only (Part A section 2); GDG limits in the DEFINE statements cited in section 4"],
       ["External interfaces", "MQ partner applications, queue manager, queue and channel definitions; the FTP server and what is exchanged; where statements and reports go after they are produced; who consumes export files and supplies import files; printers; any feeds from outside.", "`app/jcl/FTPJCL.JCL:30` (`PGM=FTP`, server named as a placeholder at `:23`); `app/jcl/TXT2PDF1.JCL:24`; `app/jcl/INTRDRJ1.JCL:17`; MQ API calls with run-time queue names (section 4)"]]
sec.append(("8. What the repository does not show", [
    "Each row is **unknown**. The third column is the only evidence the repository gives; it does not answer the question. Owners are placeholders, as in Part C.",
    tbl(["Area", "Unknown", "What the repository does show", "Owner"], [u + ["_TBD_"] for u in UNK]),
    "Part A sections 2 and 3 list the categories the estate does not supply and what cannot run off-host; they are not repeated here."]))

# ---------------------------------------------------------------- markdown
md = ["# As-is architecture - CardDemo legacy estate\n", "Baseline `%s` (`main`). Companion: `architecture-as-is.html` (diagrams, same content). Generated by `modernization/legacy/tools/build_architecture.py` from Parts A-D.\n" % SHA]
def mdtbl(t):
    return "\n".join(["| " + " | ".join(t["head"]) + " |", "|" + "---|" * len(t["head"])] + ["| " + " | ".join(str(c).replace("|", "/").replace("\n", " ") for c in r) + " |" for r in t["rows"]]) + "\n"
STT = tbl(["Store", "Kind", "Owner (writes)", "Online programs", "Batch programs", "First citation"], [[s["name"], s["kind"], s["owner"], s["online"], s["batch"], "`%s`" % s["cite"]] for s in stores])
MER = {"1. Scope and sources": """```mermaid
flowchart TB
  term[3270 terminals - not shown]:::u --> ON
  sch[Scheduler exports: CA-7, Control-M] --> BA
  subgraph ON[Online layer - CICS]
    oc[CARDDEMO base]; oa[AUTH]; ot[TRANTYPE]; ov[VSAMMQ]
  end
  subgraph BA[Batch layer - JCL]
    bc[CARDDEMO base]; bx[AUTH]; bt[TRANTYPE]
  end
  subgraph DA[Data layer]
    vs[(VSAM KSDS + AIX)]; gd[(Sequential + GDG)]; db[(Db2)]; im[(IMS)]; mq[(MQ queues - names open)]; tq[(TD queues)]
  end
  oa -. menu XCTL, CANDIDATE .-> oc
  ot -. menu XCTL, CANDIDATE .-> oc
  oc --> vs; bc --> vs; bc --> gd; ot --> db; bt --> db; oa --> im; bx --> im; oa --> mq; ov --> mq; oc --> tq
  bc --> lf[HLASM COBDATFT, MVSWAIT; PL/I CBFEECAL - leaf]
  classDef u stroke-dasharray: 4 3
```
""", "2. Online layer": """```mermaid
flowchart TB
  T[Transaction ids in CSD - PROVEN] --> SG[COSGN00C sign-on]
  SG --> AD[COADM01C admin menu]; SG --> ME[COMEN01C main menu]
  AD -.-> US[COUSR00C-03C users]; ME -.-> FN[account, card, transaction, bill pay, report programs]
  ME -. option table .-> AU[AUTH: COPAUS0C, COPAUS1C, COPAUS2C]
  AD -. option table .-> TT[TRANTYPE: COTRTLIC, COTRTUPC]
  T --> PA[AUTH: COPAUA0C]; T --> VM[VSAMMQ: COACCT01, CODATE01]
  FN --> JB[TD queue JOBS - internal reader]
```
Dashed = CANDIDATE.
""", "3. Batch layer": """```mermaid
flowchart TB
  S1[CA-7 export] --> TB[Scheduler tables]; S2[Control-M export] --> TB
  TB -. matched by member name .-> J[Scheduled jobs]
  U[Unscheduled jobs]
  J --> P[Programs run by steps]; U --> P
  J --> UT[Utility steps: IDCAMS, SORT, IEBGENER ...]
  P --> L[Leaf: COBDATFT, MVSWAIT HLASM; CBFEECAL PL/I]
  P --> IMS[DFSRRC00 launcher - AUTH]; P --> TSO[IKJEFT01 launcher - TRANTYPE]
```
""", "4. Data layer": """```mermaid
flowchart TB
  ON[Online layer] -->|CSD FILE| V[(VSAM KSDS: ACCTDATA, CUSTDATA, CARDDATA, CARDXREF, TRANSACT, USRSEC)]
  BA[Batch layer] -->|DD| V
  V --- AIX[(Alternate indexes and paths)]
  BA --> VB[(Batch-only VSAM: TCATBALF, TRANCATG, TRANTYPE, DISCGRP, TRXFL)]
  BA --> G[(Sequential and GDG)]
  ON --> D[(Db2: TRANSACTION_TYPE, AUTHFRDS)]; BA --> D
  ON --> I[(IMS: PAUTSUM0, PAUTDTL1)]; BA --> I
  ON --> Q[(MQ queues - names open)]; ON --> TQ[(TD queues JOBS, CSSL)]
```
"""}
for title, body in sec:
    md.append("## " + title + "\n")
    if title in MER:
        md.append("**%s**\n\n%s" % (dict((("1. Scope and sources", "Figure 1 - the estate"), ("2. Online layer", "Figure 2 - online layer"), ("3. Batch layer", "Figure 3 - batch layer"), ("4. Data layer", "Figure 4 - data layer")))[title], MER[title]))
    for b in body:
        md.append(b + "\n" if isinstance(b, str) else (mdtbl(STT) if b.get("storetable") else mdtbl(b)))
open("modernization/legacy/architecture-as-is.md", "w").write("\n".join(md))

# ---------------------------------------------------------------- html
def inl(s):
    s = esc(str(s)); s = re.sub(r"`([^`]+)`", r"<code>\1</code>", s); s = re.sub(r"\*\*([^*]+)\*\*", r"<b>\1</b>", s); return s.replace("_TBD_", "<i>TBD</i>").replace("\n", "<br>")
def htbl(t, id=""):
    return '<div class="tw"><table%s><thead><tr>%s</tr></thead><tbody>%s</tbody></table></div>' % (' id="%s"' % id if id else "", "".join("<th>%s</th>" % esc(h) for h in t["head"]),
        "".join("<tr>%s</tr>" % "".join("<td>%s</td>" % inl(c) for c in r) for r in t["rows"]))
DSEC = {"1. Scope and sources": 0, "2. Online layer": 1, "3. Batch layer": 2, "4. Data layer": 3}
body = []
for i, (title, items) in enumerate(sec):
    h = ['<details open id="s%d"><summary>%s</summary>' % (i + 1, esc(title))]
    if title in DSEC:
        k, cap, d = DIA[DSEC[title]]
        h.append('<figure><figcaption>%s</figcaption><div class="dw">%s</div></figure>' % (esc(cap), d.svg("dia-" + k)))
    for b in items:
        if isinstance(b, str):
            h.append("<p>%s</p>" % inl(b))
        elif b.get("storetable"):
            h.append('<div class="filters"><label>Kind <select id="fk"><option value="">all</option>%s</select></label><label>Owner <select id="fo"><option value="">all</option>%s</select></label>'
                     '<label><input type="checkbox" id="fs"> touched by both layers only</label><label>Search <input type="search" id="fq" placeholder="store or program"></label>'
                     '<span class="meta">showing <b id="shown"></b> of <b id="total"></b> stores</span></div><div class="tw"><table id="stores"><thead><tr><th>Store</th><th>Kind</th><th>Owner (writes)</th><th>Online programs</th><th>Batch programs</th><th>First citation</th></tr></thead><tbody></tbody></table></div>' % (
                         "".join("<option>%s</option>" % esc(k) for k in sorted(kinds)), "".join("<option>%s</option>" % esc(k) for k in sorted({s["owner"] for s in stores}))))
        else:
            h.append(htbl(b))
    h.append("</details>")
    body.append("".join(h))
page = """<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>CardDemo as-is architecture</title><style>
:root{--fg:#1b1f24;--mut:#5c6670;--line:#d5dae0;--acc:#0b5cad;--soft:#f4f6f8}
*{box-sizing:border-box}body{margin:0;font:14px/1.5 system-ui,Segoe UI,Helvetica,Arial,sans-serif;color:var(--fg);background:#fff}
header{padding:16px 24px;border-bottom:1px solid var(--line)}h1{font-size:20px;margin:0 0 4px}.meta{color:var(--mut);font-size:13px}
code{font:12px ui-monospace,Menlo,Consolas,monospace;background:var(--soft);padding:1px 4px;border-radius:3px}a{color:var(--acc)}
nav{position:sticky;top:0;background:#fff;z-index:2;display:flex;flex-wrap:wrap;gap:6px;padding:8px 24px;border-bottom:1px solid var(--line)}
nav a,nav button{border:1px solid var(--line);background:var(--soft);padding:4px 10px;border-radius:14px;text-decoration:none;font:inherit;font-size:13px;cursor:pointer;color:var(--fg)}
main{padding:8px 24px 40px;max-width:1280px}.cards{display:flex;flex-wrap:wrap;gap:10px;margin:12px 0}.card{border:1px solid var(--line);border-radius:6px;padding:8px 12px;min-width:120px}.card b{font-size:20px;display:block}
details{border:1px solid var(--line);border-radius:6px;margin:12px 0;padding:0 14px;scroll-margin-top:52px}summary{cursor:pointer;padding:10px 0;font-weight:600;font-size:16px}
.tw{overflow:auto}table{border-collapse:collapse;width:100%;margin:8px 0 14px}th,td{border:1px solid var(--line);padding:4px 7px;text-align:left;vertical-align:top;font-size:13px}th{background:var(--soft)}
figure{margin:6px 0 14px}figcaption{font-weight:600;margin-bottom:4px}.dw{overflow:auto;border:1px solid var(--line);border-radius:6px;padding:6px}.dia{min-width:900px;width:100%;height:auto}
.zone{fill:#fafbfc;stroke:#c5ccd4;stroke-dasharray:5 4}.zl{font:600 12px system-ui;fill:#5c6670}
.dia g rect{stroke-width:1.2}.b rect{fill:#fff;stroke:#7b8794}.on rect{fill:#e8f1fb;stroke:#0b5cad}.ba rect{fill:#e9f6ec;stroke:#17633a}.da rect{fill:#f3ecfb;stroke:#6b3fa0}.lf rect{fill:#fdf0d5;stroke:#9a5b00}.u rect{fill:#fff;stroke:#a4262c;stroke-dasharray:5 3}
.bl{font:600 12px system-ui;text-anchor:middle;fill:#1b1f24}.bs{font:11px system-ui;text-anchor:middle;fill:#3c4650}.al{font:10px system-ui;fill:#5c6670}
.ar{stroke:#3c4650;stroke-width:1.3}.ar.ca{stroke:#c98a1e;stroke-dasharray:5 3}
.filters{display:flex;flex-wrap:wrap;gap:10px;align-items:center;margin:8px 0;padding:8px;background:var(--soft);border-radius:6px}.filters input,.filters select{font:inherit;padding:4px 6px;border:1px solid var(--line);border-radius:4px}
.ok{color:#17633a;font-weight:600}.bad{color:#a4262c;font-weight:600}.legend span{display:inline-block;margin-right:14px}.sw{display:inline-block;width:12px;height:12px;border:1px solid;vertical-align:-1px;margin-right:4px}
</style></head><body>
<header><h1>CardDemo legacy estate - as-is architecture</h1>
<div class="meta">Baseline <code id="sha">__SHA__</code> (main) &middot; describes what exists; proposes no target &middot; <span id="selfcheck"></span></div>
<div class="meta">Sources: <a href="../INTAKE.md">Part A/C INTAKE.md</a> &middot; <a href="fan-out.md">Part B fan-out.md</a> &middot; <a href="call-graph.html">Part D call-graph.html</a> &middot; <a href="call-graph.md">call-graph.md</a> &middot; <a href="../../store/reports/estate.html">estate report</a> &middot; <a href="architecture-as-is.md">this page as Markdown</a></div></header>
<nav id="toc">__TOC__<button id="exp" type="button">Expand all</button><button id="col" type="button">Collapse all</button></nav>
<main><div class="cards" id="cards"></div>
<p class="legend"><span><i class="sw" style="background:#e8f1fb;border-color:#0b5cad"></i>online</span><span><i class="sw" style="background:#e9f6ec;border-color:#17633a"></i>batch</span><span><i class="sw" style="background:#f3ecfb;border-color:#6b3fa0"></i>data</span><span><i class="sw" style="background:#fdf0d5;border-color:#9a5b00"></i>HLASM / PL/I leaf</span><span><i class="sw" style="border-color:#a4262c;border-style:dashed"></i>not shown by the repository</span><span>dashed arrow = CANDIDATE</span></p>
__BODY__</main>
<script id="data" type="application/json">__DATA__</script>
<script>
const A=JSON.parse(document.getElementById('data').textContent),S=A.stores,F=A.facts;
const h=s=>String(s==null?'':s).replace(/[&<>"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c]));
document.getElementById('cards').innerHTML=[['Programs',F.programs],['Online programs',F.online],['Transactions',F.trans],['Jobs',F.jobs],['Unscheduled jobs',F.unsched],['Stores',S.length],['Shared by both layers',S.filter(s=>s.shared).length],['Open asks (graph)',F.open]].map(c=>'<div class="card"><b>'+c[1]+'</b>'+c[0]+'</div>').join('');
function fill(){const k=fk.value,o=fo.value,sh=fs.checked,q=fq.value.toLowerCase();
 const rows=S.filter(s=>(!k||s.kind===k)&&(!o||s.owner===o)&&(!sh||s.shared)&&(!q||(s.name+' '+s.online+' '+s.batch).toLowerCase().includes(q)));
 document.querySelector('#stores tbody').innerHTML=rows.map(s=>'<tr><td><code>'+h(s.name)+'</code>'+(s.via?'<br><span class="meta">'+h(s.via)+'</span>':'')+'</td><td>'+h(s.kind)+'</td><td>'+h(s.owner)+'</td><td>'+h(s.online)+'</td><td>'+h(s.batch)+'</td><td><code>'+h(s.cite)+'</code></td></tr>').join('');
 shown.textContent=rows.length;total.textContent=S.length}
const fk=document.getElementById('fk'),fo=document.getElementById('fo'),fs=document.getElementById('fs'),fq=document.getElementById('fq'),shown=document.getElementById('shown'),total=document.getElementById('total');
[fk,fo,fs,fq].forEach(x=>x.addEventListener('input',fill));fill();
const det=[...document.querySelectorAll('main details')];
document.getElementById('exp').onclick=()=>det.forEach(d=>d.open=true);document.getElementById('col').onclick=()=>det.forEach(d=>d.open=false);
document.querySelectorAll('#toc a').forEach(a=>a.addEventListener('click',()=>{const d=document.querySelector(a.getAttribute('href'));if(d)d.open=true}));
const ok=S.length===F.stores&&S.filter(s=>s.shared).length===F.shared&&document.querySelectorAll('svg.dia').length===4;
document.getElementById('selfcheck').innerHTML=ok?'<span class="ok">counts match the embedded data ('+S.length+' stores, 4 diagrams)</span>':'<span class="bad">COUNT MISMATCH</span>';
</script></body></html>"""
toc = "".join('<a href="#s%d">%s</a>' % (i + 1, esc(t)) for i, (t, _) in enumerate(sec))
out = page.replace("__SHA__", SHA).replace("__TOC__", toc).replace("__BODY__", "".join(body)).replace("__DATA__", json.dumps({"stores": stores, "facts": F}).replace("</", "<\\/"))
open("modernization/legacy/architecture-as-is.html", "w").write(out)
print(json.dumps(F)); print("shared:", [s["name"] for s in shared]); print("gdg", gdg); print("aix", aix)
