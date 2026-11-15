"""strategy-interest.html: one self-contained page (no external script, style, font or image)."""
from __future__ import annotations

import html
import json
import re

esc = html.escape

linkPatterns = [
    (re.compile(r"\bINT-[QH]\d\d\b"), "dec-"), (re.compile(r"\bREL-\d\d\b"), "rel-"), (re.compile(r"\bG\d\d\b"), "gap-"),
    (re.compile(r"\b(?:TL|RC|SS|CO|FB)\d\b"), "item-"), (re.compile(r"\bM\d\d\b"), "move-"), (re.compile(r"\bFI-\d\b"), "fi-"),
    (re.compile(r"\bE[1-6]\b"), "enc-"),
]
ticketPattern = re.compile(r"\b(tickets? )(\d\d)((?:(?:, | and |, and )\d\d)*)\b")
combined = re.compile("|".join(f"(?:{p.pattern})" for p, _ in linkPatterns) + "|" + ticketPattern.pattern)


def linked(text: str, skip: str = "") -> str:
    """Escape text and turn the ids it mentions into in-page links (not the row's own id)."""
    out, pos = [], 0
    for m in combined.finditer(text):
        out.append(esc(text[pos:m.start()]))
        token = m.group(0)
        if token.lower().startswith("ticket"):
            word, rest = token.split(" ", 1)
            out.append(esc(word) + " " + re.sub(r"\d\d", lambda n: f'<a class="xref" href="#ticket-{n.group(0)}">{n.group(0)}</a>', esc(rest)))
        elif token == skip:
            out.append(esc(token))
        else:
            prefix = next(pre for p, pre in linkPatterns if p.fullmatch(token))
            out.append(f'<a class="xref" href="#{prefix}{token}">{esc(token)}</a>')
        pos = m.end()
    out.append(esc(text[pos:]))
    return "".join(out)


def proposed() -> str:
    return '<span class="st proposed">PROPOSED</span>'


def tableHtml(name, header, rows, attrs=None):
    """rows: list of (rowId, dataAttrs dict, [cell html])."""
    head = "".join(f"<th>{esc(h)}</th>" for h in header)
    body = []
    for rowId, data, cells in rows:
        dataAttrs = "".join(f' data-{k}="{esc(str(v))}"' for k, v in (data or {}).items())
        idAttr = f' id="{esc(rowId)}"' if rowId else ""
        body.append(f"<tr{idAttr}{dataAttrs}>" + "".join(f"<td>{c}</td>" for c in cells) + "</tr>")
    return f'<div class="scroll"><table data-rows="{name}"><thead><tr>{head}</tr></thead><tbody>{"".join(body)}</tbody></table></div>'


def filters(name, selects, placeholder="search this table"):
    parts = [f'<div class="filters" data-for="{name}">']
    for key, label, options in selects:
        opts = '<option value="">all</option>' + "".join(f'<option value="{esc(o)}">{esc(o)}</option>' for o in options)
        parts.append(f'<label>{esc(label)} <select data-filter="{key}">{opts}</select></label>')
    parts.append(f'<label>Search <input type="search" data-filter="text" placeholder="{esc(placeholder)}"></label>')
    parts.append(f'<button type="button" data-clear>Clear</button><span class="showing" data-showing="{name}"></span></div>')
    return "".join(parts)


# ------------------------------------------------------------------ diagrams
class Svg:
    def __init__(self, svgId, width, height, title):
        self.parts = [f'<svg id="{svgId}" class="diagram" viewBox="0 0 {width} {height}" role="img" aria-label="{esc(title)}">'
                      '<defs><marker id="' + svgId + '-arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">'
                      '<path d="M0,0L10,5L0,10z" class="mk"/></marker></defs>']
        self.svgId, self.labels = svgId, []

    def lane(self, x, y, w, h, title, cls=""):
        self.parts.append(f'<rect class="lane {cls}" x="{x}" y="{y}" width="{w}" height="{h}" rx="8"/><text class="lt" x="{x + 10}" y="{y + 17}">{esc(title)}</text>')

    def box(self, x, y, w, h, title, sub="", cls="", href=None):
        inner = f'<rect class="node {cls}" x="{x}" y="{y}" width="{w}" height="{h}" rx="6"/>'
        if sub:
            inner += f'<text class="nt" x="{x + w / 2}" y="{y + h / 2 - 7}">{esc(title)}</text><text class="ns" x="{x + w / 2}" y="{y + h / 2 + 9}">{esc(sub)}</text>'
        else:
            inner += f'<text class="nt" x="{x + w / 2}" y="{y + h / 2}">{esc(title)}</text>'
        self.parts.append(f'<a href="{href}" data-node="{esc(title)}">{inner}</a>' if href else f'<g data-node="{esc(title)}">{inner}</g>')

    def edge(self, points, label="", href=None, dashed=False, labelAt=None):
        d = "M" + " L".join(f"{x},{y}" for x, y in points)
        self.parts.append(f'<path class="ed{" dash" if dashed else ""}" d="{d}" marker-end="url(#{self.svgId}-arrow)" data-edge="{esc(label)}"/>')
        if label:
            (x1, y1), (x2, y2) = points[0], points[1]
            lx, ly = labelAt or ((x1 + x2) / 2, (y1 + y2) / 2 - 6)
            text = f'<text class="el" x="{lx}" y="{ly}">{esc(label)}</text>'
            self.labels.append(f'<a href="{href}">{text}</a>' if href else text)

    def text(self, x, y, value, cls="lg"):
        self.parts.append(f'<text class="{cls}" x="{x}" y="{y}">{esc(value)}</text>')

    def done(self):
        return "".join(self.parts) + "".join(self.labels) + "</svg>"


