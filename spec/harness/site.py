"""Static pages: the assessment page and the comparison report, each with a Markdown twin."""
import html
import json
import re
from pathlib import Path

from . import SPEC_DIR

CSS = """
body{font-family:system-ui,-apple-system,Segoe UI,sans-serif;margin:0;color:#1d2433;background:#f6f7f9}
header{background:#14213d;color:#fff;padding:20px 32px}header h1{margin:0;font-size:22px}
header p{margin:4px 0 0;opacity:.8}main{padding:24px 32px;max-width:1200px}
section{background:#fff;border:1px solid #dde1e7;border-radius:8px;padding:16px 20px;margin-bottom:18px}
h2{font-size:17px;margin:0 0 12px}table{border-collapse:collapse;width:100%;font-size:13px}
th,td{border-bottom:1px solid #eceff3;padding:6px 8px;text-align:left;vertical-align:top}
th{background:#f1f3f6}.tiles{display:flex;gap:12px;flex-wrap:wrap}
.tile{flex:1;min-width:150px;background:#fff;border:1px solid #dde1e7;border-radius:8px;padding:12px 16px}
.tile b{display:block;font-size:28px}.green{color:#13795b}.amber{color:#b26a00}.red{color:#b42318}
.badge{display:inline-block;padding:1px 8px;border-radius:10px;font-size:12px;background:#eceff3}
.badge.green{background:#d9f2e6}.badge.amber{background:#fdecc8}.badge.red{background:#fde2e1}
.empty{color:#6b7280;font-style:italic}nav a{color:#fff;margin-right:16px}code{font-size:12px}
.waves{display:flex;gap:12px}.wave{flex:1;border:1px solid #dde1e7;border-radius:6px;padding:8px}
"""

ROW_RE = re.compile(r"^\|(.+)\|\s*$")


def rule_rows(md_text):
    """Rows of the rule table: the table whose header starts with 'Rule'."""
    rows, header, in_table = [], None, False
    for line in md_text.splitlines():
        m = ROW_RE.match(line.strip())
        if not m:
            in_table = False
            continue
        cells = [c.strip() for c in m.group(1).split("|")]
        if header is None or not in_table:
            if cells and cells[0].lower() == "rule":
                header, in_table = [c.lower() for c in cells], True
            continue
        if set("".join(cells)) <= set("-: "):
            continue
        rows.append(dict(zip(header, cells)))
    return rows


def load_specification():
    programs = []
    for path in sorted((SPEC_DIR / "specification").glob("*.md")):
        if path.name.startswith("_") or path.name.lower() == "readme.md":
            continue
        rows = rule_rows(path.read_text())
        programs.append({"program": path.stem, "file": f"spec/specification/{path.name}", "rules": rows})
    return programs


def load_json(path, default):
    path = Path(path)
    return json.loads(path.read_text()) if path.exists() else default


def status_of(row):
    s = (row.get("status") or "").lower()
    return "green" if "green" in s else "amber" if "amber" in s else "red" if "red" in s else "unknown"


def measures(inventory, spec):
    rules = [r for p in spec for r in p["rules"]]
    return {
        "programs_inventoried": len(inventory.get("programs", [])),
        "rules_specified": len(rules),
        "rules_proven": sum(1 for r in rules if status_of(r) == "green"),
        "rules_amber": sum(1 for r in rules if status_of(r) == "amber"),
    }


def esc(v):
    return html.escape(str(v if v is not None else ""))


def page(title, subtitle, body):
    nav = '<nav><a href="index.html">Assessment</a><a href="comparison.html">Comparison report</a></nav>'
    return (f"<!doctype html><html><head><meta charset='utf-8'><title>{esc(title)}</title><style>{CSS}</style></head>"
            f"<body><header>{nav}<h1>{esc(title)}</h1><p>{esc(subtitle)}</p></header><main>{body}</main></body></html>")


