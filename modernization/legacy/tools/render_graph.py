#!/usr/bin/env python3
"""Render modernization/legacy/call-graph.json to call-graph.md and a self-contained call-graph.html.
Run from the repository root after build_graph.py."""
import json, html, collections

D = json.load(open("modernization/legacy/call-graph.json"))
E = D["edges"]; SHA = D["baseline"]
esc = html.escape
def cites(c, n=3):
    return ", ".join("`%s`" % x for x in c[:n]) + (" (+%d more)" % (len(c) - n) if len(c) > n else "")
def by(t, st=None):
    return [e for e in E if e["type"] == t and (st is None or e["status"] == st)]
TYPES = [("call", "Program to program"), ("job", "Job to step to program"), ("data", "Program to data"),
         ("copybook", "Copybook use"), ("tran", "Transaction to program"), ("sched", "Scheduler")]
cnt = {t: collections.Counter(e["status"] for e in by(t)) for t, _ in TYPES}

# ------------------------------------------------------------------ markdown
m = []
w = m.append
w("# Call graph - CardDemo legacy estate\n")
w("Baseline `%s` (`main`). Built from the %s scan stores (`store/<MODULE>/*.json`, `store/catalog.json`) by "
  "`modernization/legacy/tools/build_graph.py`; the full edge list with every citation is `modernization/legacy/call-graph.json` "
  "and the browsable view is `call-graph.html`. Nothing here is taken from member names alone.\n" % (SHA, D["tool"]))
w("## How to read the status\n")
w("* **PROVEN** - a static statement the scanner read, cited by file and line.\n* **CANDIDATE** - a dynamic target the scan narrowed to a set of values, "
  "or a link inferred by matching names across modules. Not a fact.\n* **OPEN** - the target is not settled in the source. It is an open ask; no target is guessed.\n")
w("PROVEN and CANDIDATE edges are kept in separate tables throughout.\n")
w("## Counts\n\n| Edge family | PROVEN | CANDIDATE | OPEN | Total |\n|---|---|---|---|---|")
for t, name in TYPES:
    c = cnt[t]; w("| %s | %d | %d | %d | %d |" % (name, c["PROVEN"], c["CANDIDATE"], c["OPEN"], sum(c.values())))
w("| **All edges** | %d | %d | %d | %d |\n" % tuple([sum(cnt[t][s] for t, _ in TYPES) for s in ("PROVEN", "CANDIDATE", "OPEN")] + [len(E)]))
w("Nodes: %d programs (%d COBOL, %d HLASM leaf, %d PL/I leaf), %d jobs, %d copybooks named in COPY/INCLUDE (%d used by more than one program), %d transactions.\n" % (
    len(D["programs"]), sum(p["lang"] == "COBOL" for p in D["programs"]), sum(p["lang"] == "HLASM" for p in D["programs"]),
    sum(p["lang"] == "PL/I" for p in D["programs"]), len(D["jobs"]), len(D["copybooks"]), sum(c["shared"] for c in D["copybooks"]), len(D["transactions"])))

w("## 1. Program to program\n")
def calltable(rows, extra=False):
    w("| From | Relation | To | Target is | Module (from -> to) | Cited at |\n|---|---|---|---|---|---|")
    for e in rows:
        w("| `%s` | %s | `%s` | %s | %s -> %s%s | %s |" % (e["frm"], e["rel"], e["to"], e.get("tkind", ""), e["mod"], e.get("tomod") or "-",
                                                      " **(cross-module)**" if e["xmod"] else "", cites(e["cite"])))
    w("")
w("### 1.1 PROVEN (%d)\n" % cnt["call"]["PROVEN"]); calltable(by("call", "PROVEN"))
w("HLASM and PL/I members are leaf nodes: `COBDATFT` and `MVSWAIT` (HLASM), `CBFEECAL` (PL/I). The scan records the CALL into them; their own logic is not analysed.\n")
w("### 1.2 CANDIDATE (%d)\n\nDynamic `XCTL`/`LINK` whose target field the scan narrowed to these values. Each needs confirming.\n" % cnt["call"]["CANDIDATE"]); calltable(by("call", "CANDIDATE"))
w("### 1.3 Cross-module edges (no child scan could see both ends)\n")
w("| From (module) | Relation | To (module) | Status | Cited at |\n|---|---|---|---|---|")
for e in E:
    if e["type"] in ("call", "tran") and e["xmod"]:
        w("| `%s` (%s) | %s | `%s` (%s) | %s | %s |" % (e["frm"], e["mod"], e["rel"], e["to"], e.get("tomod"), e["status"], cites(e["cite"])))