moveShort = {
    "M01": ("INTCALC.STEP15 / CBACT04C", "interestCalcJob / calcInterestStep"), "M02": ("TCATBALF balances", "tran_cat_balance"),
    "M03": ("DISCGRP rates", "disclosure_group"), "M04": ("CARDXREF + alternate index", "card_xref + index"),
    "M05": ("ACCTDATA accounts (rewritten)", "account (3 columns, written back)"), "M06": ("SYSTRAN(+1) transactions", "interest_transaction (exported)"),
    "M07": ("PARM='2022071800'", "job parameter parmDate"), "M08": ("system clock", "injected clock"),
    "M09": ("DISPLAY on SYSOUT", "run log, same lines"), "M10": ("CEE3ABD abend 999", "FAILED + exit code"),
    "M11": ("Control-M folder, job INTCALC", ""), "M12": ("CLOSEFIL, COMBTRAN, WAITSTEP, OPENFIL", ""),
    "M13": ("setup jobs (5)", ""), "M14": ("other users of the files", ""),
}


def moveDiagram(design):
    rows = [m for m in design["moveMap"] if m["disposition"] != "STAYS"]
    stays = [m for m in design["moveMap"] if m["disposition"] == "STAYS"]
    top, rowH = 34, 42
    height = top + rowH * len(rows) + 110
    s = Svg("diagram-move", 1180, height, "Move map: what moves, what is bridged, what stays")
    s.lane(10, 6, 470, top + rowH * len(rows) + 4, "Mainframe today")
    s.lane(700, 6, 470, top + rowH * len(rows) + 4, "After the move (Java 21, Spring Batch, PostgreSQL 15)", "tgt")
    for i, m in enumerate(rows):
        y = top + i * rowH
        left, right = moveShort[m["id"]]
        cls = "mv" if m["disposition"] == "MOVES" else "br"
        s.box(30, y, 430, 30, left, cls="leg", href=f"#move-{m['id']}")
        s.box(720, y, 430, 30, right, cls=cls, href=f"#move-{m['id']}")
        s.edge([(460, y + 15), (720, y + 15)], f"{m['id']} {m['disposition']}", href=f"#move-{m['id']}", dashed=m["disposition"] == "BRIDGED")
    y = top + rowH * len(rows) + 22
    s.lane(10, y, 1160, 78, "Stays on the mainframe, unchanged", "stay")
    w = 1120 / len(stays)
    for i, m in enumerate(stays):
        s.box(30 + i * w, y + 30, w - 14, 34, m["id"], moveShort[m["id"]][0], cls="leg", href=f"#move-{m['id']}")
    return s.done()


def flowDiagram():
    s = Svg("diagram-flow", 1180, 430, "One run inside the scheduler window")
    s.lane(10, 6, 1160, 108, "Mainframe - stays (Control-M folder MONTHLY-InterestCalculation)")
    s.lane(10, 124, 1160, 108, "Target - Spring Batch job interestCalcJob", "tgt")
    s.lane(10, 242, 1160, 150, "PostgreSQL 15 - schema carddemo", "db")
    mf = [("CLOSEFIL", "closes CICS files", 30), ("unload 4 files", "IDCAMS REPRO", 190), ("write back", "ACCTDATA, SYSTRAN(+1)", 640), ("COMBTRAN", "reads SYSTRAN(0)", 810), ("WAITSTEP", "", 960), ("OPENFIL", "", 1060)]
    widths = {"CLOSEFIL": 140, "unload 4 files": 150, "write back": 150, "COMBTRAN": 130, "WAITSTEP": 85, "OPENFIL": 85}
    for title, sub, x in mf:
        s.box(x, 40, widths[title], 52, title, sub, cls="leg")
    s.edge([(170, 66), (190, 66)])
    s.edge([(790, 66), (810, 66)])
    s.edge([(940, 66), (960, 66)])
    s.edge([(1045, 66), (1060, 66)])
    tg = [("loadExtractsStep", "interest-loader", 190, "#comp-interest-loader"), ("calcInterestStep", "interest-calc", 415, "#comp-interest-calc"), ("exportDatasetsStep", "interest-export", 640, "#comp-interest-export")]
    for title, sub, x, href in tg:
        s.box(x, 158, 150, 52, title, sub, cls="mv", href=href)
    s.edge([(340, 184), (415, 184)])
    s.edge([(565, 184), (640, 184)])
    s.edge([(265, 92), (265, 158)], "extract files", labelAt=(312, 140))
    s.edge([(715, 158), (715, 92)], "SYSTRAN + ACCTDATA image", labelAt=(800, 140))
    s.box(110, 300, 380, 62, "account, card_xref, tran_cat_balance, disclosure_group", "replicas - refreshed for every run", cls="br", href="#table-account")
    s.box(620, 300, 330, 62, "interest_transaction, interest_run", "owned by the target", cls="mv", href="#table-interestTransaction")
    s.edge([(240, 210), (240, 300)], "truncate + load", labelAt=(180, 290))
    s.edge([(440, 300), (460, 210)], "read", labelAt=(424, 262))
    s.edge([(510, 210), (480, 300)], "update 3 columns", labelAt=(552, 290), dashed=True)
    s.edge([(540, 210), (650, 300)], "insert", labelAt=(618, 262))
    s.edge([(760, 300), (740, 210)], "read", labelAt=(776, 262))
    s.text(30, 414, "Solid arrow: order or data flow. Dashed arrow: the only write to a replica table by the calculation.")
    return s.done()


