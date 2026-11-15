#!/usr/bin/env python3
"""Render stream-INTCALC.html (one self-contained file) from manifest.json.

Run from the repository root:  python3 modernization/streams/interest/tools/render_stream.py
"""
import html
import json
import re
from pathlib import Path

outDir = Path(__file__).resolve().parents[1]
manifest = json.loads((outDir / "manifest.json").read_text())
counts = manifest["counts"]
scope = manifest["scope"]
hlq = "AWS.M2.CARDDEMO."
refRx = re.compile(r"\b([CDIUVAO]\d{1,3})\b")
knownIds = ({e["id"] for e in manifest["edges"]} | {a["id"] for a in manifest["openAsks"]}
            | {u["id"] for u in manifest["scheduler"]["unproven"]}
            | {v["id"] for v in manifest["views"]["disagreements"]} | {o["id"] for o in manifest["outside"]})


def esc(value):
    return html.escape("" if value is None else str(value))


def xref(text):
    """Escape text and turn known ids (D3, A4, ...) into in-page links."""
    return refRx.sub(lambda m: f'<a class="xref" href="#{m.group(1)}">{m.group(1)}</a>'
                     if m.group(1) in knownIds else m.group(1), esc(text))


def cites(items):
    out = []
    for item in items if isinstance(items, list) else [items]:
        if not item:
            continue
        if item in manifest["excerpts"]:
            out.append(f'<a class="cite" href="#src" data-cite="{esc(item)}">{esc(item)}</a>')
        else:
            out.append(f"<code>{esc(item)}</code>")
    return " ".join(out)


def badge(status):
    return f'<span class="st {status.lower()}">{status}</span>'


def short(dsn):
    return dsn.replace(hlq, "")


def table(headers, rows, attrs=""):
    head = "".join(f"<th>{h}</th>" for h in headers)
    return f'<div class="scroll"><table {attrs}><thead><tr>{head}</tr></thead><tbody>{"".join(rows)}</tbody></table></div>'


def row(cells, attrs=""):
    return f"<tr {attrs}>" + "".join(f"<td>{c}</td>" for c in cells) + "</tr>"


def select(name, label, values):
    options = '<option value="">All</option>' + "".join(f'<option value="{esc(v)}">{esc(v)}</option>' for v in values)
    return f'<label>{label} <select data-filter="{name}">{options}</select></label>'


# ---------------------------------------------------------------- diagrams
def box(x, y, w, h, title, sub="", cls="node", link=None):
    body = (f'<rect class="{cls}" x="{x}" y="{y}" width="{w}" height="{h}" rx="5"/>'
            f'<text x="{x + w / 2}" y="{y + (h / 2 if not sub else h / 2 - 5)}" class="nt">{esc(title)}</text>')
    if sub:
        body += f'<text x="{x + w / 2}" y="{y + h / 2 + 11}" class="ns">{esc(sub)}</text>'
    return f'<a href="#{link}">{body}</a>' if link else body


def arrow(x1, y1, x2, y2, status, label, link, labelDx=0, labelDy=-5, at=0.5):
    cls = "ep" if status == "PROVEN" else "ec"
    mx, my = x1 + (x2 - x1) * at + labelDx, y1 + (y2 - y1) * at + labelDy
    labelLayer.append(f'<a href="#{link}"><text x="{mx}" y="{my}" class="el {cls}t">{esc(label)}</text></a>')
    return (f'<a href="#{link}"><line class="{cls}" x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" '
            f'marker-end="url(#m{cls})"/></a>')


labelLayer = []


def flushLabels():
    out = "".join(labelLayer)
    labelLayer.clear()
    return out


