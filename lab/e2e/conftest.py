"""Fixtures of the replay lab page suite: a lab server, the output of the gate, one fresh page per test."""
import os
import re
import subprocess
import sys
import time
import urllib.request
from pathlib import Path

import pytest
from playwright.sync_api import sync_playwright

repoRoot = Path(__file__).resolve().parents[2]
runTimeout = 240000


@pytest.fixture(scope="session")
def labUrl():
    """LAB_E2E_URL names a running lab; otherwise one is started on LAB_E2E_PORT (default 8091)."""
    given = os.environ.get("LAB_E2E_URL")
    if given:
        yield given.rstrip("/")
        return
    port = os.environ.get("LAB_E2E_PORT", "8091")
    server = subprocess.Popen([sys.executable, "lab/server.py"], cwd=repoRoot, env={**os.environ, "LAB_PORT": port},
                              stdout=subprocess.DEVNULL, stderr=subprocess.STDOUT)
    url = f"http://127.0.0.1:{port}"
    try:
        for _ in range(100):
            try:
                urllib.request.urlopen(url + "/", timeout=1).close()
                break
            except OSError:
                time.sleep(0.2)
        else:
            raise RuntimeError("the lab server did not start")
        yield url
    finally:
        server.terminate()
        server.wait(timeout=10)


@pytest.fixture(scope="session")
def gate():
    """`lab/replay.py gate`, what `make lab-gate` runs: its exit code and its lines by case."""
    result = subprocess.run([sys.executable, "lab/replay.py", "gate"], cwd=repoRoot, capture_output=True, text=True)
    countsOf = lambda text: {name: int(value) for name, value in re.findall(  # noqa: E731
        r"(MATCH|INTENDED_CHANGE|UNRESOLVED|UNCLASSIFIED) (\d+)", text)}
    cases, totals = {}, {}
    for line in result.stdout.splitlines():
        row = re.match(r"(golden|probe)\s+(\S+)\s+MATCH", line)
        total = re.match(r"(golden|probe|all) total:", line)
        if row:
            cases[row.group(2)] = countsOf(line)
        elif total:
            totals[total.group(1)] = countsOf(line)
    coverage = re.search(r"^rule coverage: (\d+) of (\d+)", result.stdout, re.M)
    assert cases and coverage, "the gate printed no result:\n" + result.stdout + result.stderr
    return {"exitCode": result.returncode, "cases": cases, "totals": totals, "output": result.stdout,
            "coverage": (int(coverage.group(1)), int(coverage.group(2)))}


@pytest.fixture(scope="session")
def browser():
    with sync_playwright() as playwright:
        channel = os.environ.get("LAB_E2E_CHANNEL")
        launched = playwright.chromium.launch(channel=channel) if channel else playwright.chromium.launch()
        yield launched
        launched.close()


@pytest.fixture
def lab(browser, labUrl):
    """The lab page in a new context, loaded, with its console and page errors collected."""
    context = browser.new_context(viewport={"width": 1500, "height": 950})
    page = context.new_page()
    page.set_default_timeout(runTimeout)
    page.consoleErrors = []
    page.on("console", lambda message: page.consoleErrors.append(message.text) if message.type == "error" else None)
    page.on("pageerror", lambda error: page.consoleErrors.append(str(error)))
    page.goto(labUrl + "/")
    page.wait_for_selector("#goldenList li")
    yield page
    context.close()


def waitDone(page, count):
    page.wait_for_function(
        "n => document.getElementById('status').textContent.startsWith('Done: ' + n + ' case')", arg=count)


def rowCounts(row):
    return {name: int(row.locator(f"td[data-class={name}]").inner_text())
            for name in ("MATCH", "INTENDED_CHANGE", "UNRESOLVED", "UNCLASSIFIED")}