def ticketDiagram(tickets, td):
    s = Svg("diagram-tickets", 1180, 372, "Ticket dependency order")
    lanes = [(20, 210, "Start in parallel", "tgt"), (250, 210, "After the pilot", ""), (480, 440, "Wait on the schema", ""), (940, 220, "Final gates", "gate")]
    for x, w, title, cls in lanes:
        s.lane(x, 6, w, 340, title, cls)
    boxW, boxH = 170, 44
    pos = {1: (40, 215), 2: (40, 40), 3: (270, 180), 4: (270, 250), 5: (500, 40), 7: (500, 110), 6: (500, 250), 8: (730, 296), 9: (965, 250), 10: (965, 75)}
    for t in tickets:
        x, y = pos[t["n"]]
        cls = {"pilot": "mv", "schema": "br", "gate": "gt"}.get(t["group"], "leg")
        s.box(x, y, boxW, boxH, f"{t['id']} {t['slug']}", "PILOT" if t["group"] == "pilot" else ("final gate" if t["group"] == "gate" else ""), cls=cls, href=f"#ticket-{t['id']}")
    for t in tickets:
        for d in t["deps"]:
            (x1, y1), (x2, y2) = pos[d], pos[t["n"]]
            if (d, t["n"]) == (2, 6):  # around the second column, into the bottom of 06
                points = [(x1 + boxW, y1 + 30), (240, y1 + 30), (240, 322), (x2 + 60, 322), (x2 + 60, y2 + boxH)]
            else:
                points = [(x1 + boxW, y1 + boxH / 2), (x2, y2 + boxH / 2)]
            s.edge(points, dashed=(d == 2))
    s.text(20, 364, "Arrow: must be merged first. Dashed arrow: waits on the schema (ticket 02).")
    return s.done()


css = """
:root{--fg:#1b1f24;--mut:#5c6670;--line:#d5dbe1;--soft:#f4f6f8;--acc:#0b5cad;--hi:#fff3bf;--mv:#17633a;--br:#9a5b00;--sy:#5b4b9a}
*{box-sizing:border-box}body{margin:0;font:14.5px/1.5 system-ui,-apple-system,Segoe UI,Roboto,sans-serif;color:var(--fg);background:#fff}
header{padding:16px 24px 10px;border-bottom:1px solid var(--line)}h1{margin:0 0 6px;font-size:22px}h2{font-size:18px;margin:22px 0 6px}h3{font-size:15px;margin:16px 0 4px}
.meta{margin:2px 0;color:var(--mut);font-size:13px}code{font:12.5px ui-monospace,Menlo,Consolas,monospace;background:var(--soft);padding:1px 4px;border-radius:3px;overflow-wrap:anywhere}
.notice{background:#fff8e8;border:1px solid #e6c98a;border-radius:6px;padding:8px 12px;margin:10px 0 2px;font-size:13.5px}
nav{position:sticky;top:0;z-index:5;background:#fff;border-bottom:1px solid var(--line);padding:6px 20px;display:flex;flex-wrap:wrap;gap:4px}
nav button{font:inherit;border:1px solid transparent;background:none;padding:6px 12px;border-radius:6px;cursor:pointer;color:var(--fg)}
nav button[aria-selected=true]{background:var(--acc);color:#fff}nav button:focus-visible{outline:2px solid var(--acc);outline-offset:1px}
main{padding:6px 24px 30px;max-width:1280px}section[hidden]{display:none}.lead{max-width:900px}
.cards{display:grid;grid-template-columns:repeat(auto-fill,minmax(160px,1fr));gap:10px;margin:12px 0}
.card{border:1px solid var(--line);border-radius:6px;padding:8px 12px;text-decoration:none;color:var(--fg);font-size:12.5px}
.card b{font-size:22px;display:block;color:var(--acc)}.card:hover,.card:focus{border-color:var(--acc);outline:none}
.scroll{overflow-x:auto}table{border-collapse:collapse;width:100%;margin:8px 0}
th,td{border:1px solid var(--line);padding:5px 8px;text-align:left;vertical-align:top;font-size:13px}th{background:var(--soft)}
tr.flash td,details.flash,li.flash{background:var(--hi)}tr[hidden]{display:none}
.st{display:inline-block;padding:1px 7px;border-radius:10px;font-size:11px;font-weight:700;letter-spacing:.3px;color:#fff;white-space:nowrap}
.st.proposed{background:#8a6d00}.st.open{background:#a4262c}.st.MOVES{background:var(--mv)}.st.BRIDGED{background:var(--br)}.st.STAYS{background:var(--sy)}
.st.pilot{background:var(--mv)}.st.gate{background:#a4262c}.st.grp{background:#5c6670}
a.xref{color:var(--acc);font-weight:600}.sub{color:var(--mut);font-size:12px}.own{white-space:nowrap}
.filters{display:flex;flex-wrap:wrap;gap:10px;align-items:center;background:var(--soft);padding:8px 10px;border-radius:6px;margin:10px 0}
.filters select,.filters input,button{font:inherit;padding:3px 6px}.showing{color:var(--mut);margin-left:auto}
details.block{border:1px solid var(--line);border-radius:6px;padding:6px 10px;margin:8px 0}summary{cursor:pointer;font-weight:600}
details.block ul{margin:4px 0 6px 0;padding-left:20px}dl{margin:8px 0}dt{font-weight:600;margin-top:6px}dd{margin:2px 0 0 0}
.diagram{width:100%;max-width:1180px;height:auto;border:1px solid var(--line);border-radius:6px;background:#fff;display:block;margin:8px 0}
.lane{fill:#fafbfc;stroke:#c5ccd3;stroke-dasharray:3 3}.lane.tgt{fill:#f1f8f3;stroke:#17633a}.lane.db{fill:#eef5fc;stroke:#0b5cad}.lane.stay{fill:#f5f0fb;stroke:#5b4b9a}.lane.gate{fill:#fdf1f1;stroke:#a4262c}
.lt{font:600 12px system-ui,sans-serif;fill:#5c6670}.node{fill:#fff;stroke:#7a8794;stroke-width:1.2}.node.leg{fill:#f4f6f8}
.node.mv{fill:#e9f5ee;stroke:#17633a;stroke-width:1.8}.node.br{fill:#fff8e8;stroke:#9a5b00;stroke-width:1.6}.node.gt{fill:#fdeaea;stroke:#a4262c;stroke-width:1.8}
.nt{font:600 12px system-ui,sans-serif;text-anchor:middle;dominant-baseline:middle;fill:#1b1f24}
.ns{font:10.5px system-ui,sans-serif;text-anchor:middle;dominant-baseline:middle;fill:#5c6670}.lg{font:11.5px system-ui,sans-serif;fill:#5c6670}
.ed{stroke:#44505c;stroke-width:1.5;fill:none}.ed.dash{stroke-dasharray:6 4;stroke:#9a5b00}.mk{fill:#44505c}
.el{font:600 11px system-ui,sans-serif;text-anchor:middle;fill:#1b1f24;paint-order:stroke;stroke:#fff;stroke-width:4px}
svg a{cursor:pointer}svg a:hover .nt,svg a:hover .el{text-decoration:underline}svg a:focus{outline:none}svg a:focus .node{stroke-width:3}
footer{padding:12px 24px;border-top:1px solid var(--line);color:var(--mut);font-size:12.5px}
"""