def streamDiagram():
    edgeBy = {e["id"]: e for e in manifest["edges"]}
    parts = ['<svg id="diagram-stream" class="diagram" viewBox="0 0 1180 560" role="img" '
             'aria-label="Interest stream: job, step, program, datasets and edges">',
             '<defs><marker id="mep" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto">'
             '<path d="M0,0L10,5L0,10z" class="mp"/></marker>'
             '<marker id="mec" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto">'
             '<path d="M0,0L10,5L0,10z" class="mc"/></marker></defs>',
             '<rect class="frame" x="400" y="70" width="400" height="330" rx="8"/>',
             '<text x="412" y="90" class="ft">Job INTCALC - app/jcl/INTCALC.jcl (stream)</text>']
    parts.append(box(430, 110, 340, 44, "STEP15", "EXEC PGM=CBACT04C, PARM='2022071800'", "node step"))
    parts.append(box(430, 200, 340, 44, "load module CBACT04C", "STEPLIB AWS.M2.CARDDEMO.LOADLIB", "node lm"))
    parts.append(box(430, 290, 340, 90, "PROGRAM-ID CBACT04C", "app/cbl/CBACT04C.cbl", "node pgm"))
    parts.append(arrow(600, 154, 600, 200, "PROVEN", "C1 EXEC PGM=", "C1", 52, 3))
    parts.append(arrow(600, 244, 600, 290, "CANDIDATE", "C2 built from (name match)", "C2", 92, 3))
    inputs = [("D8", "JCL PARM", "'2022071800'"), ("D9", "system clock", "FUNCTION CURRENT-DATE"),
              ("D1", "TCATBALF.VSAM.KSDS", "DD TCATBALF"), ("D2", "CARDXREF.VSAM.KSDS", "DD XREFFILE"),
              ("D3", "CARDXREF.VSAM.AIX.PATH", "DD XREFFIL1"), ("D6", "DISCGRP.VSAM.KSDS", "DD DISCGRP"),
              ("D4", "ACCTDATA.VSAM.KSDS", "DD ACCTFILE")]
    for i, (edgeId, title, sub) in enumerate(inputs):
        y = 20 + i * 62
        cls = "node ds" if i > 1 else "node ext"
        parts.append(box(20, y, 250, 44, title, sub, cls))
        parts.append(arrow(270, y + 22, 430, 300 + i * 11, edgeBy[edgeId]["status"], edgeId, edgeId, 0, -6, 0.2))
    parts.append(arrow(430, 372, 270, 20 + 6 * 62 + 36, "PROVEN", "D5 REWRITE", "D5", -10, 20))
    outputs = [("D7", "SYSTRAN(+1)", "DD TRANSACT - GDG, LRECL 350", "node ds", 300),
               ("D10", "SYSOUT", "DD SYSOUT - DISPLAY", "node ext", 335),
               ("C3", "CEE3ABD", "runtime routine, not in estate", "node ext", 370)]
    for i, (edgeId, title, sub, cls, fromY) in enumerate(outputs):
        y = 200 + i * 70
        parts.append(box(910, y, 250, 44, title, sub, cls))
        parts.append(arrow(770, fromY, 910, y + 22, edgeBy[edgeId]["status"], edgeId, edgeId, 0, -6))
    for i, cpy in enumerate(scope["copybooks"]):
        x = 330 + i * 124
        parts.append(box(x, 470, 112, 44, cpy["name"], f'{cpy["recordLength"]} bytes', "node cpy", f'cpy-{cpy["name"]}'))
        parts.append(arrow(480 + i * 60, 380, x + 56, 470, "PROVEN", f"I{i + 1}", f"I{i + 1}", -14, 0))
    parts.append('<line class="ep" x1="20" y1="540" x2="70" y2="540" marker-end="url(#mep)"/>'
                 '<text x="78" y="544" class="lg">PROVEN (static, cited)</text>'
                 '<line class="ec" x1="250" y1="540" x2="300" y2="540" marker-end="url(#mec)"/>'
                 '<text x="308" y="544" class="lg">CANDIDATE (dynamic or inferred)</text>'
                 '<text x="560" y="544" class="lg">Click an edge label to open its evidence. Dataset names omit the prefix AWS.M2.CARDDEMO.</text>')
    parts.append(flushLabels() + "</svg>")
    return "".join(parts)


def schedulerDiagram():
    parts = ['<svg id="diagram-scheduler" class="diagram" viewBox="0 0 1180 300" role="img" '
             'aria-label="Scheduler order of the folder and the setup jobs">',
             '<defs><marker id="mep2" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto"><path d="M0,0L10,5L0,10z" class="mp"/></marker><marker id="mec2" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto"><path d="M0,0L10,5L0,10z" class="mc"/></marker></defs>',
             '<text x="20" y="22" class="ft">Setup jobs (tools/runjcl/chains.json, harness order) - not stream logic, not in this folder</text>']
    for i, job in enumerate(manifest["setup"]["jobs"]):
        parts.append(box(20 + i * 150, 34, 135, 40, job["job"], f'setup {job["order"]}', "node setup", "tab-setup"))
        if i:
            parts.append(f'<line class="es" x1="{5 + i * 150}" y1="54" x2="{20 + i * 150}" y2="54"/>')
    parts.append('<path class="es" d="M755,54 L800,54 L800,112 L337,112 L337,160" fill="none" marker-end="url(#mec)"/>'
                 '<text x="812" y="92" class="lg">needed before a fresh run</text>')
    parts.append('<text x="20" y="236" class="ft">Control-M folder MONTHLY-InterestCalculation (app/scheduler/CardDemo.controlm)</text>')
    order = manifest["scheduler"]["folderOrder"]
    for i, job in enumerate(order):
        x = 20 + i * 232
        role = {"INTCALC": "stream", "CLOSEFIL": "predecessor", "COMBTRAN": "successor"}.get(job["job"], "later in folder")
        parts.append(box(x, 162, 170, 48, job["job"], role, "node pgm" if job["job"] == "INTCALC" else "node ext", "tab-scheduler"))
        if i:
            parts.append(arrow(x - 62, 186, x, 186, "PROVEN", f'cond -{order[i - 1]["job"]}', "tab-scheduler", 0, -32))
    parts.append('<text x="20" y="262" class="lg">Solid arrows: IN/OUT conditions in the export (PROVEN). '
                 'The CA 7 listing (app/scheduler/CardDemo.ca7) names neither INTCALC nor COMBTRAN.</text>'
                 '<text x="20" y="282" class="lg">Not proven by either export: run frequency, order against POSTTRAN and the file reloads, '
                 'completion of the CICS close. See the Scheduler tab.</text>')
    parts.append(flushLabels() + "</svg>")
    return "".join(parts)


