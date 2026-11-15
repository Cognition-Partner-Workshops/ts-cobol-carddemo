#!/usr/bin/env python3
"""Render rules-INTCALC.html (one self-contained review board) from rules.yaml.

  python3 modernization/rules/interest/tools/build_board.py           write the board
  python3 modernization/rules/interest/tools/build_board.py --check   fail when it is out of date
"""
import hashlib
import html
import json
import re
import sys
from pathlib import Path

import yaml

rulesDir = Path(__file__).resolve().parents[1]
repoRoot = rulesDir.parents[2]
boardPath = rulesDir / "rules-INTCALC.html"
rulesText = (rulesDir / "rules.yaml").read_text()
rulebook = yaml.safe_load(rulesText)
typeOrder = ["RULE", "QUIRK", "HOLE"]
notice = ("Not SME sign-off. These rules were read out of the code by an agent. Nothing on this board has been "
          "confirmed by a business owner; decisions recorded here stay in this browser until exported.")


def esc(value):
    return html.escape("" if value is None else str(value))


def excerpt(cite):
    path, _, span = cite.rpartition(":")
    first, _, last = span.partition("-")
    first, last = int(first), int(last or first)
    lines = (repoRoot / path).read_text(errors="replace").splitlines()[first - 1:last]
    return {"cite": cite, "path": path, "first": first, "lines": [line.rstrip() for line in lines]}


def card(rule):
    excerpts = [excerpt(c) for c in rule["source"]]
    blocks = []
    for item in excerpts:
        rows = "".join(f'<span class="ln">{item["first"] + i}</span><span class="src">{esc(line)}</span>\n'
                       for i, line in enumerate(item["lines"]))
        blocks.append(f'<div class="cite">{esc(item["cite"])}</div><pre data-cite="{esc(item["cite"])}">{rows}</pre>')
    fixtures = ", ".join(rule["fixtures"]) or f'none - {rule.get("fixtureNote", "")}'
    extra = ""
    if rule["type"] != "RULE":
        options = "".join(f'<li><b>{esc(o["id"])}</b> {esc(o["label"])}</li>' for o in rule["options"])
        extra = (f'<p class="why"><b>Why it is open:</b> {esc(rule["why"])}</p>'
                 f'<p class="question"><b>Question for the business:</b> {esc(rule["question"])}</p>'
                 f'<ul class="options">{options}</ul>')
    ruleId = rule["id"]
    return f'''<article class="card {rule["type"].lower()}" id="{ruleId}" data-id="{ruleId}" data-type="{rule["type"]}">
<header><span class="type">{rule["type"]}</span><span class="rid">{ruleId}</span><h3>{esc(rule["title"])}</h3>
<span class="decision" data-role="decision">UNDECIDED</span></header>
<p class="statement">{esc(rule["statement"])}</p>
{extra}
<dl><dt>Call path</dt><dd>{esc(rule["callPath"])}</dd><dt>Fixture cases</dt><dd>{esc(fixtures)}</dd>
<dt>Source</dt><dd>{esc(", ".join(rule["source"]))}</dd></dl>
<details class="excerpt"><summary>Source excerpt ({len(excerpts)} citation{"s" if len(excerpts) != 1 else ""})</summary>
{"".join(blocks)}</details>
<div class="controls">
<button type="button" class="approve" data-role="approve">Approve</button>
<button type="button" class="reject" data-role="reject">Reject</button>
<label class="commentLabel">Comment <textarea data-role="comment" rows="2" aria-label="Comment on {ruleId}"></textarea></label>
<button type="button" data-role="saveComment">Save comment</button>
<span class="meta" data-role="meta"></span>
</div>
<p class="cardError" data-role="error" role="alert" hidden></p>
</article>'''


def tableOf(headers, rows):
    head = "".join(f"<th>{esc(h)}</th>" for h in headers)
    body = "".join("<tr>" + "".join(f"<td>{esc(c)}</td>" for c in row) + "</tr>" for row in rows)
    return f"<table><thead><tr>{head}</tr></thead><tbody>{body}</tbody></table>"


def ruleList(value):
    return "none" if value in ("none", [], None) else ", ".join(value)