js = r"""
(function(){
var tabs=[].slice.call(document.querySelectorAll('nav [role=tab]'));
function show(key,focus){tabs.forEach(function(b){var on=b.id==='tabbtn-'+key;b.setAttribute('aria-selected',on?'true':'false');
  b.tabIndex=on?0:-1;document.getElementById(b.getAttribute('aria-controls')).hidden=!on;if(on&&focus)b.focus();});}
tabs.forEach(function(b,i){b.addEventListener('click',function(){var key=b.id.replace('tabbtn-','');show(key);history.replaceState(null,'','#tab-'+key);});
  b.addEventListener('keydown',function(ev){var d=ev.key==='ArrowRight'?1:ev.key==='ArrowLeft'?-1:0;if(!d&&ev.key!=='Home'&&ev.key!=='End')return;
    ev.preventDefault();var j=ev.key==='Home'?0:ev.key==='End'?tabs.length-1:(i+d+tabs.length)%tabs.length;var key=tabs[j].id.replace('tabbtn-','');
    show(key,true);history.replaceState(null,'','#tab-'+key);});});
function clear(box){[].forEach.call(box.querySelectorAll('[data-filter]'),function(c){c.value='';});apply(box);}
function go(hash){var id=decodeURIComponent(hash.replace(/^#/,''));if(!id)return;var el=document.getElementById(id);if(!el)return;
  var panel=el.closest('section[role=tabpanel]');if(panel)show(panel.id.replace('tab-',''));
  if(el===panel){window.scrollTo(0,0);return;}
  if(el.tagName==='TR'&&el.hidden){var name=el.closest('table').getAttribute('data-rows');var f=document.querySelector('.filters[data-for="'+name+'"]');if(f)clear(f);}
  for(var p=el;p;p=p.parentElement){if(p.tagName==='DETAILS')p.open=true;}
  el.scrollIntoView({block:'center'});el.classList.add('flash');setTimeout(function(){el.classList.remove('flash');},1800);}
document.addEventListener('click',function(ev){var a=ev.target.closest&&ev.target.closest('a');if(!a)return;
  var href=a.getAttribute('href')||'';if(href.charAt(0)==='#'){ev.preventDefault();history.replaceState(null,'',href);go(href);}});
function apply(box){var name=box.getAttribute('data-for');var rows=[].slice.call(document.querySelectorAll('table[data-rows="'+name+'"] tbody tr'));
  var want={};[].forEach.call(box.querySelectorAll('[data-filter]'),function(c){want[c.getAttribute('data-filter')]=c.value;});var shown=0;
  rows.forEach(function(r){var ok=true;for(var k in want){if(!want[k])continue;
      if(k==='text'){if(r.textContent.toLowerCase().indexOf(want[k].toLowerCase())<0)ok=false;}
      else if(r.getAttribute('data-'+k)!==want[k])ok=false;}
    r.hidden=!ok;if(ok)shown++;});
  var out=document.querySelector('[data-showing="'+name+'"]');out.textContent='showing '+shown+' of '+rows.length;
  out.setAttribute('data-shown',shown);out.setAttribute('data-total',rows.length);}
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


def render(design, tickets, outputs, dd, td):
    m, c = design["meta"], design["counts"]
    owner = lambda role: f'<span class="own">{esc(role)}</span>'  # noqa: E731
    badge = lambda value: f'<span class="st {value}">{value}</span>'  # noqa: E731
    expandBar = lambda sel: (f'<p><button type="button" data-expand="{sel}">Expand all</button> '  # noqa: E731
                             f'<button type="button" data-collapse="{sel}">Collapse all</button></p>')
    sections = []

    # ---- overview
    cards = [("moveMap", "move-map rows", "#tab-movemap"), ("moves", "parts that move", "#tab-movemap"), ("bridged", "parts bridged", "#tab-movemap"),
             ("stays", "parts that stay", "#tab-movemap"), ("components", "target components", "#components"), ("tables", "tables and table sets", "#ownership"),
             ("buildSteps", "build steps", "#tab-build"), ("tickets", "tickets", "#tab-tickets"), ("gateTickets", "final gates", "#tab-tickets"),
             ("dataItems", "data-move items", "#tab-data"), ("fixtureCases", "recorded cases reconciled", "#recon-cases"), ("fields", "fields mapped", "#tab-datamap"),
             ("gaps", "mapping gaps", "#gaps"), ("registerOpen", "business decisions OPEN", "#register"), ("releaseDecisions", "release decisions OPEN", "#release"),
             ("ownerRoles", "owner roles to confirm", "#owners"), ("outsideUsers", "other uses of the same files", "#move-M14")]
    cardHtml = "".join(f'<a class="card" href="{href}" data-count="{key}"><b>{c[key]}</b>{esc(label)}</a>' for key, label, href in cards)
    ownersTable = tableHtml("owners", ["Owner role", "Move-map rows", "Build steps", "Data-move items", "Release decisions", "Mapping gaps", "Total"],
                            [(None, {"role": o["role"]}, [owner(o["role"]), o["moveMap"], o["buildSteps"], o["dataMoves"], o["releaseDecisions"], o["gaps"], f"<b>{o['total']}</b>"]) for o in design["owners"]])
    sections.append(("overview", "Overview", f"""