# ---------------------------------------------------------------- sections
def overview():
    cards = [("jobs", "Jobs in scope", "tab-scope"), ("steps", "Steps", "tab-scope"),
             ("programsInScope", "Programs in scope", "tab-scope"), ("copybooks", "Copybooks", "tab-scope"),
             ("datasets", "Datasets", "tab-scope"), ("edges", "Edges", "tab-edges"),
             ("edgesProven", "Edges PROVEN", "tab-edges"), ("edgesCandidate", "Edges CANDIDATE", "tab-edges"),
             ("setupJobs", "Setup jobs", "tab-setup"), ("outside", "Outside-stream edges", "tab-outside"),
             ("outsideJobs", "Other jobs on the datasets", "tab-outside"),
             ("outsidePrograms", "CICS programs on the datasets", "tab-outside"),
             ("outsideTransactions", "CICS transactions on the datasets", "tab-outside"),
             ("predecessors", "Predecessor jobs", "tab-scheduler"), ("successors", "Successor jobs", "tab-scheduler"),
             ("unprovenOrderings", "Orderings not proven", "tab-scheduler"),
             ("disagreements", "Disagreements between views", "tab-views"), ("openAsks", "Open asks", "tab-asks")]
    cardHtml = "".join(f'<a class="card" href="#{tab}" data-count="{key}"><b>{counts[key]}</b>{label}</a>'
                       for key, label, tab in cards)
    meta = manifest["meta"]
    return f"""
<p>The stream is one batch job, <code>INTCALC</code>, with one step that runs <code>CBACT04C</code>. It reads the
transaction-category balances, looks up the account, the card cross-reference and the disclosure-group rate, writes
one interest transaction per balance with a non-zero rate to a new generation of <code>SYSTRAN</code>, and rewrites
the account balance. {cites(scope["jobs"][0]["descriptionBasis"])}</p>
<p>{esc(meta["outOfScope"])}</p>
<div class="cards">{cardHtml}</div>
<h2>Stream diagram</h2>{streamDiagram()}
<h2>Scheduler and setup</h2>{schedulerDiagram()}
<h2>How to read the marks</h2>
<ul><li>{badge("PROVEN")} {esc(meta["statusLegend"]["PROVEN"])}.</li>
<li>{badge("CANDIDATE")} {esc(meta["statusLegend"]["CANDIDATE"])}.</li>
<li>A source reference such as {cites(scope["steps"][0]["source"])} opens the cited lines, which are stored in this file.</li>
<li>Ids such as {xref("D3")}, {xref("A4")} or {xref("U2")} jump to the edge, ask or ordering they name.</li></ul>"""


def scopeSection():
    job, step = scope["jobs"][0], scope["steps"][0]
    jobRows = [row([f'<code>{job["name"]}</code>', "stream", cites(job["jobCard"]),
                    f'{esc(job["scheduler"]["folder"])} {cites(job["scheduler"]["definition"])}', esc(job["description"])])]
    stepRows = [row([f'<code>{step["job"]}.{step["step"]}</code>', f'<code>{step["execPgm"]}</code>',
                     f'<code>{step["parm"]}</code>', cites(step["source"]),
                     f'<code>{step["steplib"]["dsn"]}</code> {cites(step["steplib"]["source"])}',
                     " ".join(f'{s["dd"]} {cites(s["source"])}' for s in step["sysout"])])]
    programRows = []
    for p in scope["programs"]:
        notes = "".join(f'<li>{esc(n["text"])} {cites(n["evidence"])}</li>' for n in p["notes"])
        programRows.append(row([f'<code>{p["id"]}</code>', esc(p["kind"]), "in scope" if p["inScope"] else "boundary",
                                cites(p["path"]) if p["path"] else "no source in the estate",
                                cites(p["programId"]) if p["programId"] else "-",
                                f'<details><summary>{len(p["notes"])} note(s)</summary><ul>{notes}</ul></details>']))
    dsRows = []
    for d in scope["datasets"]:
        cat = d["catalog"]
        catText = f'{cat["type"]}, defined by <code>{cat["definedBy"]}</code> {cites(cat["source"])}'
        for key in ("keys", "recordsize", "limit", "pathentry"):
            if key in cat:
                catText += f'<br>{key.upper()} <code>{esc(cat[key])}</code>'
        layout = d.get("recordLayout") or {}
        layoutText = (f'<a class="xref" href="#cpy-{layout["copybook"]}">{layout["copybook"]}</a> '
                      f'{layout["record"]}, {layout["recordLength"]} bytes<br>'
                      + (cites(layout["boundAt"]) if layout["boundAt"] in manifest["excerpts"] else esc(layout["boundAt"])))
        gen = f'({d["generation"]})' if d["generation"] else "-"
        extra = ""
        if d.get("gdg"):
            extra = (f'<br>GDG LIMIT {d["gdg"]["limit"]} {cites(d["gdg"]["definedAt"])}<br>'
                     f'DCB <code>{d["dcb"]["value"]}</code> {cites(d["dcb"]["source"])}')
        dsRows.append(row([f'<code>{d["dd"]}</code> {cites(d["ddSource"])}', f'<code>{d["dsn"]}</code>', gen + extra,
                           f'<code>{esc(d["disp"])}</code>', catText, esc(d["direction"]),
                           (f'<code>{d["cobolFile"]}</code> {cites([d["selectSource"], d["fdSource"]])}<br>{esc(d["access"])}'
                            if d["cobolFile"] else esc(d["access"])), layoutText]))
    cpyHtml = []
    for c in scope["copybooks"]:
        fieldRows = [row([f["level"], f'<code>{f["name"]}</code>', f'<code>{esc(f["pic"] or "group")}</code>',
                          "" if f["offset"] is None else f["offset"], "" if f["length"] is None else f["length"]])
                     for f in c["fields"]]
        cpyHtml.append(
            f'<details class="block" id="cpy-{c["name"]}"><summary><code>{c["name"]}</code> - record {c["record"]}, '
            f'{c["recordLength"]} bytes, {len(c["fields"])} items, used for {c["file"]}</summary>'
            f'<p>Member {cites(c["path"])}; stated length {cites(c["statedLength"])}; included at {cites(c["includedAt"])}; '
            f'bound to the file at {cites(c["usedAt"])} (the FD record {c["fdRecord"]} is a key plus filler, the '
            f'copybook record is the layout the program moves it into or from). Length: {esc(c["recordLengthBasis"])}.</p>'
            + table(["Level", "Item", "PIC", "Offset", "Length"], fieldRows, f'data-rows="copybook-{c["name"]}"') + "</details>")
    return f"""
<h2>Jobs <span class="n">({counts["jobs"]})</span></h2>
{table(["Job", "Role", "JCL", "Scheduler definition", "What it does"], jobRows, 'data-rows="jobs"')}
<h2>Steps <span class="n">({counts["steps"]})</span></h2>
{table(["Step", "EXEC PGM", "PARM", "Source", "STEPLIB", "Output DDs"], stepRows, 'data-rows="steps"')}
<h2>Programs <span class="n">({counts["programsInScope"]} in scope, {counts["boundaryRoutines"]} boundary routine)</span></h2>
<p>The JCL names a load module, the source declares a PROGRAM-ID, and the file has a name. They are the same string
here, but only the file-to-PROGRAM-ID link is stated in a source; the load-module link is edge {xref("C2")}.</p>
{table(["Id", "Kind", "Scope", "Source file", "PROGRAM-ID", "Notes"], programRows, 'data-rows="programs"')}
<h2>Datasets and DD names <span class="n">({counts["ddAllocations"]} DD allocations, {counts["datasets"]} datasets)</span></h2>
{table(["DD", "Dataset", "GDG generation", "DISP", "Catalog definition", "Direction", "COBOL file", "Record layout"], dsRows, 'data-rows="datasets"')}
<h2>Copybooks and record layouts <span class="n">({counts["copybooks"]})</span></h2>
<p><button type="button" data-expand="#tab-scope">Expand all</button> <button type="button" data-collapse="#tab-scope">Collapse all</button></p>
{"".join(cpyHtml)}"""