w("\nAll program-to-program edges between modules are CANDIDATE: the base menus transfer control through a table "
  "(`EXEC CICS XCTL PROGRAM(CDEMO-MENU-OPT-PGMNAME(WS-OPTION))`, `app/cbl/COMEN01C.cbl:156`), not a literal. "
  "`store/estate.json` lists two of them (`COMEN01C` to `COPAUS0C`, `COADM01C` to `COTRTLIC`) as proven; the module traces grade them candidate and this graph follows the traces.\n")
w("### 1.4 OPEN - dynamic targets not settled (%d open asks)\n\n| Program | Statement | Field | Cited at | Owner |\n|---|---|---|---|---|" % cnt["call"]["OPEN"])
for e in by("call", "OPEN"):
    w("| `%s` | %s | %s | %s | _TBD_ |" % (e["frm"], e["rel"], e["to"], cites(e["cite"])))
w("")
w("## 2. Job to step to program\n\nAll %d edges are PROVEN (JCL `EXEC` statements). One row per step.\n" % cnt["job"]["PROVEN"])
w("| Job (module) | Step | EXEC | Program run | Cited at |\n|---|---|---|---|---|")
steps = collections.OrderedDict()
for e in by("job"):
    if e["rel"] == "has step":
        steps[e["to"]] = {"job": e["frm"], "mod": e["mod"], "cite": e["cite"], "exec": "", "run": ""}
    elif e["rel"] in ("EXEC PGM", "EXEC PROC"):
        steps[e["frm"]]["exec"] += ("PROC " if e["rel"] == "EXEC PROC" else "PGM ") + e["to"] + " "
    else:
        steps[e["frm"]]["run"] = "`%s` %s" % (e["to"], e["rel"].replace("runs ", ""))
for k, s in steps.items():
    w("| `%s` (%s) | %s | %s | %s | %s |" % (s["job"], s["mod"], k.split(".", 1)[1], s["exec"].strip(), s["run"] or "-", cites(s["cite"])))
w("")
w("## 3. Program to dataset, table, queue or map\n")
def datatable(rows):
    w("| From | Direction | To | Store | Status | Cited at | Note |\n|---|---|---|---|---|---|---|")
    for e in rows:
        w("| `%s` | %s | `%s` | %s | %s | %s | %s |" % (e["frm"], e["dir"], e["to"], e["rel"], e["status"], cites(e["cite"], 2), e["note"].replace("|", "/")))
    w("")
pd = [e for e in by("data", "PROVEN") if not e["dir"].startswith("DISP")]
w("### 3.1 PROVEN - programs (%d)\n" % len(pd)); datatable(pd)
ut = [e for e in by("data", "PROVEN") if e["dir"].startswith("DISP")]
w("### 3.2 PROVEN - utility steps (%d)\n\nA utility step names the dataset on a DD; the direction shown is the JCL `DISP`, not a read/write claim.\n" % len(ut)); datatable(ut)
w("### 3.3 CANDIDATE (%d)\n" % cnt["data"]["CANDIDATE"]); datatable(by("data", "CANDIDATE"))
w("### 3.4 OPEN (%d open asks)\n" % cnt["data"]["OPEN"]); datatable(by("data", "OPEN"))
w("## 4. Programs sharing copybook layouts\n\n%d copybooks are copied by more than one program; %d of them by programs in more than one module. Every use is a PROVEN `COPY`/`INCLUDE` line (all %d are in `call-graph.json`).\n" % (
    sum(c["shared"] for c in D["copybooks"]), sum(c["xmod"] for c in D["copybooks"]), cnt["copybook"]["PROVEN"]))
w("| Copybook | Member | Programs | Modules | Used by | First citation |\n|---|---|---|---|---|---|")
for c in sorted(D["copybooks"], key=lambda c: -len(c["programs"])):
    if c["shared"]:
        w("| `%s` | %s | %d | %s | %s | `%s` |" % (c["name"], "`%s`" % c["path"] if c["path"] else "not in the estate", len(c["programs"]), ", ".join(c["modules"]),
                                                 ", ".join(c["programs"]), c["uses"][0]["cite"]))