<h2>In short</h2>
<p class="lead">Once a month the mainframe job <code>INTCALC</code> charges interest on card accounts. This plan replaces that one job with a
Java 21 / Spring Batch job on PostgreSQL 15 and leaves everything around it where it is. Other mainframe jobs and the online screens keep
using the same files, so the new job gets a fresh copy of four files before each run and hands back two results: the new interest
transactions and the updated account balances. The new job first has to produce exactly what the old one produces, known oddities
included, on {c['fixtureCases']} recorded cases and then on real cycles run side by side, before it takes over.</p>
<p class="lead">Every item on this page is {proposed()} and names the role that has to confirm it. Open business questions are not settled here:
they stay in the decision register and are built as the mainframe behaves today.</p>
<div class="cards" id="cards">{cardHtml}</div>
<h2>What moves, at a glance</h2>
<p class="sub">Click a box or a label to open its row in the move map. Solid arrow: rebuilt in the target. Dashed arrow: stays on the mainframe and is copied for each run.</p>
{moveDiagram(design)}
<h2 id="owners">Who has to confirm what</h2>
<p class="lead">{c['ownerRoles']} roles. The numbers are the items on this page that carry the role.</p>{ownersTable}
"""))

    # ---- move map
    moveRows = [(f"move-{x['id']}", {"disposition": x["disposition"], "owner": x["owner"]},
                 [f"<b>{x['id']}</b>", linked(x["legacy"]), linked(x["target"]), badge(x["disposition"]), linked(x["note"]), proposed(), owner(x["owner"])]) for x in design["moveMap"]]
    sections.append(("movemap", "Move map", f"""
<h2>Move map</h2>
<p class="lead"><b>MOVES</b>: rebuilt in the target. <b>BRIDGED</b>: stays on the mainframe and is copied to and from the target for each run.
<b>STAYS</b>: not touched. {c['moveMap']} rows: {c['moves']} move, {c['bridged']} are bridged, {c['stays']} stay.</p>
{filters("movemap", [("disposition", "Disposition", ["MOVES", "BRIDGED", "STAYS"]), ("owner", "Owner", sorted({x["owner"] for x in design["moveMap"]}))])}
{tableHtml("movemap", ["Id", "Today", "After the move", "Disposition", "Note", "Status", "Owner to confirm"], moveRows)}
"""))

    # ---- architecture
    compRows = [(f"comp-{x['id']}", None, [f"<code>{esc(x['id'])}</code>", esc(x["step"]), esc(x["role"]), esc(x["writes"]), esc(x["reads"]), "yes" if x["coexistenceOnly"] else "no"]) for x in dd.components]
    tableRows = [(f"table-{t['id']}", {"kind": t["kind"]}, [f"<code>{esc(t['name'])}</code>", esc(t["copybook"] or "-"), esc(t["legacy"]), esc(t["kind"]), esc(t["systemOfRecord"]), esc(t["writer"]), esc(t["readers"])]) for t in dd.tables]
    switchRows = [(None, None, [linked(s["decision"]), f"<code>{esc(s['property'])}</code>", esc(s["default"]), esc(s["other"]), esc(s["where"])]) for s in dd.policySwitches]
    fiRows = [(f"fi-{f['id']}", None, [f"<b>{f['id']}</b>", esc(f["what"]), esc(f["where"]), linked(f["frozenBy"])]) for f in dd.frozenInterfaces]
    sections.append(("architecture", "Target and coexistence", f"""
