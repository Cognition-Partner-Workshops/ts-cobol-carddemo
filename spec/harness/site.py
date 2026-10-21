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
    inv_rows = [[esc(p.get("name")), esc(p.get("type")), esc(p.get("group")), esc(", ".join(p.get("jcl", []))),
                 esc(", ".join(p.get("copybooks", []))), esc(", ".join(p.get("datasets", []))),
                 esc(", ".join(p.get("external_calls", []))),
                 "<span class='badge amber'>unreferenced</span>" if p.get("unreferenced") else esc(p.get("wave", ""))]
                for p in progs]
    edge_rows = [[esc(e.get("from")), esc(e.get("to")), esc(e.get("via"))] for e in waves.get("edges", [])]
    wave_html = "".join(
        f"<div class='wave'><b>{esc(w.get('name'))}</b><ul>" + "".join(f"<li>{esc(x)}</li>" for x in w.get("members", [])) + "</ul></div>"
        for w in waves.get("waves", [])) or "<p class='empty'>No wave plan yet.</p>"
    rule_rows_html = []
    for p in spec:
        for r in p["rules"]:
            st = status_of(r)
            rule_rows_html.append([esc(p["program"]), esc(r.get("rule")), f"<code>{esc(r.get('cobol citation', ''))}</code>",
                                   f"<code>{esc(r.get('acceptance test', ''))}</code>", f"<span class='badge {st}'>{esc(st)}</span>"])
    body = (f"<div class='tiles'>{tiles}</div><br>"
            f"<section><h2>Inventory</h2>{table(['Program', 'Type', 'Group', 'JCL', 'Copybooks', 'Datasets', 'External calls', 'Wave'], inv_rows)}</section>"
            f"<section><h2>Dependencies</h2>{table(['From', 'To', 'Through'], edge_rows)}</section>"
            f"<section><h2>Wave plan</h2><div class='waves'>{wave_html}</div></section>"
            f"<section><h2>Specification</h2>{table(['Program', 'Rule', 'COBOL citation', 'Acceptance test', 'Status'], rule_rows_html)}</section>")
    html_text = page("CardDemo assessment", f"{label} · specification in spec/specification", body)
    md = [f"# CardDemo assessment\n\n{label}\n\n## Measures\n"]
    md.append(md_table(["Measure", "Value"], [[k.replace("_", " "), v] for k, v in m.items()]))
    md.append("\n## Inventory\n\n" + md_table(["Program", "Type", "Group", "JCL", "Copybooks", "Datasets", "External calls", "Wave"],
              [[p.get("name"), p.get("type"), p.get("group"), ", ".join(p.get("jcl", [])), ", ".join(p.get("copybooks", [])),
                ", ".join(p.get("datasets", [])), ", ".join(p.get("external_calls", [])),
                "unreferenced" if p.get("unreferenced") else p.get("wave", "")] for p in progs]))
    md.append("\n## Dependencies\n\n" + md_table(["From", "To", "Through"], [[e.get("from"), e.get("to"), e.get("via")] for e in waves.get("edges", [])]))
    md.append("\n## Wave plan\n\n" + ("".join(f"- {w.get('name')}: {', '.join(w.get('members', []))}\n" for w in waves.get("waves", [])) or "Nothing here yet.\n"))
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