def edgesSection():
    rows = []
    for e in manifest["edges"]:
        rows.append(row([f'<b>{e["id"]}</b>', e["kind"], f'<code>{esc(short(e["from"]))}</code>',
                         f'<code>{esc(short(e["to"]))}</code>', esc(e["label"]), esc(e["direction"] or "-"),
                         badge(e["status"]), xref(e["basis"]), cites(e["evidence"])],
                    f'id="{e["id"]}" data-status="{e["status"]}" data-kind="{e["kind"]}"'))
    return f"""
<p>Every call, copy and data edge of the stream. PROVEN and CANDIDATE edges are separate rows and are never combined.
{counts["edgesProven"]} PROVEN, {counts["edgesCandidate"]} CANDIDATE; {counts["callEdges"]} call, {counts["copyEdges"]} copy,
{counts["dataEdges"]} data. CBACT04C has no dynamic CALL.</p>
<div class="filters" data-for="edges">{select("status", "Status", ["PROVEN", "CANDIDATE"])}
{select("kind", "Kind", ["call", "copy", "data"])}
<label>Search <input type="search" data-filter="text" placeholder="DD, dataset, id"></label>
<button type="button" data-clear>Clear</button> <span class="showing" data-showing="edges"></span></div>
{table(["Id", "Kind", "From", "To", "Label", "Direction", "Status", "Basis", "Evidence"], rows, 'data-rows="edges"')}"""


def setupSection():
    meta = manifest["setup"]["meta"]
    rows = []
    for s in manifest["setup"]["jobs"]:
        stepList = "".join(f'<li><code>{st["step"]}</code> {esc(st["pgm"])}: {esc(st["action"])} {cites(st["source"])}'
                           + (" - " + ", ".join(f"<code>{esc(short(d))}</code>" for d in st["datasets"]) if st["datasets"] else "")
                           + "</li>" for st in s["steps"])
        rows.append(row([s["order"], f'<code>{s["job"]}</code> <span class="st setup">SETUP</span>',
                         f'{cites(s["jcl"])}<br>listed at {cites(s["listedAt"])}',
                         f'<details><summary>{len(s["steps"])} step(s)</summary><ul>{stepList}</ul></details>',
                         esc(s["provides"]), esc(", ".join(s["scheduledIn"]) or "no scanned scheduler table")]))
    inputs = ", ".join(f"<code>{esc(short(k))}</code> ({v} bytes)" for k, v in meta["inputs"].items())
    return f"""
<p>These jobs are <b>setup</b>: they build the files the stream needs before it can run. They are not stream logic
and are not in the stream's Control-M folder. {esc(meta["note"])} List: {cites(meta["source"])}; the stream's own job
list: {cites(meta["streamJobs"])}. Flat-file inputs the harness loads from: {inputs}.</p>
<p><button type="button" data-expand="#tab-setup">Expand all</button> <button type="button" data-collapse="#tab-setup">Collapse all</button></p>
{table(["Order", "Job", "JCL", "Steps", "What the stream needs from it", "Also scheduled in"], rows, 'data-rows="setup"')}
<p>{counts["setupJobs"]} setup jobs, {counts["setupSteps"]} steps. Every one of these steps also appears under
Outside-stream dependencies when it touches one of the stream's datasets, marked with relation "setup".</p>"""