<h2>One run inside the scheduler window</h2>
<p class="lead">The files stay the interface. Between CLOSEFIL and COMBTRAN the mainframe unloads four files, the new job loads them,
computes the interest and exports two results, and the mainframe writes them back. Nothing else in the folder changes.
Full text: <code>modernization/design/architecture.md</code>, version {esc(m['architectureVersion'])}.</p>
{flowDiagram()}
<h2 id="components">Components ({c['components']})</h2>
{tableHtml("components", ["Component", "Step", "What it does", "Writes", "Reads", "Only while neighbours stay"], compRows)}
<h2 id="ownership">Which component owns which table ({c['tables']})</h2>
<p class="lead">One writer per table. A <b>replica</b> is a copy of a mainframe file, valid from the load to the write-back of the same run.</p>
{filters("ownership", [("kind", "Kind", ["replica", "owned", "framework"])])}
{tableHtml("ownership", ["Table", "Layout", "Legacy dataset", "Kind", "System of record", "Who may write", "Who reads"], tableRows)}
<div id="arch-details">{expandBar('#arch-details')}
<details class="block" id="boundaries"><summary>Job, transaction and restart boundaries</summary>
<ul><li><b>Job:</b> one job, <code>interestCalcJob</code>, replaces <code>INTCALC.STEP15</code>; steps load, calculate, export. Every launch is a new run (INT-H02 as coded: no guard against a second run).</li>
<li><b>Transaction:</b> the load commits one table at a time; the calculation commits whole accounts - an account's interest transactions and its balance update together, never split; the export moves finished files into place.</li>
<li><b>Restart:</b> load and export are repeated; the calculation continues after the last committed account and ends with the same rows as an uninterrupted run ({linked('REL-11')}). The mainframe job has no restart.</li>
<li><b>Last account:</b> gets its transactions but, as coded, no balance update ({linked('INT-Q04')}).</li></ul></details>
<details class="block" id="coexistence"><summary>Rules of coexistence with the jobs that stay</summary>
<ul><li>The mainframe files stay the system of record ({linked('REL-02')}); the replica tables are replaced before every run.</li>
<li>The run happens between CLOSEFIL and OPENFIL, so the online programs that rewrite ACCTDATA are shut out as today.</li>
<li>Between unload and write-back no mainframe job may write ACCTDATA; the write-back replaces a record only if it still equals the unloaded image. The order against POSTTRAN is not in the scheduler exports ({linked('INT-H06')}).</li>
<li>Only the records of updated accounts are written back ({linked('REL-03')}); TCATBALF, DISCGRP and CARDXREF never are.</li>
<li>The exported transactions are cataloged as SYSTRAN(+1), so COMBTRAN reads them as today ({linked('REL-09')}).</li></ul></details>
<details class="block" id="switches"><summary>Policy switches: one per open decision ({c['policySwitches']})</summary>
<p>Each open decision is built as the mainframe behaves today, behind one switch. Changing an answer later is a change in one place.</p>
{tableHtml("switches", ["Decision", "Switch", "Default (legacy behaviour)", "Other values", "Where it applies"], switchRows)}</details>
<details class="block" id="frozen"><summary>Frozen shared interfaces ({c['frozenInterfaces']})</summary>
{tableHtml("frozen", ["Id", "Interface", "Where", "Frozen by"], fiRows)}</details></div>
"""))

    # ---- build steps
    stepBlocks = []
    for b, t in zip(design["buildSteps"], tickets):
        after = ", ".join(f'<a class="xref" href="#ticket-{a}">{a}</a>' for a in b["after"]) or "nothing"
        tag = '<span class="st pilot">PILOT</span> ' if t["group"] == "pilot" else ('<span class="st gate">FINAL GATE</span> ' if t["group"] == "gate" else "")
        stepBlocks.append(f'<details class="block" id="step-{b["step"]}" data-step="{b["step"]}"><summary>Step {b["step"]}: {esc(b["title"])} {tag}{proposed()}</summary>'
                          f'<p>{esc(b["outcome"])}</p><p class="sub">Ticket <a class="xref" href="#ticket-{b["ticket"]}">{b["ticket"]}</a> - starts after: {after} - '
                          f'owner to confirm: {owner(b["owner"])} - {esc(td.groups[b["group"]])}</p></details>')
    sections.append(("build", "Build steps", f"""
<h2>Build steps in order ({c['buildSteps']})</h2>
<p class="lead">Each step is one ticket. Step 1 is the pilot: a thin slice from input to output that can already be compared on one recorded case.
Steps 9 and 10 are gates: they prove the result and build nothing new.</p>
<div id="steps">{expandBar('#steps')}{"".join(stepBlocks)}</div>
"""))

    # ---- moving the data
    phaseBlocks = []
    for p in dd.dataMoves:
        rows = [(f"item-{i['id']}", {"phase": p["title"], "owner": i["owner"]}, [f"<b>{i['id']}</b>", linked(i["text"], skip=i["id"]), proposed(), owner(i["owner"])]) for i in p["items"]]
        extra = ""
        if p["phase"] == "recon":
            reconRows = [(f"recon-{r['case']}", None, [f"<code>{r['case']}</code>", r["accounts"], r["cards"], r["balances"], r["rates"], r["sumCurrBalBefore"], r["sumCatBal"], r["sumRates"],
                                                         r["transactions"], r["sumTranAmt"], r["sumCurrBalAfter"], r["balanceChange"], f"{r['lastAccountInterest']} <span class='sub'>({r['lastAccount']})</span>",
                                                         "holds" if r["rc5Holds"] else "FAILS"]) for r in design["reconciliation"]]
            extra = (f'<h3 id="recon-cases">The figures on the {c["fixtureCases"]} recorded cases</h3><p class="sub">Computed from <code>fixtures/interest/*/input</code> and <code>expected</code> '
                     'with the estate\'s copybook decoder. Last column: the sum of the transactions equals the balance change plus the interest of the last account (RC5).</p>'
                     + tableHtml("recon", ["Case", "Accounts", "Cards", "Balances", "Rates", "Sum ACCT-CURR-BAL before", "Sum TRAN-CAT-BAL", "Sum DIS-INT-RATE", "Transactions written",
                                           "Sum TRAN-AMT", "Sum ACCT-CURR-BAL after", "Balance change", "Interest of last account (not added)", "RC5"], reconRows))
        phaseBlocks.append(f'<details class="block" open id="phase-{p["phase"]}" data-phase="{p["phase"]}"><summary>{esc(p["title"])} ({len(p["items"])})</summary><p>{esc(p["intro"])}</p>'
                           + tableHtml("datamoves", ["Id", "Item", "Status", "Owner to confirm"], rows) + extra + "</details>")
    sections.append(("data", "Moving the data", f"""
