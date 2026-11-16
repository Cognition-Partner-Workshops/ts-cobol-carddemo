#!/usr/bin/env python3
"""The replay lab page: one local page over lab/replay.py.

    python3 lab/server.py            http://127.0.0.1:8090  (LAB_PORT, LAB_HOST override)

GET  /                 the page
GET  /api/meta         header (toolchain, baseline, frozen date), the cases, the limits
POST /api/runs         {"kind": "golden"|"probe", "name": ...} or {"kind": "custom", "input": "<json>"};
                       answers {"accepted": true, "runId"} or {"accepted": false, "error", "problems"}
GET  /api/runs/<id>    queued | running | done (with the result) | failed
GET  /decisions        the decision register, one anchor per row (#INT-Q01)
GET  /board            the rules review board (modernization/rules/interest/rules-INTCALC.html)

Runs are queued and executed one at a time; the server keeps no "latest result", so a page that
is reloaded starts empty and can only show runs it started itself.
"""
import html
import json
import os
import queue
import re
import sys
import threading
import time
import uuid
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

labDir = Path(__file__).resolve().parent
sys.path.insert(0, str(labDir))
sys.dont_write_bytecode = True
import cases  # noqa: E402
import replay  # noqa: E402

boardPath = replay.rulesDir / "rules-INTCALC.html"
customTemplate = {
    "accounts": [{"id": 4001, "balance": "1500.00", "group": "GOLD", "cycleCredit": "25.00", "cycleDebit": "10.00"},
                 {"id": 4002, "balance": "250.00", "group": "STANDARD"}],
    "balances": [{"account": 4001, "type": "01", "category": 1, "balance": "1235.50"},
                 {"account": 4001, "type": "01", "category": 2, "balance": "400.00"},
                 {"account": 4002, "type": "01", "category": 1, "balance": "250.00"}]}
jobs, jobQueue, jobsLock = {}, queue.Queue(), threading.Lock()
maxJobs = 300
metaCache = {}


def worker():
    while True:
        jobId = jobQueue.get()
        job = jobs[jobId]
        job.update(status="running", startedAt=time.strftime("%H:%M:%S"))
        try:
            caseRef = replay.caseRefFor(job["kind"], job.get("name"), job.get("spec"))
            result = replay.runCase(caseRef, runId=jobId)
            result["finishedAt"] = time.strftime("%Y-%m-%d %H:%M:%S")
            job.update(status="done", result=result)
        except (replay.LabError, cases.InvalidCase) as error:
            job.update(status="failed", error=str(error))
        except Exception as error:      # the page must get an answer whatever went wrong
            job.update(status="failed", error=f"the run stopped unexpectedly: {type(error).__name__}: {error}")


def submit(body):
    kind = body.get("kind")
    job = {"kind": kind, "status": "queued", "queuedAt": time.strftime("%H:%M:%S")}
    if kind == "custom":
        text = body.get("input")
        if not isinstance(text, str) or not text.strip():
            raise cases.InvalidCase(["the input is empty - type the accounts and balances as JSON"])
        try:
            spec = json.loads(text)
        except json.JSONDecodeError as error:
            raise cases.InvalidCase([f"the input is not valid JSON: {error.msg} at line {error.lineno}, "
                                     f"column {error.colno}"])
        cases.checkCase(spec)
        job["spec"] = spec
        job["name"] = "custom"
    elif kind == "golden":
        if body.get("name") not in cases.goldenCases():
            raise cases.InvalidCase([f"'{body.get('name')}' is not a golden case"])
        job["name"] = body["name"]
    elif kind == "probe":
        if body.get("name") not in [p["name"] for p in cases.loadProbes()]:
            raise cases.InvalidCase([f"'{body.get('name')}' is not an edge probe"])
        job["name"] = body["name"]
    else:
        raise cases.InvalidCase(["kind must be golden, probe or custom"])
    jobId = uuid.uuid4().hex[:10]
    with jobsLock:
        for old in list(jobs)[:max(0, len(jobs) - maxJobs)]:
            if jobs[old]["status"] in ("done", "failed"):
                del jobs[old]
        jobs[jobId] = job
    jobQueue.put(jobId)
    return jobId


def meta():
    if not metaCache:
        rulebook = replay.loadRulebook()
        decisions = replay.loadDecisions()
        metaCache.update({
            "header": replay.headerInfo(),
            "golden": [{"name": n, "note": cases.goldenNote(n)} for n in cases.goldenCases()],
            "probes": [{"name": p["name"], "title": p["title"], "stresses": p["stresses"]}
                       for p in cases.loadProbes()],
            "customTemplate": json.dumps(customTemplate, indent=1),
            "rules": [{"id": r["id"], "type": r["type"], "title": r["title"]} for r in rulebook["rules"]],
            "decisions": {k: {"status": v["status"], "approvedOption": v["approvedOption"]}
                          for k, v in decisions.items()},
            "limits": replay.limits, "classes": replay.classes})
    # the toolchain is read once; the commit of the checkout is read on every request, it can change under a running lab
    metaCache["header"].update(labCommit=replay.gitValue("rev-parse", "HEAD"),
                               branch=replay.gitValue("rev-parse", "--abbrev-ref", "HEAD"))
    return metaCache