def outsideSection():
    rows = []
    for o in manifest["outside"]:
        relation = o.get("relation", "")
        where = ", ".join(o.get("scheduledIn", [])) if o["actorKind"] == "batch job step" else ""
        if o["actorKind"] == "batch job step" and not where:
            where = "no scanned scheduler table"
        rows.append(row([f'<b>{o["id"]}</b>', f'<code>{esc(short(o["dataset"]))}</code>',
                         f'<code>{esc(o["actor"])}</code><br><span class="sub">{o["actorKind"]}, module {esc(o["module"])}</span>',
                         f'{esc(o["via"])}<br><span class="sub">program <code>{esc(o["program"])}</code></span>',
                         f'<span class="dir {o["direction"]}">{o["direction"]}</span>', badge(o["status"]),
                         esc(relation), esc(o["basis"]) + (f"<br><i>Scheduled in: {esc(where)}</i>" if where else ""),
                         cites(o["evidence"])],
                    f'id="{o["id"]}" data-status="{o["status"]}" data-dataset="{esc(short(o["dataset"]))}" '
                    f'data-actorkind="{o["actorKind"]}" data-direction="{o["direction"]}" data-relation="{relation}"'))
    dsns = sorted({short(o["dataset"]) for o in manifest["outside"]})
    directions = sorted({o["direction"] for o in manifest["outside"]})
    matrix = []
    for dsn in dsns:
        cells = [f"<code>{esc(dsn)}</code>"]
        for kind in ("batch job step", "CICS program", "CICS transaction"):
            sub = [o for o in manifest["outside"] if short(o["dataset"]) == dsn and o["actorKind"] == kind]
            cells.append(str(len(sub)))
        sub = [o for o in manifest["outside"] if short(o["dataset"]) == dsn]
        cells.append(str(sum(1 for o in sub if o["direction"] in ("write", "read-write", "define", "delete"))))
        cells.append(str(len(sub)))
        matrix.append(row(cells))
    return f"""
<p>Every other job step, CICS program and CICS transaction that touches one of the stream's {counts["datasets"]}
datasets, with the direction of the edge as seen from that actor. {counts["outside"]} rows: {counts["outsideBatch"]}
batch job steps ({counts["outsideJobs"]} jobs), {counts["outsideCicsPrograms"]} CICS program rows
({counts["outsidePrograms"]} programs), {counts["outsideCicsTransactions"]} CICS transaction rows
({counts["outsideTransactions"]} transactions); {counts["outsideProven"]} PROVEN, {counts["outsideCandidate"]} CANDIDATE.
A transaction has one PROVEN row for the programs it reaches by a static path and a separate CANDIDATE row for
programs it reaches only through a dynamic XCTL or LINK target. Direction "control" is the CICS file close or open
sent by CLOSEFIL and OPENFIL; "define" and "delete" are IDCAMS catalog actions.</p>
{table(["Dataset", "Batch job steps", "CICS program rows", "CICS transaction rows", "Rows that change or redefine it", "All rows"], matrix, 'data-rows="outside-matrix"')}
<div class="filters" data-for="outside">{select("dataset", "Dataset", dsns)}
{select("actorkind", "Actor", ["batch job step", "CICS program", "CICS transaction"])}
{select("direction", "Direction", directions)}{select("status", "Status", ["PROVEN", "CANDIDATE"])}
{select("relation", "Relation", ["setup", "predecessor", "successor", "later in folder", "other"])}
<label>Search <input type="search" data-filter="text" placeholder="job, program, transaction"></label>
<button type="button" data-clear>Clear</button> <span class="showing" data-showing="outside"></span></div>
{table(["Id", "Dataset", "Actor", "Via", "Direction", "Status", "Relation", "Basis", "Evidence"], rows, 'data-rows="outside"')}"""


def schedulerSection():
    sch = manifest["scheduler"]
    exportRows = [row([cites(x["file"]) if x["file"] in manifest["excerpts"] else f'<code>{x["file"]}</code>', esc(x["dialect"]),
                       "yes" if x["namesStreamJob"] else "no",
                       cites(x["folder"]) if x.get("folder") else f'{esc(x["note"])} {cites(x["evidence"])}']) for x in sch["exports"]]
    orderRows = [row([o["position"], f'<code>{o["job"]}</code>', o["role"], cites(o["definition"])]) for o in sch["folderOrder"]]

    def neighbour(items, name):
        return [row([f'<code>{n["job"]}</code>', n["distance"], badge(n["status"]), f'{esc(n["basis"])} {cites(n["evidence"])}',
                     f'{esc(n["whatItDoes"])} {cites(n["whatEvidence"])}']) for n in items]
    attrs = sch["runAttributes"]
    stated = "; ".join(f"{k}: <code>{esc(v)}</code>" for k, v in attrs["stated"].items())
    unprovenRows = [row([f'<b>{u["id"]}</b>', esc(u["text"]), cites(u["evidence"]), xref(u["ask"]) if u["ask"] else "-"],
                        f'id="{u["id"]}"') for u in sch["unproven"]]
    return f"""
<h2>Exports</h2>
{table(["File", "Dialect", "Names INTCALC", "Evidence"], exportRows, 'data-rows="exports"')}
<h2>Order inside the folder <span class="n">({len(sch["folderOrder"])} jobs)</span></h2>
{table(["Position", "Job", "Role for this stream", "Definition"], orderRows, 'data-rows="folder-order"')}
<h2>Predecessors named by the exports <span class="n">({counts["predecessors"]})</span></h2>
{table(["Job", "Distance", "Status", "Basis", "What it does"], neighbour(sch["predecessors"], "pred"), 'data-rows="predecessors"')}
<h2>Successors named by the exports <span class="n">({counts["successors"]})</span></h2>
{table(["Job", "Distance", "Status", "Basis", "What it does"], neighbour(sch["successors"], "succ"), 'data-rows="successors"')}
<h2>Run attributes of INTCALC</h2>
<p>Stated in the definition {cites(attrs["definition"])}: {stated}. Not stated: {esc(", ".join(attrs["notStated"]))}.</p>
<h2>Ordering the exports do not prove <span class="n">({counts["unprovenOrderings"]})</span></h2>
{table(["Id", "What is not proven", "Evidence", "Ask"], unprovenRows, 'data-rows="unproven"')}"""