<h2>Moving the data ({c['dataItems']} items in {c['phases']} parts)</h2>
<p class="lead">No data moves for good in this stream: the mainframe files stay the master copy. "Moving the data" here means copying four files
in before a run and two results back after it, proving the copy is exact, and knowing how to step back.</p>
{filters("datamoves", [("phase", "Part", [p["title"] for p in dd.dataMoves]), ("owner", "Owner", sorted({i["owner"] for i in design["dataItems"]}))], "search the items")}
<div id="phases">{expandBar('#phases')}{"".join(phaseBlocks)}</div>
"""))

    # ---- tickets
    fi = {f["id"]: f for f in dd.frozenInterfaces}
    groupTag = lambda t: ('<span class="st pilot">PILOT</span>' if t["group"] == "pilot" else '<span class="st gate">FINAL GATE</span>' if t["group"] == "gate" else "")  # noqa: E731
    ticketRows = [(f"ticketrow-{t['id']}", {"group": td.groups[t["group"]]},
                   [f'<a class="xref" href="#ticket-{t["id"]}">{t["id"]}</a>', f'{esc(t["title"])} {groupTag(t)}', esc(t["outcome"]),
                    ", ".join(f'<a class="xref" href="#ticket-{d:02d}">{d:02d}</a>' for d in t["deps"]) or "nothing", esc(td.groups[t["group"]]), f'<code>{esc(t["file"])}</code>']) for t in tickets]
    ticketBlocks = []
    li = lambda items: "".join(f"<li>{linked(x)}</li>" for x in items)  # noqa: E731
    for t in tickets:
        frozen = [f"{i} {fi[i]['what']}" if i in fi else i for i in t["frozen"]] + [f"{i} {fi[i]['what']} - defined here, frozen once merged" for i in t["defines"]]
        deps = [f"ticket {d:02d} merged" for d in t["deps"]] or ["none"]
        ticketBlocks.append(f"""<details class="block" id="ticket-{t['id']}" data-ticket="{t['id']}"><summary>{t['id']} - {esc(t['title'])} {groupTag(t)}</summary>