def build():
    rules = rulebook["rules"]
    counts = {t: sum(1 for r in rules if r["type"] == t) for t in typeOrder}
    sections = []
    for ruleType in typeOrder:
        cards = "\n".join(card(r) for r in rules if r["type"] == ruleType)
        sections.append(f'<section class="group" id="group-{ruleType}"><h2>{ruleType} '
                        f'<span class="count" id="count-{ruleType}">{counts[ruleType]}</span> '
                        f'<small>{esc(rulebook["types"][ruleType])}</small></h2>\n{cards}</section>')
    summary = "".join(f"<p>{esc(p)}</p>" for p in re.split(r"\n\s*\n", rulebook["summary"].strip()))
    coverage = rulebook["coverage"]
    paragraphRows = [(p["name"], ruleList(p["rules"]), p.get("reason", "")) for p in coverage["paragraphs"]]
    edgeRows = [(e["id"], e["edge"], ruleList(e["rules"]), e.get("reason", "")) for e in coverage["edges"]]
    pathRows = [(p["id"], p["path"], p["decides"], ruleList(p["rules"]) if p["rules"] else "covered: " + p["covered"])
                for p in rulebook["paths"]]
    noteRows = [(n["id"], n["note"], ", ".join(n["source"])) for n in rulebook["technicalNotes"]]
    boardData = {"rules": [{"id": r["id"], "type": r["type"], "title": r["title"]} for r in rules],
                 "rulesSha256": hashlib.sha256(rulesText.encode()).hexdigest()}
    page = PAGE.replace("@@NOTICE@@", esc(notice)).replace("@@SUMMARY@@", summary)
    page = page.replace("@@SECTIONS@@", "\n".join(sections)).replace("@@TOTAL@@", str(len(rules)))
    page = page.replace("@@SHA@@", boardData["rulesSha256"][:12])
    page = page.replace("@@PATHS@@", tableOf(["Path", "Walk", "Decides or computes", "Rules"], pathRows))
    page = page.replace("@@PARAGRAPHS@@", tableOf(["Paragraph", "Rules", "Reason when none"], paragraphRows))
    page = page.replace("@@EDGES@@", tableOf(["Edge", "What", "Rules", "Reason when none"], edgeRows))
    page = page.replace("@@NOTES@@", tableOf(["Note", "Detail that carries no policy", "Source"], noteRows))
    return page.replace("@@DATA@@", json.dumps(boardData).replace("</", "<\\/"))