def table(headers, rows):
    if not rows:
        return "<p class='empty'>Nothing here yet.</p>"
    head = "".join(f"<th>{esc(h)}</th>" for h in headers)
    body = "".join("<tr>" + "".join(f"<td>{c}</td>" for c in r) + "</tr>" for r in rows)
    return f"<table><thead><tr>{head}</tr></thead><tbody>{body}</tbody></table>"


def md_table(headers, rows):
    if not rows:
        return "Nothing here yet.\n"
    out = ["| " + " | ".join(headers) + " |", "|" + "---|" * len(headers)]
    out += ["| " + " | ".join(str(c).replace("|", "/") for c in r) + " |" for r in rows]
    return "\n".join(out) + "\n"


def flat(items):
    """Datasets and external calls may be strings or objects; the page shows one label per item."""
    out = []
    for x in items or []:
        if isinstance(x, dict):
            x = x.get("dsn") or x.get("queue") or x.get("cics_file") or x.get("ims_db") or x.get("file") or x.get("target") or x.get("dd") or ""
        if x:
            out.append(str(x))
    return ", ".join(out)


def complexity_cells(p):
    c = p.get("complexity") or {}
    if c.get("score") is None:
        return ["", "", "", "", "n/a"]
    return [c.get("paragraphs"), c.get("file_io_verbs"), c.get("external_calls"), c["score"], c.get("band", "")]


BAND_CLS = {"low": "green", "medium": "amber", "high": "red"}


def wave_svg(waves):
    """Left-to-right job graph of the wave plan, arrows labelled with the datasets that order the jobs."""
    if not waves:
        return ""
    w, h, gap = 230, 120, 120
    pos = {wv["job"]: i * (w + gap) + 10 for i, wv in enumerate(waves)}
    parts = []
    for i, wv in enumerate(waves):
        x = pos[wv["job"]]
        deferred = "deferred" in wv.get("status", "")
        parts.append(f"<rect x='{x}' y='40' width='{w}' height='{h}' rx='8' fill='{'#f6f7f9' if deferred else '#e8f0fe'}' "
                     f"stroke='#14213d' stroke-dasharray='{'6 4' if deferred else '0'}'/>")
        parts.append(f"<text x='{x + 10}' y='62' font-weight='bold' font-size='13'>{esc(wv['name'])}</text>")
        for j, m in enumerate(wv.get("members", [])):
            parts.append(f"<text x='{x + 14}' y='{86 + j * 18}' font-size='12'>{esc(m)}</text>")
        if deferred:
            parts.append(f"<text x='{x + 10}' y='{40 + h - 10}' font-size='11' fill='#b26a00'>deferred: inventoried only</text>")
    for k, wv in enumerate(waves):
        for a in wv.get("after", []):
            if a["job"] not in pos:
                continue
            x1, x2 = pos[a["job"]] + w, pos[wv["job"]]
            y = 100 if x2 - x1 == gap else 40 + h + 12 + 14 * k
            if x2 - x1 == gap:
                parts.append(f"<line x1='{x1}' y1='{y}' x2='{x2 - 4}' y2='{y}' stroke='#14213d' marker-end='url(#arr)'/>")
                for j, d in enumerate(a["datasets"] or ["job order"]):
                    parts.append(f"<text x='{x1 + 4}' y='{y - 8 - j * 12}' font-size='9'>{esc(d.replace('AWS.M2.CARDDEMO.', ''))}</text>")
            else:
                mid1, mid2 = pos[a["job"]] + w // 2, x2 + w // 2
                parts.append(f"<path d='M{mid1},{40 + h} V{y} H{mid2} V{40 + h + 4}' fill='none' stroke='#6b7280' marker-end='url(#arr)'/>")
                label = ", ".join(d.replace("AWS.M2.CARDDEMO.", "") for d in a["datasets"]) or "job order"
                parts.append(f"<text x='{mid1 + 6}' y='{y - 3}' font-size='9' fill='#6b7280'>{esc(a['job'])} to {esc(wv['job'])}: {esc(label)}</text>")
    width = len(waves) * (w + gap)
    return (f"<svg width='{width}' height='{40 + h + 20 + 14 * len(waves)}' xmlns='http://www.w3.org/2000/svg' font-family='system-ui'>"
            "<defs><marker id='arr' markerWidth='8' markerHeight='8' refX='7' refY='4' orient='auto'><path d='M0,0 L8,4 L0,8 z' fill='#14213d'/></marker></defs>"
            + "".join(parts) + "</svg>")