def inlineMarkdown(text):
    text = html.escape(text.replace("\\|", "|")).replace("&lt;br&gt;", "<br>")
    text = re.sub(r"\*\*(.+?)\*\*", r"<b>\1</b>", text)
    text = re.sub(r"`(.+?)`", r"<code>\1</code>", text)
    return re.sub(r"(?<![\w>])_(.+?)_(?![\w<])", r"<i>\1</i>", text)


def decisionsPage():
    """decisions.md as HTML; every register row gets its id as anchor."""
    out, inTable, paragraph = [], False, []

    def flush():
        if paragraph:
            out.append("<p>" + inlineMarkdown(" ".join(paragraph)) + "</p>")
            paragraph.clear()

    for line in replay.decisionsPath.read_text().splitlines():
        if line.startswith("|"):
            flush()
            cells = [c.strip() for c in re.split(r"(?<!\\)\|", line)[1:-1]]
            if all(re.fullmatch(r":?-+:?", c) for c in cells):
                continue
            if not inTable:
                out.append("<table><thead><tr>" + "".join(f"<th>{inlineMarkdown(c)}</th>" for c in cells)
                           + "</tr></thead><tbody>")
                inTable = True
                continue
            rowId = cells[0] if re.fullmatch(r"INT-[QH]\d+", cells[0]) else ""
            status = html.escape(cells[-1])
            out.append(f'<tr id="{rowId}" data-status="{status}">'
                       + "".join(f'<td class="c{i}">{inlineMarkdown(c)}</td>' for i, c in enumerate(cells)) + "</tr>")
            continue
        if inTable:
            out.append("</tbody></table>")
            inTable = False
        if line.startswith("# "):
            flush()
            out.append(f"<h1>{inlineMarkdown(line[2:])}</h1>")
        elif line.startswith("## "):
            flush()
            out.append(f"<h2>{inlineMarkdown(line[3:])}</h2>")
        elif line.startswith(">"):
            paragraph.append(line.lstrip("> "))
        elif not line.strip():
            flush()
        else:
            paragraph.append(line)
    flush()
    if inTable:
        out.append("</tbody></table>")
    style = ("body{font:15px/1.45 system-ui,sans-serif;margin:24px;color:#1b1f24}table{border-collapse:collapse}"
             "td,th{border:1px solid #c9d1d9;padding:6px 8px;vertical-align:top;text-align:left}th{background:#f3f5f7}"
             "tr:target{background:#fff3bf;outline:3px solid #e8590c}td.c6{font-weight:700}"
             "code{background:#eef1f4;padding:0 3px}a{color:#0b5fbf}")
    return ("<!doctype html><html lang='en'><head><meta charset='utf-8'><title>Decision register - interest stream"
            f"</title><style>{style}</style></head><body><p><a href='/'>Back to the replay lab</a> - rendered from "
            "<code>modernization/rules/interest/decisions.md</code>; the highlighted row is the one the lab linked to."
            "</p>" + "\n".join(out) + "</body></html>")


class Handler(BaseHTTPRequestHandler):
    server_version = "ReplayLab/1"

    def log_message(self, pattern, *args):
        if os.environ.get("LAB_VERBOSE"):
            super().log_message(pattern, *args)

    def send(self, status, body, contentType="application/json"):
        data = body if isinstance(body, bytes) else (
            body if isinstance(body, str) else json.dumps(body)).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", contentType + ("; charset=utf-8" if "image" not in contentType else ""))
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self):
        path = self.path.split("?")[0]
        if path in ("/", "/index.html"):
            self.send(200, (labDir / "page.html").read_bytes(), "text/html")
        elif path == "/favicon.ico":
            self.send(200, b'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16"><rect width="16" height="16" '
                           b'rx="3" fill="#0b5fbf"/></svg>', "image/svg+xml")
        elif path == "/api/meta":
            self.send(200, meta())
        elif path.startswith("/api/runs/"):
            job = jobs.get(path.rsplit("/", 1)[1])
            if job is None:
                self.send(404, {"error": "this run is not known to the lab server (it may have been restarted)"})
            else:
                self.send(200, {k: v for k, v in job.items() if k != "spec"})
        elif path == "/decisions":
            self.send(200, decisionsPage(), "text/html")
        elif path == "/board":
            self.send(200, boardPath.read_bytes(), "text/html")
        else:
            self.send(404, {"error": "not found"})

    def do_POST(self):
        if self.path != "/api/runs":
            self.send(404, {"error": "not found"})
            return
        try:
            length = int(self.headers.get("Content-Length") or 0)
            if length > 200000:
                raise cases.InvalidCase(["the input is too large (200 kB at most)"])
            body = json.loads(self.rfile.read(length) or b"{}")
            if not isinstance(body, dict):
                raise cases.InvalidCase(["the request must be a JSON object"])
            self.send(200, {"accepted": True, "runId": submit(body)})
        except json.JSONDecodeError:
            self.send(400, {"error": "the request is not valid JSON"})
        except cases.InvalidCase as error:
            # an input the lab refuses is an answer, not a failed request: 200, so the browser logs no error
            self.send(200, {"accepted": False, "error": "The input cannot be run.", "problems": error.problems})


def main():
    host, port = os.environ.get("LAB_HOST", "127.0.0.1"), int(os.environ.get("LAB_PORT", "8090"))
    threading.Thread(target=worker, daemon=True).start()
    server = ThreadingHTTPServer((host, port), Handler)
    print(f"replay lab: http://{host}:{port}/   (Ctrl-C stops it)", flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("replay lab stopped")


if __name__ == "__main__":
    main()