PAGE = r'''<!DOCTYPE html>
<html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Rules review board - INTCALC (interest stream)</title>
<style>
:root{--ink:#1c2430;--muted:#5b6675;--line:#d5dbe3;--bg:#f5f7fa;--rule:#1f6f4a;--quirk:#9a5b00;--hole:#a3262e;--accent:#1d4ed8}
*{box-sizing:border-box}body{margin:0;font:15px/1.5 system-ui,-apple-system,"Segoe UI",sans-serif;color:var(--ink);background:var(--bg)}
#top{position:sticky;top:0;z-index:5;background:#fff;border-bottom:1px solid var(--line);padding:10px 24px}
#top h1{font-size:19px;margin:0 0 6px}
#notice{margin:0 0 8px;padding:8px 12px;border:2px solid var(--hole);border-radius:6px;background:#fff4f4;color:#7a141b;font-weight:600}
#bar{display:flex;flex-wrap:wrap;gap:10px 18px;align-items:center}
#bar label{font-weight:600}#reviewer{font:inherit;padding:5px 8px;border:1px solid #8b96a5;border-radius:4px;width:220px}
#reviewer.missing{border-color:var(--hole);outline:2px solid #f3b6ba}
#totals{display:flex;gap:6px;flex-wrap:wrap}#totals span{background:var(--bg);border:1px solid var(--line);border-radius:12px;padding:2px 10px}
#totals b{font-variant-numeric:tabular-nums}
button{font:inherit;padding:5px 12px;border:1px solid #8b96a5;border-radius:4px;background:#fff;cursor:pointer}
button:hover{background:#eef2f7}button:focus-visible,summary:focus-visible,textarea:focus-visible,input:focus-visible{outline:2px solid var(--accent);outline-offset:1px}
#barError{color:var(--hole);font-weight:600}
main{max-width:1080px;margin:0 auto;padding:18px 24px 60px}
#summary{background:#fff;border:1px solid var(--line);border-radius:8px;padding:6px 20px;margin-bottom:22px}
#summary h2{margin:12px 0 4px;font-size:17px}
.group h2{font-size:17px;margin:26px 0 10px}.group h2 small{font-weight:400;color:var(--muted);margin-left:8px}
.count{display:inline-block;min-width:26px;text-align:center;border-radius:12px;background:var(--ink);color:#fff;padding:0 8px;font-size:14px}
.card{background:#fff;border:1px solid var(--line);border-left:6px solid var(--muted);border-radius:8px;padding:12px 18px;margin:0 0 14px}
.card.rule{border-left-color:var(--rule)}.card.quirk{border-left-color:var(--quirk)}.card.hole{border-left-color:var(--hole)}
.card header{display:flex;gap:10px;align-items:center;flex-wrap:wrap}.card h3{margin:0;font-size:16px;flex:1 1 300px}
.type{font-size:12px;font-weight:700;letter-spacing:.04em;color:#fff;border-radius:4px;padding:1px 8px;background:var(--muted)}
.rule .type{background:var(--rule)}.quirk .type{background:var(--quirk)}.hole .type{background:var(--hole)}
.rid{font-family:ui-monospace,Menlo,Consolas,monospace;font-size:13px;color:var(--muted)}
.decision{font-size:12px;font-weight:700;border-radius:4px;padding:2px 9px;border:1px solid var(--line);color:var(--muted)}
.decision.approved{background:#e3f4ea;border-color:var(--rule);color:var(--rule)}
.decision.rejected{background:#fde8e9;border-color:var(--hole);color:var(--hole)}
.statement{font-size:16px;margin:8px 0}.why,.question{margin:6px 0}.options{margin:4px 0 8px;padding-left:22px}
dl{display:grid;grid-template-columns:110px 1fr;gap:2px 12px;margin:8px 0;font-size:14px}dt{color:var(--muted)}dd{margin:0;overflow-wrap:anywhere}
details{margin:8px 0}summary{cursor:pointer;color:var(--accent);font-weight:600}
.cite{font-family:ui-monospace,Menlo,Consolas,monospace;font-size:12px;color:var(--muted);margin-top:8px}
pre{margin:2px 0 0;padding:8px;background:#0f1722;color:#e6edf5;border-radius:6px;overflow-x:auto;font:12.5px/1.45 ui-monospace,Menlo,Consolas,monospace}
.ln{display:inline-block;width:42px;color:#8391a5;user-select:none}
.controls{display:flex;flex-wrap:wrap;gap:8px;align-items:flex-end;margin-top:8px}
.approve.on{background:var(--rule);color:#fff;border-color:var(--rule)}.reject.on{background:var(--hole);color:#fff;border-color:var(--hole)}
.commentLabel{display:flex;flex-direction:column;font-size:13px;color:var(--muted);flex:1 1 280px}
textarea{font:inherit;padding:5px 8px;border:1px solid #8b96a5;border-radius:4px;resize:vertical;color:var(--ink)}
.meta{font-size:13px;color:var(--muted);flex-basis:100%}
.cardError{color:var(--hole);font-weight:600;margin:6px 0 0}
table{border-collapse:collapse;width:100%;font-size:13.5px;background:#fff}th,td{border:1px solid var(--line);padding:4px 8px;text-align:left;vertical-align:top}th{background:#eef2f7}
.appendix details{background:#fff;border:1px solid var(--line);border-radius:8px;padding:8px 14px}
footer{color:var(--muted);font-size:13px;margin-top:24px}
</style></head><body>
<div id="top">
<h1>Rules review board - INTCALC, the monthly interest run</h1>
<p id="notice" role="note">@@NOTICE@@</p>
<div id="bar">
<label for="reviewer">Reviewer name</label><input id="reviewer" type="text" autocomplete="off" placeholder="your name">
<div id="totals" aria-live="polite">
<span>Approved <b id="total-approved">0</b></span><span>Rejected <b id="total-rejected">0</b></span>
<span>Commented <b id="total-commented">0</b></span><span>Undecided <b id="total-undecided">@@TOTAL@@</b></span>
<span>Cards <b id="total-cards">@@TOTAL@@</b></span></div>
<button type="button" id="export">Export CSV</button>
<button type="button" id="clear">Clear decisions</button>
<button type="button" id="clearConfirm" hidden>Yes, clear all</button>
<button type="button" id="clearCancel" hidden>Cancel</button>
<span id="barError" role="alert"></span>
</div></div>
<main>
<section id="summary"><h2>What the monthly interest run does</h2>@@SUMMARY@@</section>
@@SECTIONS@@
<section class="appendix"><h2>Coverage and technical notes</h2>
<details id="paths"><summary>Paths walked from the job step</summary>@@PATHS@@</details>
<details id="coverage"><summary>Coverage: every paragraph and data edge</summary>@@PARAGRAPHS@@<br>@@EDGES@@</details>
<details id="notes"><summary>Technical notes (no policy)</summary>@@NOTES@@</details>
</section>
<footer>Generated from modernization/rules/interest/rules.yaml (sha256 @@SHA@@) by tools/build_board.py. Decisions are kept in this browser only (localStorage) and leave it only through Export CSV.</footer>
</main>
<script id="boardData" type="application/json">@@DATA@@</script>
<script>
(function () {
  "use strict";
  var boardData = JSON.parse(document.getElementById("boardData").textContent);
  var storeKey = "rules-INTCALC-decisions-v1";
  var reviewerKey = "rules-INTCALC-reviewer-v1";
  var reviewerInput = document.getElementById("reviewer");
  var barError = document.getElementById("barError");
  var decisions = {};
  try { decisions = JSON.parse(localStorage.getItem(storeKey) || "{}"); } catch (e) { decisions = {}; }
  try { reviewerInput.value = localStorage.getItem(reviewerKey) || ""; } catch (e) { /* storage unavailable */ }

  function save() {
    try { localStorage.setItem(storeKey, JSON.stringify(decisions)); barError.textContent = ""; }
    catch (e) { barError.textContent = "This browser refused to store the decisions; export before closing."; }
  }
  function part(cardEl, role) { return cardEl.querySelector('[data-role="' + role + '"]'); }
  function entryOf(ruleId) { return decisions[ruleId] || { decision: "", comment: "", reviewer: "", at: "" }; }

  function render() {
    var totals = { approved: 0, rejected: 0, commented: 0, undecided: 0 };
    document.querySelectorAll(".card").forEach(function (cardEl) {
      var entry = entryOf(cardEl.dataset.id);
      var badge = part(cardEl, "decision");
      badge.textContent = entry.decision || "UNDECIDED";
      badge.className = "decision " + entry.decision.toLowerCase();
      part(cardEl, "approve").classList.toggle("on", entry.decision === "APPROVED");
      part(cardEl, "reject").classList.toggle("on", entry.decision === "REJECTED");
      part(cardEl, "approve").setAttribute("aria-pressed", String(entry.decision === "APPROVED"));
      part(cardEl, "reject").setAttribute("aria-pressed", String(entry.decision === "REJECTED"));
      if (document.activeElement !== part(cardEl, "comment")) { part(cardEl, "comment").value = entry.comment; }
      var bits = [];
      if (entry.comment) { bits.push("Comment: " + entry.comment); }
      if (entry.reviewer) { bits.push("by " + entry.reviewer + (entry.at ? " at " + entry.at : "")); }
      part(cardEl, "meta").textContent = bits.join(" - ");
      if (entry.decision === "APPROVED") { totals.approved += 1; }
      else if (entry.decision === "REJECTED") { totals.rejected += 1; }
      else { totals.undecided += 1; }
      if (entry.comment) { totals.commented += 1; }
    });
    Object.keys(totals).forEach(function (name) { document.getElementById("total-" + name).textContent = totals[name]; });
  }

  function reviewerOrRefuse(cardEl) {
    var reviewerName = reviewerInput.value.trim();
    var errorEl = part(cardEl, "error");
    if (!reviewerName) {
      errorEl.textContent = "Not recorded: enter your name in the Reviewer name field first.";
      errorEl.hidden = false;
      reviewerInput.classList.add("missing");
      return null;
    }
    errorEl.hidden = true;
    reviewerInput.classList.remove("missing");
    return reviewerName;
  }

  function record(cardEl, change) {
    var reviewerName = reviewerOrRefuse(cardEl);
    if (!reviewerName) { return; }
    var entry = entryOf(cardEl.dataset.id);
    Object.keys(change).forEach(function (name) { entry[name] = change[name]; });
    entry.reviewer = reviewerName;
    entry.at = new Date().toISOString().slice(0, 19) + "Z";
    if (!entry.decision && !entry.comment) { delete decisions[cardEl.dataset.id]; } else { decisions[cardEl.dataset.id] = entry; }
    save();
    render();
  }

  document.querySelectorAll(".card").forEach(function (cardEl) {
    part(cardEl, "approve").addEventListener("click", function () {
      record(cardEl, { decision: entryOf(cardEl.dataset.id).decision === "APPROVED" ? "" : "APPROVED" });
    });
    part(cardEl, "reject").addEventListener("click", function () {
      record(cardEl, { decision: entryOf(cardEl.dataset.id).decision === "REJECTED" ? "" : "REJECTED" });
    });
    part(cardEl, "saveComment").addEventListener("click", function () {
      record(cardEl, { comment: part(cardEl, "comment").value.trim() });
    });
  });

  reviewerInput.addEventListener("input", function () {
    try { localStorage.setItem(reviewerKey, reviewerInput.value); } catch (e) { /* storage unavailable */ }
    if (reviewerInput.value.trim()) { reviewerInput.classList.remove("missing"); }
  });

  function csvCell(value) { return '"' + String(value == null ? "" : value).replace(/"/g, '""') + '"'; }
  document.getElementById("export").addEventListener("click", function () {
    var rows = [["id", "type", "title", "decision", "comment", "reviewer", "decidedAt"]];
    boardData.rules.forEach(function (rule) {
      var entry = entryOf(rule.id);
      rows.push([rule.id, rule.type, rule.title, entry.decision || "UNDECIDED", entry.comment, entry.reviewer, entry.at]);
    });
    var csvText = rows.map(function (row) { return row.map(csvCell).join(","); }).join("\r\n") + "\r\n";
    var link = document.createElement("a");
    link.href = URL.createObjectURL(new Blob([csvText], { type: "text/csv;charset=utf-8" }));
    link.download = "rules-INTCALC-decisions.csv";
    document.body.appendChild(link);
    link.click();
    link.remove();
  });

  var clearButton = document.getElementById("clear");
  var clearConfirm = document.getElementById("clearConfirm");
  var clearCancel = document.getElementById("clearCancel");
  function askClear(asking) { clearButton.hidden = asking; clearConfirm.hidden = !asking; clearCancel.hidden = !asking; }
  clearButton.addEventListener("click", function () { askClear(true); clearConfirm.focus(); });
  clearCancel.addEventListener("click", function () { askClear(false); clearButton.focus(); });
  clearConfirm.addEventListener("click", function () {
    decisions = {};
    save();
    document.querySelectorAll('[data-role="error"]').forEach(function (el) { el.hidden = true; });
    document.querySelectorAll('[data-role="comment"]').forEach(function (el) { el.value = ""; });
    askClear(false);
    render();
    clearButton.focus();
  });
  render();
}());
</script>
</body></html>
'''


def main():
    page = build()
    if "--check" in sys.argv:
        if not boardPath.exists() or boardPath.read_text() != page:
            print(f"FAIL: {boardPath.relative_to(repoRoot)} is out of date with rules.yaml; run `make rules`")
            sys.exit(1)
        print(f"{boardPath.relative_to(repoRoot)} is current with rules.yaml")
        return
    boardPath.write_text(page)
    print(f"wrote {boardPath.relative_to(repoRoot)}: {len(rulebook['rules'])} cards")


if __name__ == "__main__":
    main()