def viewsSection():
    v = manifest["views"]
    items = []
    for d in v["disagreements"]:
        items.append(f'<details class="block" id="{d["id"]}" open><summary><b>{d["id"]}</b> {esc(d["topic"])}</summary>'
                     f'<dl><dt>mftb batch-chain-analysis</dt><dd>{esc(d["mftb"])}</dd>'
                     f'<dt>docs/chain-graph.md</dt><dd>{esc(d["chainGraph"])}</dd>'
                     f'<dt>Decided from the source</dt><dd>{xref(d["decision"])} {cites(d["evidence"])}</dd></dl></details>')
    return f"""
<p><b>View 1 - {esc(v["mftb"]["entryPoint"])}.</b> <code>{esc(v["mftb"]["command"])}</code>: {esc(v["mftb"]["resolved"])}.
{esc(v["mftb"]["rerun"])}</p>
<p><b>View 2 - {esc(v["chainGraph"]["file"])}.</b> Generated by {esc(v["chainGraph"]["generatedBy"])}; section {cites(v["chainGraph"]["section"])}.</p>
<h2>Where they agree</h2><ul>{"".join(f"<li>{esc(a)}</li>" for a in v["agreements"])}</ul>
<h2>Where they disagree <span class="n">({counts["disagreements"]})</span></h2>
<p><button type="button" data-expand="#tab-views">Expand all</button> <button type="button" data-collapse="#tab-views">Collapse all</button></p>
{"".join(items)}"""


def asksSection():
    rows = [row([f'<b>{a["id"]}</b>', f'<span class="sev {a["severity"]}">{a["severity"]}</span>', esc(a["owner"]),
                 esc(a["question"]), esc(a["why"]), cites(a["evidence"]), xref(" ".join(a["related"])) or "-"],
                f'id="{a["id"]}" data-severity="{a["severity"]}"') for a in manifest["openAsks"]]
    return f"""
<p>Questions the sources in the repository cannot settle. Each names who can answer it.</p>
<div class="filters" data-for="asks">{select("severity", "Severity", ["high", "medium", "low"])}
<label>Search <input type="search" data-filter="text" placeholder="text"></label>
<button type="button" data-clear>Clear</button> <span class="showing" data-showing="asks"></span></div>
{table(["Id", "Severity", "Owner", "Ask", "Why it is open", "Evidence", "Related"], rows, 'data-rows="asks"')}"""


tabs = [("overview", "Overview", overview()), ("scope", "In scope", scopeSection()), ("edges", "Edges", edgesSection()),
        ("setup", "Setup jobs", setupSection()), ("outside", "Outside-stream dependencies", outsideSection()),
        ("scheduler", "Scheduler", schedulerSection()), ("views", "Two views", viewsSection()),
        ("asks", "Open asks", asksSection())]
navHtml = "".join(f'<button type="button" role="tab" id="tabbtn-{key}" aria-controls="tab-{key}" '
                  f'aria-selected="{"true" if i == 0 else "false"}" tabindex="{0 if i == 0 else -1}">{label}</button>'
                  for i, (key, label, _) in enumerate(tabs))
sectionHtml = "".join(f'<section role="tabpanel" id="tab-{key}" aria-labelledby="tabbtn-{key}"{"" if i == 0 else " hidden"}>'
                      f'{body}</section>' for i, (key, _, body) in enumerate(tabs))