w("")
w("## 5. Transaction id to initial program (CSD)\n\n| Transaction | Initial program | CSD module | Program module | Status | Cited at |\n|---|---|---|---|---|---|")
for e in by("tran"):
    w("| `%s` | `%s` | %s | %s | %s | %s |" % (e["frm"], e["to"], e["mod"], e.get("tomod") or "**program not in the estate**", e["status"], cites(e["cite"])))
w("")
w("## 6. Scheduler order\n\nFrom `app/scheduler/CardDemo.ca7` and `app/scheduler/CardDemo.controlm`. \"Runs before\" edges are PROVEN from the export. "
  "The link from a scheduled job to a JCL member is CANDIDATE: it is a match on member name across modules, and the exports name no library.\n")
for t in D["scheduler"]:
    w("### Table `%s` (%s, `%s`)\n\n| Job | JCL member matched | Cited at |\n|---|---|---|" % (t["table"], t["dialect"], t["path"]))
    for j in t["jobs"]:
        w("| `%s` | %s | `%s` |" % (j["job"], "`%s`" % j["jcl"] if j["jcl"] else "**none in the estate - open ask**", j["cite"]))
    if t["order"]:
        w("\nOrder: " + "; ".join("`%s` -> `%s`" % (a, b) for a, b, _ in t["order"]) + "\n")
    else:
        w("\nOrder: single job, no predecessor in this table.\n")
w("Across tables:\n")
for e in by("sched"):
    if "across" in e["rel"]:
        w("* %s -> %s (%s) - PROVEN, `%s`" % (e["frm"], e["to"], e["note"], e["cite"][0]))
w("\n### Unscheduled JCL jobs (%d)\n\nJobs in the estate that neither scheduler export names.\n\n| Job | Module | Member |\n|---|---|---|" % len(D["unscheduled"]))
for n, mo, p in D["unscheduled"]:
    w("| `%s` | %s | `%s` |" % (n, mo, p))
w("")
w("## 7. Uncalled programs and unreferenced members\n\n### Uncalled programs (%d)\n\nNo PROVEN or CANDIDATE call, no JCL step and no CSD transaction reaches them.\n\n| Program | Module | Member |\n|---|---|---|" % len(D["uncalled"]))
for u in D["uncalled"]:
    w("| `%s` | %s | `%s` |" % (u["program"], u["module"], u["path"]))
w("\nPrograms reached only through CANDIDATE edges or only by a transaction are not in this list; `call-graph.json` (`entered`) shows what reaches each program.\n")
w("### Unreferenced members (%d)\n\nMembers that no scanned member refers to by name. \"Text mentions\" is a plain word search of `app/` for the member name, given so a reader can tell a truly unused member from a scanner gap; it is not evidence of use.\n\n| Member | Kind | Text mentions elsewhere | Where |\n|---|---|---|---|" % len(D["unreferenced"]))
for u in D["unreferenced"]:
    w("| `%s` | %s | %d | %s |" % (u["path"], u["kind"], u["mentions"], ", ".join("`%s`" % x for x in u["where"]) or "-"))
w("")
w("## 8. Disagreements with `docs/chain-graph.md` (%d)\n\n| What | Item | Scan | docs/chain-graph.md |\n|---|---|---|---|" % len(D["disagreements"]))
for x in D["disagreements"]:
    w("| %s | `%s` | %s | %s |" % (x["what"], x["item"], x["scan"], x["doc"]))
w("\nWhere both list a program, copybooks, DD names and static CALLs agree except as shown. `docs/chain-graph.md` covers `app/cbl`, the transaction-type sub-application and lower-case `.jcl` members only; the authorization and VSAM-MQ sub-applications and the upper-case `.JCL`/`.CBL` members are outside it.\n")
open("modernization/legacy/call-graph.md", "w").write("\n".join(m) + "\n")