def assessment(run_dir, label):
    inv = load_json(SPEC_DIR / "assessment" / "inventory.json", {"programs": []})
    waves = load_json(SPEC_DIR / "assessment" / "waves.json", {"waves": [], "edges": []})
    spec = load_specification()
    report = load_json(Path(run_dir) / "report" / "comparison.json", None)
    m = measures(inv, spec)
    tiles = "".join(f"<div class='tile'><b>{v}</b>{esc(k.replace('_', ' '))}</div>" for k, v in m.items())
    if report:
        tiles += f"<div class='tile'><b class='{'green' if report['verdict'] == 'pass' else 'red'}'>{esc(report['verdict'])}</b>COBOL and Java comparison</div>"
    progs = inv.get("programs", [])
    inv_heads = ["Program", "Type", "Group", "JCL", "Copybooks", "Datasets", "External calls", "Complexity", "Wave"]

    def inv_cells(p):
        c = complexity_cells(p)
        return [p.get("name"), p.get("type"), p.get("group"), ", ".join(p.get("jcl", [])), ", ".join(p.get("copybooks", [])),
                flat(p.get("datasets")), flat(p.get("external_calls")), f"{c[3]} {c[4]}".strip(),
                "unreferenced" if p.get("unreferenced") else p.get("wave", "")]

    inv_rows = []
    for p in progs:
        cells = [esc(c) for c in inv_cells(p)]
        band = (p.get("complexity") or {}).get("band")
        if band:
            cells[7] = f"{esc((p.get('complexity') or {}).get('score'))} <span class='badge {BAND_CLS.get(band, '')}'>{esc(band)}</span>"
        if p.get("unreferenced"):
            cells[8] = "<span class='badge amber'>unreferenced</span>"
        inv_rows.append(cells)
    cx_heads = ["Program", "Group", "Paragraphs", "I/O verbs", "External calls", "Score", "Band"]
    cx_progs = sorted(progs, key=lambda p: -((p.get("complexity") or {}).get("score") or -1))
    cx_md = [[p.get("name"), p.get("group"), *complexity_cells(p)] for p in cx_progs]
    cx_rows = [[esc(c) for c in r[:-1]] + [f"<span class='badge {BAND_CLS.get(r[-1], '')}'>{esc(r[-1])}</span>"] for r in cx_md]
    edges = waves.get("edges", [])
    edge_rows = [[esc(e.get("from")), esc(e.get("to")), esc(e.get("via"))] for e in edges]
    unref = [mb for mb in inv.get("members", []) if mb.get("unreferenced")]
    if not inv.get("members"):
        unref = [{"member": p.get("source") or p.get("name"), "kind": p.get("type"), "covered_by": p.get("evidence", "")}
                 for p in progs if p.get("unreferenced")]
    unref_md = [[u["member"], u.get("kind", ""), u.get("covered_by", ""), u.get("source", "")] for u in unref]
    issues = inv.get("issues", [])
    issue_md = [[i.get("group"), i.get("kind"), i.get("text")] for i in issues]
    cov = inv.get("coverage")
    cov_md = ([["members under app/", cov["members"]], ["not in any group file (covered by the merge cross-check)", len(cov["not_in_group_files"])],
               ["unreferenced", len(cov["unreferenced"])], ["neither inventoried nor unreferenced", len(cov["neither"])]] if cov else [])
    counts_md = [[g, n] for g, n in (inv.get("counts") or {}).items()]
    wave_html = "".join(
        f"<div class='wave'><b>{esc(w.get('name'))}</b>"
        + (" <span class='badge amber'>deferred</span>" if "deferred" in w.get("status", "") else " <span class='badge green'>in scope</span>")
        + f"<p>{esc(w.get('status', ''))}</p><ul>" + "".join(f"<li>{esc(x)}</li>" for x in w.get("members", [])) + "</ul>"
        + "".join(f"<p>after {esc(a['job'])}: reads {esc(', '.join(a['datasets']) or 'job order')}</p>" for a in w.get("after", []))
        + "</div>"
        for w in waves.get("waves", [])) or "<p class='empty'>No wave plan yet.</p>"
    rule_rows_html = []
    for p in spec:
        for r in p["rules"]:
            st = status_of(r)
            rule_rows_html.append([esc(p["program"]), esc(r.get("rule")), f"<code>{esc(r.get('cobol citation', ''))}</code>",
                                   f"<code>{esc(r.get('acceptance test', ''))}</code>", f"<span class='badge {st}'>{esc(st)}</span>"])
    body = (f"<div class='tiles'>{tiles}</div><br>"
            f"<section><h2>Programs by group</h2>{table(['Group', 'Programs'], [[esc(c) for c in r] for r in counts_md])}</section>"
            f"<section><h2>Inventory</h2>{table(inv_heads, inv_rows)}</section>"
            f"<section><h2>Complexity per program</h2><p>{esc(inv.get('complexity', ''))}</p>{table(cx_heads, cx_rows)}</section>"
            f"<section><h2>Dependency graph</h2>{wave_svg(waves.get('waves', []))}{table(['From', 'To', 'Through'], edge_rows)}</section>"
            f"<section><h2>Unreferenced members</h2>{table(['Member', 'Kind', 'Evidence', 'Found by'], [[esc(c) for c in r] for r in unref_md])}</section>"
            f"<section><h2>Wave plan</h2><div class='waves'>{wave_html}</div></section>"
            f"<section><h2>Open issues</h2>{table(['Group', 'Kind', 'Issue'], [[esc(c) for c in r] for r in issue_md])}</section>"
            f"<section><h2>Coverage of app/</h2>{table(['Check', 'Members'], [[esc(c) for c in r] for r in cov_md])}</section>"
            f"<section><h2>Specification</h2>{table(['Program', 'Rule', 'COBOL citation', 'Acceptance test', 'Status'], rule_rows_html)}</section>")
    html_text = page("CardDemo assessment", f"{label} · specification in spec/specification", body)
    md = [f"# CardDemo assessment\n\n{label}\n\n## Measures\n"]
    md.append(md_table(["Measure", "Value"], [[k.replace("_", " "), v] for k, v in m.items()]))
    md.append("\n## Programs by group\n\n" + md_table(["Group", "Programs"], counts_md))
    md.append("\n## Inventory\n\n" + md_table(inv_heads, [inv_cells(p) for p in progs]))
    md.append("\n## Complexity per program\n\n" + md_table(cx_heads, cx_md))
    md.append("\n## Dependency graph\n\n" + md_table(["From", "To", "Through"], [[e.get("from"), e.get("to"), e.get("via")] for e in edges]))
    md.append("\n## Unreferenced members\n\n" + md_table(["Member", "Kind", "Evidence", "Found by"], unref_md))
    md.append("\n## Wave plan\n\n" + ("".join(
        f"- {w.get('name')}: {', '.join(w.get('members', []))} ({w.get('status', '')})"
        + "".join(f"; after {a['job']}: reads {', '.join(a['datasets']) or 'job order'}" for a in w.get("after", [])) + "\n"
        for w in waves.get("waves", [])) or "Nothing here yet.\n"))
    md.append("\n## Open issues\n\n" + md_table(["Group", "Kind", "Issue"], issue_md))
    md.append("\n## Coverage of app/\n\n" + md_table(["Check", "Members"], cov_md))
    md.append("\n## Specification\n\n" + md_table(["Program", "Rule", "COBOL citation", "Acceptance test", "Status"],
              [[p["program"], r.get("rule"), r.get("cobol citation", ""), r.get("acceptance test", ""), status_of(r)] for p in spec for r in p["rules"]]))
    return html_text, "".join(md), m