<dl><dt>Work item</dt><dd>{t['workItem']} - {esc(t['outcome'])}</dd>
<dt>Source</dt><dd><code>{esc(m['repository'])}</code>, baseline <code>{m['baseline']['sha']}</code>, stream root {m['root']}</dd>
<dt>Context</dt><dd>Rules: {esc(", ".join(t['rules']) or "none")}<br>Architecture: architecture.md version {m['architectureVersion']}. {linked(t['context'])}<br>Data-map rows: {linked(t['dataRows'])}<br>Decisions: {linked(t['decisions'])}</dd>
<dt>Scope</dt><dd>Allowed files:<ul>{li(t['allowed'])}</ul>Frozen shared interfaces:<ul>{li(frozen)}</ul>Excluded work:<ul>{li(t['excluded'])}</ul></dd>
<dt>Dependencies</dt><dd><ul>{li(deps)}</ul></dd>
<dt>Acceptance</dt><dd><ul>{li(t['acceptance'])}</ul></dd>
<dt>Verification</dt><dd><ul>{li(t['verification'])}</ul></dd>
<dt>Deliverables</dt><dd>One branch and one pull request; the test output named under Verification; a list of gaps - {linked(t['gaps'])}.</dd>
<dt>Ticket writes</dt><dd>prohibited</dd>
<dt>Restrictions</dt><dd>No edits to <code>app/</code> or <code>fixtures/*/expected</code>; no merge or deploy.</dd></dl>
<p class="sub">File: <code>modernization/design/{esc(t['file'])}</code></p></details>""")
    sections.append(("tickets", "Tickets", f"""
<h2>Ticket queue ({c['tickets']})</h2>
<p class="lead">Bounded engineering work only. Ticket <a class="xref" href="#ticket-01">01</a> is the <b>pilot</b>: no dependencies, and its output can already be compared
on the recorded case <code>i01-baseline</code>. Tickets 01 and 02 can start at once and in parallel; 03 and 04 follow the pilot; 05 to 08 wait on the schema;
09 and 10 are the final gates. Decisions are not tickets. No ticket has been created in any external system.</p>
<p class="sub">Click a box to open the ticket.</p>
{ticketDiagram(tickets, td)}
{filters("tickets", [("group", "Group", [td.groups[g] for g in td.groups])])}
{tableHtml("tickets", ["Ticket", "Title", "What exists when it is done", "Starts after", "Group", "File"], ticketRows)}
<h2>Ticket text</h2>
<div id="ticketlist">{expandBar('#ticketlist')}{"".join(ticketBlocks)}</div>
"""))

    # ---- data map
    mapRows = [(f"dm-{r['copybook']}-{r['field']}-{i}", {"copybook": r["copybook"], "kind": r["kind"], "role": r["role"]},
                [f"<code>{r['copybook']}</code>", f"<code>{esc(r['field'])}</code>", esc(r["pic"]), "" if r["offset"] is None else r["offset"], "" if r["length"] is None else r["length"],
                 esc(r["kind"]), linked(r["encoding"]), f"<code>{esc(r['column'])}</code>", f"<code>{esc(r['pgType'])}</code>", esc(r["role"]), linked(r["note"])]) for i, r in enumerate(design["dataMap"])]
    encRows = [(f"enc-{e['id']}", None, [f"<b>{e['id']}</b>", esc(e["applies"]), linked(e["decode"]), linked(e["encode"])]) for e in design["encodingRules"]]
    keyRows = [(None, None, [f"<code>{esc(t['name'])}</code>", esc(t["legacyKey"]), f"<code>{esc(t['primaryKey'])}</code>", esc(t["legacyIndexes"]), esc("; ".join(t["indexes"]) or "-")]) for t in dd.tables if t["kind"] != "framework"]
    gapRows = [(f"gap-{g['id']}", {"owner": g["owner"]}, [f"<b>{g['id']}</b>", esc(g["title"]), linked(g["detail"], skip=g["id"]), esc(g["affects"]), owner(g["owner"]), linked(g["closes"])]) for g in design["gaps"]]
    sections.append(("datamap", "Data map", f"""
<h2>Data map: {c['fields']} fields of {c['copybooks']} copybooks</h2>
<p class="lead">Every field of the five record layouts and the column that holds it. Amounts keep their exact digits: a field with nine digits and two
decimals becomes <code>NUMERIC(11,2)</code>. Full text: <code>modernization/design/data-map-interest.md</code>.</p>
{filters("datamap", [("copybook", "Copybook", sorted({r["copybook"] for r in design["dataMap"]})), ("kind", "Kind", sorted({r["kind"] for r in design["dataMap"]})), ("role", "Use by the job", sorted({r["role"] for r in design["dataMap"]}))])}
{tableHtml("datamap", ["Copybook", "Field", "PIC", "Offset", "Length", "Kind", "Rule", "Column", "Type", "Use by the job", "Note"], mapRows)}
<div id="map-details">{expandBar('#map-details')}
<details class="block" id="encoding"><summary>Sign and encoding rules ({c['encodingRules']})</summary>
{tableHtml("encoding", ["Rule", "Applies to", "Reading a record", "Writing a record"], encRows)}</details>
<details class="block" id="keys"><summary>Keys and alternate indexes</summary>
{tableHtml("keys", ["Table", "Legacy key", "Primary key", "Legacy alternate index", "Index"], keyRows)}</details></div>
<h2 id="gaps">Mapping gaps that could not be closed ({c['gaps']})</h2>
{filters("gaps", [("owner", "Who can close it", sorted({g["owner"] for g in design["gaps"]}))])}
{tableHtml("gaps", ["Gap", "What is not known", "Detail", "Affects", "Who can close it", "What closes it"], gapRows)}
"""))

    # ---- decisions
    regRows = [(f"dec-{r['id']}", {"type": r["type"]}, [f"<b>{r['id']}</b>", esc(r["type"]), esc(r["question"]), esc(r["asCoded"]), f"<code>{esc(r['property'])}</code> = {esc(r['default'])}", '<span class="st open">OPEN</span>'])
               for r in design["registerDecisions"]]
    relRows = [(f"rel-{d['id']}", {"owner": d["owner"]}, [f"<b>{d['id']}</b>", esc(d["question"]), "<br>".join(f"<b>{o[0]}</b> {linked(o[2:])}" for o in d["options"]), d["proposed"], owner(d["owner"]), '<span class="st open">OPEN</span>'])
               for d in dd.releaseDecisions]
    sections.append(("decisions", "Decisions", f"""
<h2 id="register">Business decisions the design depends on ({c['registerDecisions']}, all OPEN)</h2>
<p class="lead">These are the rows of the stream's decision register (<code>modernization/rules/interest/decisions.md</code>): {c['quirks']} quirks and {c['holes']} holes.
None is answered. Each is built as the mainframe behaves today and can be changed later in one place, the switch shown.</p>
{filters("register", [("type", "Type", ["QUIRK", "HOLE"])])}
{tableHtml("register", ["Id", "Type", "Question for the business", "Built as (legacy behaviour)", "Switch and default", "Status"], regRows)}
<h2 id="release">Release and coexistence decisions ({c['releaseDecisions']}, all OPEN)</h2>
<p class="lead">Choices the design had to assume. They are kept in <code>modernization/design/decisions-release.md</code>, not in the ticket queue.</p>
{filters("release", [("owner", "Owner", sorted({d["owner"] for d in dd.releaseDecisions}))])}
{tableHtml("release", ["Id", "Question", "Options", "Design assumes", "Owner", "Status"], relRows)}
"""))

    nav = "".join(f'<button type="button" role="tab" id="tabbtn-{key}" aria-controls="tab-{key}" aria-selected="{"true" if i == 0 else "false"}" tabindex="{0 if i == 0 else -1}">{esc(title)}</button>'
                  for i, (key, title, _) in enumerate(sections))
    body = "".join(f'<section role="tabpanel" id="tab-{key}" aria-labelledby="tabbtn-{key}"{"" if i == 0 else " hidden"}>{content}</section>' for i, (key, _, content) in enumerate(sections))
    dataJson = json.dumps(design, separators=(",", ":")).replace("</", "<\\/")
    return f"""<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Strategy - moving the interest stream (INTCALC)</title><style>{css}</style></head><body>
<header><h1>Strategy - moving the interest stream (INTCALC)</h1>
<p class="meta">Repository <code>{esc(m['repository'])}</code> - stream root <code>{esc(m['root'])}</code> - target: {esc(m['targetStack'])}</p>
<p class="meta">Baseline of the estate: commit <code id="sha">{esc(m['baseline']['sha'])}</code> (head of <code>{esc(m['baseline']['branch'])}</code>).
Design based on <code id="base-sha">{esc(m['designBase']['sha'])}</code> (head of <code>{esc(m['designBase']['branch'])}</code>). architecture.md version <code id="arch-version">{esc(m['architectureVersion'])}</code>.</p>
<p class="notice"><b>Proposal, not approved.</b> Written by an agent from the code, the stream manifest and the rulebook. Every item is marked {proposed()} with the role that has to confirm it. No Java has been written and no ticket has been created anywhere.</p>
</header>
<nav role="tablist" aria-label="Sections">{nav}</nav>
<main>{body}</main>
<footer>Self-contained page: no external script, style, font or image. Data behind the page: <code>modernization/design/design.json</code> (embedded), built by <code>{esc(m['generatedBy'])}</code>.</footer>
<script id="data" type="application/json">{dataJson}</script>
<script>{js}</script></body></html>
"""