# ------------------------------------------------------------------ svg diagram (program to program)
progs = D["programs"]; pos = {}
cols = {"CARDDEMO": 4, "AUTH": 2, "TRANTYPE": 1, "VSAMMQ": 1}; x0 = 20; boxes = []
NW, NH, GX, GY = 92, 22, 26, 34
for mod in D["modules"]:
    ps = [p for p in progs if p["module"] == mod]; nc = cols.get(mod, 1)
    for i, p in enumerate(ps):
        pos[p["id"]] = (x0 + 14 + (i % nc) * (NW + GX), 60 + (i // nc) * (NH + GY))
    wd = nc * (NW + GX) + 4; ht = ((len(ps) + nc - 1) // nc) * (NH + GY) + 20
    boxes.append((mod, x0, 30, wd, ht, len(ps))); x0 += wd + 30
W = x0; H = max(b[4] for b in boxes) + 60
svg = ['<svg id="callsvg" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 %d %d" role="img" aria-label="Program to program call graph">' % (W, H),
       '<defs><marker id="ah" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse"><path d="M0 0L10 5L0 10z" fill="context-stroke"/></marker></defs>']
for mod, x, y, wd, ht, n in boxes:
    svg.append('<rect x="%d" y="%d" width="%d" height="%d" rx="6" class="mbox"/><text x="%d" y="%d" class="mlabel">%s (%d programs)</text>' % (x, y, wd, ht, x + 8, y - 8, mod, n))
ne = 0
for e in by("call"):
    if e["status"] == "OPEN" or e["frm"] not in pos or e["to"] not in pos or e["frm"] == e["to"]:
        continue
    (x1, y1), (x2, y2) = pos[e["frm"]], pos[e["to"]]; ne += 1
    a = (x1 + NW / 2, y1 + NH if y2 > y1 else y1); b = (x2 + NW / 2, y2 if y2 > y1 else y2 + NH)
    if y1 == y2:
        a = (x1 + (NW if x2 > x1 else 0), y1 + NH / 2); b = (x2 + (0 if x2 > x1 else NW), y2 + NH / 2)
    mx = (a[0] + b[0]) / 2 + (18 if e["status"] == "CANDIDATE" else -18)
    svg.append('<path class="edge %s" data-from="%s" data-to="%s" marker-end="url(#ah)" d="M%.0f %.0f Q%.0f %.0f %.0f %.0f"><title>%s %s %s - %s - %s</title></path>' % (
        e["status"], e["frm"], e["to"], a[0], a[1], mx, (a[1] + b[1]) / 2, b[0], b[1], e["frm"], e["rel"], e["to"], e["status"], esc(e["cite"][0])))
for p in progs:
    x, y = pos[p["id"]]
    svg.append('<g class="node %s" data-id="%s" tabindex="0"><rect x="%d" y="%d" width="%d" height="%d" rx="4"/><text x="%d" y="%d">%s</text><title>%s (%s) %s</title></g>' % (
        "leaf" if p["leaf"] else "cobol", p["id"], x, y, NW, NH, x + NW / 2, y + 15, p["id"], p["id"], p["lang"], p["path"]))
svg.append("</svg>")
D["diagram_edges"] = ne

# ------------------------------------------------------------------ html
page = r"""<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>CardDemo call graph</title><style>
:root{--bg:#fff;--fg:#1b1f24;--mut:#5c6670;--line:#d5dae0;--acc:#0b5cad;--pr:#17633a;--ca:#9a5b00;--op:#a4262c;--soft:#f4f6f8}
*{box-sizing:border-box}body{margin:0;font:14px/1.45 system-ui,Segoe UI,Helvetica,Arial,sans-serif;color:var(--fg);background:var(--bg)}
header{padding:16px 24px;border-bottom:1px solid var(--line)}h1{font-size:20px;margin:0 0 4px}h2{font-size:16px;margin:18px 0 8px}
.meta{color:var(--mut);font-size:13px}code{font:12px ui-monospace,Menlo,Consolas,monospace;background:var(--soft);padding:1px 4px;border-radius:3px}
nav{display:flex;flex-wrap:wrap;gap:4px;padding:8px 24px 0;border-bottom:1px solid var(--line)}
nav button{border:1px solid var(--line);border-bottom:none;background:var(--soft);padding:7px 12px;border-radius:6px 6px 0 0;cursor:pointer;font:inherit}
nav button[aria-selected=true]{background:#fff;border-color:var(--acc);color:var(--acc);font-weight:600}
main{padding:16px 24px 40px}section[hidden]{display:none}
.cards{display:flex;flex-wrap:wrap;gap:10px}.card{border:1px solid var(--line);border-radius:6px;padding:8px 12px;min-width:120px}.card b{font-size:20px;display:block}
table{border-collapse:collapse;width:100%;margin:8px 0}th,td{border:1px solid var(--line);padding:4px 7px;text-align:left;vertical-align:top;font-size:13px}th{background:var(--soft);position:sticky;top:0}
.st{font-weight:600;font-size:12px;padding:1px 6px;border-radius:10px;border:1px solid}.PROVEN{color:var(--pr)}.CANDIDATE{color:var(--ca)}.OPEN{color:var(--op)}
.filters{display:flex;flex-wrap:wrap;gap:10px;align-items:center;margin:8px 0;padding:8px;background:var(--soft);border-radius:6px}
.filters input,.filters select{font:inherit;padding:4px 6px;border:1px solid var(--line);border-radius:4px}
details{border:1px solid var(--line);border-radius:6px;margin:8px 0;padding:0 10px}summary{cursor:pointer;padding:8px 0;font-weight:600}
.diagram{overflow:auto;border:1px solid var(--line);border-radius:6px;padding:6px}svg{min-width:1000px;width:100%;height:auto}
.mbox{fill:#fafbfc;stroke:var(--line)}.mlabel{font:600 12px system-ui;fill:var(--mut)}
.node rect{fill:#e8f1fb;stroke:#0b5cad}.node.leaf rect{fill:#fdf0d5;stroke:#9a5b00}.node text{font:10px ui-monospace,monospace;text-anchor:middle;fill:#1b1f24}.node{cursor:pointer}
.edge{fill:none;stroke-width:1.2}.edge.PROVEN{stroke:#17633a}.edge.CANDIDATE{stroke:#c98a1e;stroke-dasharray:4 3}
svg.sel .edge{opacity:.07}svg.sel .edge.on{opacity:1;stroke-width:2.2}svg.sel .node{opacity:.3}svg.sel .node.on{opacity:1}
.legend span{margin-right:14px}.ok{color:var(--pr);font-weight:600}.bad{color:var(--op);font-weight:600}a{color:var(--acc)}
</style></head><body>
<header><h1>CardDemo legacy estate - call graph</h1>
<div class="meta">Baseline <code id="sha">__SHA__</code> (main) &middot; __TOOL__ scan stores &middot; companion: <a href="architecture-as-is.html">architecture-as-is.html</a>, <a href="call-graph.md">call-graph.md</a>
&middot; <span id="selfcheck"></span></div></header>
<nav role="tablist" id="tabs"></nav><main id="main"></main>
<script id="data" type="application/json">__DATA__</script>
<script>
const D=JSON.parse(document.getElementById('data').textContent),E=D.edges;
const SVG=__SVG__;
const h=s=>String(s==null?'':s).replace(/[&<>"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c]));
const cite=c=>c.slice(0,3).map(x=>'<code>'+h(x)+'</code>').join(' ')+(c.length>3?' +'+(c.length-3)+' more':'');
const st=s=>'<span class="st '+s+'">'+s+'</span>';
const TYPES=[['call','Program to program'],['job','Job / step / program'],['data','Program to data'],['copybook','Copybooks'],['tran','Transactions'],['sched','Scheduler']];
const TABS=[['overview','Overview']].concat(TYPES).concat([['lists','Uncalled / unreferenced'],['dis','Disagreements']]);
const tabs=document.getElementById('tabs'),main=document.getElementById('main');
function count(t,s){return E.filter(e=>e.type===t&&(!s||e.status===s)).length}
function edgeTable(t){
  const mods=[...new Set(E.filter(e=>e.type===t).map(e=>e.mod))];
  return '<div class="filters"><label>Status <select data-f="status"><option value="">all</option><option>PROVEN</option><option>CANDIDATE</option><option>OPEN</option></select></label>'+
  '<label>Module <select data-f="mod"><option value="">all</option>'+mods.map(m=>'<option>'+m+'</option>').join('')+'</select></label>'+
  '<label><input type="checkbox" data-f="xmod"> cross-module only</label>'+
  '<label>Search <input type="search" data-f="q" placeholder="program, dataset, file..."></label>'+
  '<span class="meta">showing <b data-shown></b> of <b data-total>'+count(t)+'</b> edges</span></div>'+
  ['PROVEN','CANDIDATE','OPEN'].map(s=>'<details open data-status="'+s+'"><summary>'+st(s)+' <span data-n></span> edges</summary><table><thead><tr><th>#</th><th>From</th><th>Relation</th><th>Direction</th><th>To</th><th>Module</th><th>Cited at</th><th>Note</th></tr></thead><tbody></tbody></table></details>').join('');
}
function fill(sec,t){
  const f={};sec.querySelectorAll('[data-f]').forEach(x=>f[x.dataset.f]=x.type==='checkbox'?x.checked:x.value);
  const q=(f.q||'').toLowerCase();let shown=0;
  sec.querySelectorAll('details[data-status]').forEach(d=>{const s=d.dataset.status;
    const rows=E.filter(e=>e.type===t&&e.status===s&&(!f.status||f.status===s)&&(!f.mod||e.mod===f.mod)&&(!f.xmod||e.xmod)&&(!q||(e.frm+' '+e.to+' '+e.rel+' '+e.note+' '+e.cite.join(' ')).toLowerCase().includes(q)));
    shown+=rows.length;d.querySelector('[data-n]').textContent=rows.length;d.hidden=!!(f.status&&f.status!==s);
    d.querySelector('tbody').innerHTML=rows.map(e=>'<tr><td>'+e.id+'</td><td><code>'+h(e.frm)+'</code></td><td>'+h(e.rel)+'</td><td>'+h(e.dir)+'</td><td><code>'+h(e.to)+'</code>'+(e.tkind?' <span class="meta">'+h(e.tkind)+'</span>':'')+'</td><td>'+h(e.mod)+(e.xmod?' &rarr; '+h(e.tomod||'other')+' (cross-module)':'')+'</td><td>'+cite(e.cite)+'</td><td>'+h(e.note)+'</td></tr>').join('');});
  sec.querySelector('[data-shown]').textContent=shown;
}
function simple(head,rows){return '<table><thead><tr>'+head.map(x=>'<th>'+x+'</th>').join('')+'</tr></thead><tbody>'+rows.map(r=>'<tr>'+r.map(c=>'<td>'+c+'</td>').join('')+'</tr>').join('')+'</tbody></table>'}
const P={};
P.overview=()=>'<h2>Counts</h2><div class="cards">'+[['Programs',D.programs.length],['Jobs',D.jobs.length],['Transactions',D.transactions.length],['Copybooks used',D.copybooks.length],['Edges',E.length],['PROVEN',E.filter(e=>e.status==='PROVEN').length],['CANDIDATE',E.filter(e=>e.status==='CANDIDATE').length],['OPEN asks',E.filter(e=>e.status==='OPEN').length]].map(c=>'<div class="card"><b data-card="'+c[0]+'">'+c[1]+'</b>'+c[0]+'</div>').join('')+'</div>'+
 simple(['Edge family','PROVEN','CANDIDATE','OPEN','Total'],TYPES.map(t=>[t[1],count(t[0],'PROVEN'),count(t[0],'CANDIDATE'),count(t[0],'OPEN'),count(t[0])]))+
 '<h2>Program to program diagram</h2><p class="legend"><span><b style="color:#17633a">&#8212;&#8212;</b> PROVEN</span><span><b style="color:#c98a1e">- - -</b> CANDIDATE</span><span>blue = COBOL program</span><span>amber = HLASM / PL/I leaf</span><span class="meta">Click a program to highlight its edges; click it again or press Esc to clear. <b id="dn"></b></span></p><div class="diagram">'+SVG+'</div>'+
 '<p class="meta">The diagram draws the '+D.diagram_edges+' PROVEN and CANDIDATE edges whose two ends are programs in the estate. Calls to runtime routines, MQ and IMS stubs, self-transfers and OPEN targets are in the tables only.</p>';
TYPES.forEach(t=>P[t[0]]=()=>'<h2>'+t[1]+'</h2>'+edgeTable(t[0]));
P.lists=()=>'<details open><summary>Uncalled programs ('+D.uncalled.length+')</summary>'+simple(['Program','Module','Member'],D.uncalled.map(u=>['<code>'+u.program+'</code>',u.module,'<code>'+u.path+'</code>']))+'</details>'+
 '<details open><summary>Unreferenced members ('+D.unreferenced.length+')</summary>'+simple(['Member','Kind','Text mentions elsewhere'],D.unreferenced.map(u=>['<code>'+h(u.path)+'</code>',u.kind,u.mentions]))+'</details>'+
 '<details><summary>Unscheduled JCL jobs ('+D.unscheduled.length+')</summary>'+simple(['Job','Module','Member'],D.unscheduled.map(u=>['<code>'+u[0]+'</code>',u[1],'<code>'+u[2]+'</code>']))+'</details>'+
 '<details><summary>Copybooks shared by more than one program ('+D.copybooks.filter(c=>c.shared).length+')</summary>'+simple(['Copybook','Programs','Modules','Used by'],D.copybooks.filter(c=>c.shared).sort((a,b)=>b.programs.length-a.programs.length).map(c=>['<code>'+c.name+'</code>',c.programs.length,c.modules.join(', '),c.programs.join(', ')]))+'</details>';
P.dis=()=>'<h2>Disagreements with docs/chain-graph.md ('+D.disagreements.length+')</h2>'+simple(['What','Item','Scan','docs/chain-graph.md'],D.disagreements.map(x=>[h(x.what),'<code>'+h(x.item)+'</code>',h(x.scan),h(x.doc)]));
TABS.forEach(([id,name],i)=>{const b=document.createElement('button');b.textContent=name;b.id='tab-'+id;b.setAttribute('role','tab');b.setAttribute('aria-controls','p-'+id);tabs.appendChild(b);
 const s=document.createElement('section');s.id='p-'+id;s.setAttribute('role','tabpanel');s.innerHTML=P[id]();main.appendChild(s);
 if(TYPES.some(t=>t[0]===id)){fill(s,id);s.addEventListener('input',()=>fill(s,id));}
 b.onclick=()=>show(id);});
function show(id){TABS.forEach(([t])=>{document.getElementById('p-'+t).hidden=t!==id;document.getElementById('tab-'+t).setAttribute('aria-selected',t===id)});if(location.hash!=='#'+id)history.replaceState(null,'','#'+id)}
tabs.addEventListener('keydown',e=>{if(e.key!=='ArrowRight'&&e.key!=='ArrowLeft')return;const bs=[...tabs.children],i=bs.indexOf(document.activeElement);if(i<0)return;const n=bs[(i+(e.key==='ArrowRight'?1:bs.length-1))%bs.length];n.focus();n.click()});
show(TABS.some(t=>'#'+t[0]===location.hash)?location.hash.slice(1):'overview');
const svg=document.getElementById('callsvg');let cur=null;
function sel(id){cur=cur===id?null:id;svg.classList.toggle('sel',!!cur);svg.querySelectorAll('.on').forEach(x=>x.classList.remove('on'));let n=0;if(cur){const keep=new Set([cur]);svg.querySelectorAll('.edge').forEach(p=>{if(p.dataset.from===cur||p.dataset.to===cur){p.classList.add('on');n++;keep.add(p.dataset.from);keep.add(p.dataset.to)}});svg.querySelectorAll('.node').forEach(g=>{if(keep.has(g.dataset.id))g.classList.add('on')})}
 document.getElementById('dn').textContent=cur?cur+': '+n+' edges highlighted':''}
svg.querySelectorAll('.node').forEach(g=>{g.addEventListener('click',()=>sel(g.dataset.id));g.addEventListener('keydown',e=>{if(e.key==='Enter')sel(g.dataset.id)})});
document.addEventListener('keydown',e=>{if(e.key==='Escape'&&cur)sel(cur)});
const want=__COUNTS__,got={};E.forEach(e=>{const k=e.type+'/'+e.status;got[k]=(got[k]||0)+1});
const okc=JSON.stringify(Object.keys(want).sort().map(k=>[k,want[k]]))===JSON.stringify(Object.keys(got).sort().map(k=>[k,got[k]]))&&svg.querySelectorAll('.edge').length===D.diagram_edges&&svg.querySelectorAll('.node').length===D.programs.length;
document.getElementById('selfcheck').innerHTML=okc?'<span class="ok">counts match the embedded data ('+E.length+' edges)</span>':'<span class="bad">COUNT MISMATCH</span>';
</script></body></html>"""
out = (page.replace("__SHA__", SHA).replace("__TOOL__", D["tool"]).replace("__SVG__", json.dumps("".join(svg)))
       .replace("__COUNTS__", json.dumps(D["counts"])).replace("__DATA__", json.dumps(D, default=list).replace("</", "<\\/")))
open("modernization/legacy/call-graph.html", "w").write(out)
print("md lines", len(m), "html bytes", len(out), "diagram edges", ne)