def comparison(run_dir, label):
    report = load_json(Path(run_dir) / "report" / "comparison.json", None)
    if not report:
        body = "<section><p class='empty'>No comparison has run yet.</p></section>"
        return page("COBOL and Java comparison", label, body), f"# COBOL and Java comparison\n\n{label}\n\nNo comparison has run yet.\n"
    rows, md_rows, samples = [], [], []
    for d in report["datasets"]:
        rc, h = d["record_count"], d["keyed_record_hash"]
        verdict_cls = "green" if d["verdict"] == "match" else "amber" if d["unexplained"] == 0 else "red"
        rows.append([esc(d["dataset"]), f"{rc['left']} / {rc['right']}", esc(f"{d['field_profile']['fields'] - len(d['field_profile']['differing'])} of {d['field_profile']['fields']}"),
                     f"{h['matched']} matched, {h['differing']} differ", str(d["expected"]), str(d["unexplained"]),
                     f"<span class='badge {verdict_cls}'>{esc(d['verdict'])}</span>"])
        md_rows.append([d["dataset"], f"{rc['left']} / {rc['right']}", f"{d['field_profile']['fields'] - len(d['field_profile']['differing'])} of {d['field_profile']['fields']}",
                        f"{h['matched']} matched, {h['differing']} differ", d["expected"], d["unexplained"], d["verdict"]])
        for s in d["sampled_value_differences"]:
            samples.append([d["dataset"], s["key"], s["field"], s["left"], s["right"], s["expected"] or "unexplained", s["change_request"] or ""])
    heads = ["Dataset", f"Records ({report['left']} / {report['right']})", "Field profile equal", "Keyed record hash", "Expected", "Unexplained", "Verdict"]
    sample_heads = ["Dataset", "Key", "Field", report["left"], report["right"], "Explained by", "Change request"]
    verdict = report["verdict"]
    body = (f"<div class='tiles'><div class='tile'><b class='{'green' if verdict == 'pass' else 'red'}'>{esc(verdict)}</b>overall</div>"
            f"<div class='tile'><b>{report['expected']}</b>expected differences</div><div class='tile'><b>{report['unexplained']}</b>unexplained differences</div></div><br>"
            f"<section><h2>Per dataset</h2>{table(heads, rows)}</section>"
            f"<section><h2>Sampled value differences</h2>{table(sample_heads, [[esc(c) for c in r] for r in samples])}</section>")
    md = (f"# COBOL and Java comparison\n\n{label}\n\nVerdict: **{verdict}**, {report['expected']} expected and {report['unexplained']} unexplained differences.\n\n"
          f"## Per dataset\n\n{md_table(heads, md_rows)}\n## Sampled value differences\n\n{md_table(sample_heads, samples)}")
    return page("COBOL and Java comparison", label, body), md


def build(run_dir, label="CardDemo batch path"):
    run_dir = Path(run_dir)
    site = run_dir / "site"
    site.mkdir(parents=True, exist_ok=True)
    a_html, a_md, m = assessment(run_dir, label)
    c_html, c_md = comparison(run_dir, label)
    (site / "index.html").write_text(a_html)
    (site / "index.md").write_text(a_md)
    (site / "comparison.html").write_text(c_html)
    (site / "comparison.md").write_text(c_md)
    (site / "measures.json").write_text(json.dumps(m, indent=2))
    return m