meta = manifest["meta"]
css = """
:root{--fg:#1b1f24;--mut:#5c6670;--line:#d5dae0;--acc:#0b5cad;--pr:#17633a;--ca:#9a5b00;--soft:#f4f6f8;--hi:#fff4c2}
*{box-sizing:border-box}body{margin:0;font:14px/1.5 system-ui,Segoe UI,Helvetica,Arial,sans-serif;color:var(--fg);background:#fff}
header{padding:16px 24px 10px;border-bottom:1px solid var(--line)}h1{font-size:21px;margin:0 0 6px}
h2{font-size:16px;margin:22px 0 8px}.n{color:var(--mut);font-weight:400;font-size:13px}
.meta{color:var(--mut);font-size:13px;margin:2px 0}code{font:12px ui-monospace,Menlo,Consolas,monospace;background:var(--soft);padding:1px 4px;border-radius:3px}
#sha{font-weight:600;color:var(--fg)}
nav{display:flex;flex-wrap:wrap;gap:4px;padding:10px 24px 0;border-bottom:1px solid var(--line);position:sticky;top:0;background:#fff;z-index:3}
nav button{border:1px solid var(--line);border-bottom:none;background:var(--soft);padding:7px 12px;border-radius:6px 6px 0 0;cursor:pointer;font:inherit}
nav button[aria-selected=true]{background:#fff;border-color:var(--acc);color:var(--acc);font-weight:600}
main{padding:14px 24px 60px;max-width:1500px}section[hidden]{display:none}
.cards{display:grid;grid-template-columns:repeat(auto-fill,minmax(150px,1fr));gap:10px;margin:12px 0}
.card{border:1px solid var(--line);border-radius:6px;padding:8px 12px;text-decoration:none;color:var(--fg);font-size:12.5px}
.card b{font-size:22px;display:block;color:var(--acc)}.card:hover,.card:focus{border-color:var(--acc);outline:none}
.scroll{overflow-x:auto}table{border-collapse:collapse;width:100%;margin:8px 0}
th,td{border:1px solid var(--line);padding:5px 8px;text-align:left;vertical-align:top;font-size:13px}th{background:var(--soft)}
tr.flash td,details.flash{background:var(--hi)}tr[hidden]{display:none}
.st{display:inline-block;padding:1px 7px;border-radius:10px;font-size:11px;font-weight:700;letter-spacing:.3px;color:#fff}
.st.proven{background:var(--pr)}.st.candidate{background:var(--ca)}.st.setup{background:#5b4b9a}
.sev{font-weight:700}.sev.high{color:#a4262c}.sev.medium{color:var(--ca)}.sev.low{color:var(--mut)}
.dir{font-weight:600}.dir.write,.dir.read-write,.dir.delete,.dir.define{color:#a4262c}.dir.control{color:#5b4b9a}
a.cite{font:12px ui-monospace,Menlo,Consolas,monospace;color:var(--acc);text-decoration:underline dotted;overflow-wrap:anywhere}
.sub{color:var(--mut);font-size:12px}
a.xref{color:var(--acc);font-weight:600}
.filters{display:flex;flex-wrap:wrap;gap:10px;align-items:center;background:var(--soft);padding:8px 10px;border-radius:6px;margin:10px 0}
.filters select,.filters input,button{font:inherit;padding:3px 6px}.showing{color:var(--mut);margin-left:auto}
details.block{border:1px solid var(--line);border-radius:6px;padding:6px 10px;margin:8px 0}summary{cursor:pointer}
dl{margin:8px 0}dt{font-weight:600;margin-top:6px}dd{margin:2px 0 0 0}
.diagram{width:100%;max-width:1180px;height:auto;border:1px solid var(--line);border-radius:6px;background:#fff;display:block}
.node{fill:#fff;stroke:#7a8794;stroke-width:1.2}.node.ds{fill:#eef5fc;stroke:#0b5cad}.node.pgm{fill:#e9f5ee;stroke:#17633a;stroke-width:2}
.node.step{fill:#f4f6f8}.node.lm{fill:#fff8e8;stroke:#9a5b00}.node.cpy{fill:#f5f0fb;stroke:#5b4b9a}.node.ext{fill:#f4f6f8}
.node.setup{fill:#f5f0fb;stroke:#5b4b9a;stroke-dasharray:5 3}.frame{fill:none;stroke:#17633a;stroke-dasharray:2 3}
.nt{font:600 12px system-ui,sans-serif;text-anchor:middle;dominant-baseline:middle;fill:#1b1f24}
.ns{font:10.5px system-ui,sans-serif;text-anchor:middle;dominant-baseline:middle;fill:#5c6670}
.ft{font:600 12px system-ui,sans-serif;fill:#17633a}.lg{font:11.5px system-ui,sans-serif;fill:#5c6670}
.ep{stroke:#17633a;stroke-width:1.6}.ec,.es{stroke:#9a5b00;stroke-width:1.6;stroke-dasharray:6 4}.mp{fill:#17633a}.mc{fill:#9a5b00}
.el{font:600 11px system-ui,sans-serif;text-anchor:middle;paint-order:stroke;stroke:#fff;stroke-width:4px}.ept{fill:#17633a}.ect{fill:#9a5b00}
svg a{cursor:pointer}svg a:hover .el{text-decoration:underline}
dialog{border:1px solid var(--line);border-radius:8px;padding:0;max-width:min(1000px,94vw);width:900px}
dialog::backdrop{background:rgba(0,0,0,.35)}.dh{display:flex;justify-content:space-between;align-items:center;padding:8px 12px;border-bottom:1px solid var(--line);background:var(--soft)}
dialog pre{margin:0;padding:10px 12px;font:12px/1.5 ui-monospace,Menlo,Consolas,monospace;overflow:auto;max-height:60vh;white-space:pre}
dialog pre .ln{color:var(--mut);user-select:none;display:inline-block;width:4.5em}
footer{padding:12px 24px;border-top:1px solid var(--line);color:var(--mut);font-size:12.5px}
"""
js = r"""
(function(){
var data=JSON.parse(document.getElementById('data').textContent);
var tabs=[].slice.call(document.querySelectorAll('nav [role=tab]'));
function show(key,focus){tabs.forEach(function(b){var on=b.id==='tabbtn-'+key;b.setAttribute('aria-selected',on?'true':'false');
  b.tabIndex=on?0:-1;document.getElementById(b.getAttribute('aria-controls')).hidden=!on;if(on&&focus)b.focus();});}
tabs.forEach(function(b,i){b.addEventListener('click',function(){var key=b.id.replace('tabbtn-','');show(key);history.replaceState(null,'','#tab-'+key);});
  b.addEventListener('keydown',function(ev){var d=ev.key==='ArrowRight'?1:ev.key==='ArrowLeft'?-1:0;if(!d&&ev.key!=='Home'&&ev.key!=='End')return;
    ev.preventDefault();var j=ev.key==='Home'?0:ev.key==='End'?tabs.length-1:(i+d+tabs.length)%tabs.length;var key=tabs[j].id.replace('tabbtn-','');
    show(key,true);history.replaceState(null,'','#tab-'+key);});});
function go(hash){var id=decodeURIComponent(hash.replace(/^#/,''));if(!id||id==='src')return;var el=document.getElementById(id);if(!el)return;
  var panel=el.closest('section[role=tabpanel]');if(panel)show(panel.id.replace('tab-',''));
  if(el===panel){window.scrollTo(0,0);return;}
  if(el.tagName==='TR'&&el.hidden){var f=panel.querySelector('.filters');if(f)clear(f);}
  for(var p=el;p;p=p.parentElement){if(p.tagName==='DETAILS')p.open=true;}
  el.scrollIntoView({block:'center'});el.classList.add('flash');setTimeout(function(){el.classList.remove('flash');},1800);}
var dlg=document.getElementById('src');
function openCite(ref){var ex=data.excerpts[ref];if(!ex)return;document.getElementById('src-title').textContent=ref;
  var pre=document.getElementById('src-body');pre.textContent='';ex.lines.forEach(function(line,i){var s=document.createElement('span');
    s.className='ln';s.textContent=(ex.first+i);pre.appendChild(s);pre.appendChild(document.createTextNode(line+'\n'));});
  if(dlg.open)dlg.close();dlg.showModal();}
document.getElementById('src-close').addEventListener('click',function(){dlg.close();});
document.addEventListener('click',function(ev){var a=ev.target.closest&&ev.target.closest('a');if(!a)return;
  if(a.classList.contains('cite')){ev.preventDefault();openCite(a.getAttribute('data-cite'));return;}
  var href=a.getAttribute('href')||a.getAttribute('xlink:href')||'';if(href.charAt(0)==='#'){ev.preventDefault();
    if(dlg.open)dlg.close();history.replaceState(null,'',href);go(href);}});
function apply(box){var name=box.getAttribute('data-for');var rows=[].slice.call(document.querySelectorAll('table[data-rows="'+name+'"] tbody tr'));
  var want={};[].forEach.call(box.querySelectorAll('[data-filter]'),function(c){want[c.getAttribute('data-filter')]=c.value;});var shown=0;
  rows.forEach(function(r){var ok=true;for(var k in want){if(!want[k])continue;
      if(k==='text'){if(r.textContent.toLowerCase().indexOf(want[k].toLowerCase())<0)ok=false;}
      else if(r.getAttribute('data-'+k)!==want[k])ok=false;}
    r.hidden=!ok;if(ok)shown++;});
  var out=document.querySelector('[data-showing="'+name+'"]');out.textContent='showing '+shown+' of '+rows.length;
  out.setAttribute('data-shown',shown);out.setAttribute('data-total',rows.length);}
function clear(box){[].forEach.call(box.querySelectorAll('[data-filter]'),function(c){c.value='';});apply(box);}
[].forEach.call(document.querySelectorAll('.filters'),function(box){
  [].forEach.call(box.querySelectorAll('[data-filter]'),function(c){c.addEventListener('input',function(){apply(box);});c.addEventListener('change',function(){apply(box);});});
  box.querySelector('[data-clear]').addEventListener('click',function(){clear(box);});apply(box);});
[].forEach.call(document.querySelectorAll('[data-expand],[data-collapse]'),function(b){b.addEventListener('click',function(){
  var open=b.hasAttribute('data-expand');var root=document.querySelector(b.getAttribute(open?'data-expand':'data-collapse'));
  [].forEach.call(root.querySelectorAll('details'),function(d){d.open=open;});});});
window.addEventListener('hashchange',function(){go(location.hash);});
if(location.hash)go(location.hash);
})();
"""
dataJson = json.dumps(manifest, separators=(",", ":")).replace("</", "<\\/")
page = f"""<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Interest stream - INTCALC</title><style>{css}</style></head><body>
<header><h1>Interest stream - INTCALC</h1>
<p class="meta">Repository <code>{esc(meta["repository"])}</code> - root job <code>{esc(meta["root"]["jcl"])}</code> -
Control-M folder <code>{esc(meta["root"]["schedulerFolder"])}</code> in <code>{esc(meta["root"]["schedulerExport"])}</code></p>
<p class="meta">Baseline: branch <code>{esc(meta["baseline"]["branch"])}</code> at commit <code id="sha">{esc(meta["baseline"]["sha"])}</code>.
Data: <code>modernization/streams/interest/manifest.json</code>, built by <code>{esc(meta["generatedBy"])}</code> from the mftb store and the sources.</p>
<p class="meta">{badge("PROVEN")} static and cited &nbsp; {badge("CANDIDATE")} dynamic or inferred &nbsp; <span class="st setup">SETUP</span> needed before a run, not stream logic</p>
</header>
<nav role="tablist" aria-label="Sections">{navHtml}</nav>
<main>{sectionHtml}</main>
<footer>Self-contained page: no external script, style, font or image. The cited source lines are stored in the page ({counts["citations"]} excerpts).</footer>
<dialog id="src" aria-labelledby="src-title"><div class="dh"><code id="src-title"></code><button type="button" id="src-close">Close</button></div><pre id="src-body"></pre></dialog>
<script id="data" type="application/json">{dataJson}</script>
<script>{js}</script></body></html>
"""
(outDir / "stream-INTCALC.html").write_text(page)
print("stream-INTCALC.html", len(page), "bytes")
