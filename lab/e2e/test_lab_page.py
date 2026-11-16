"""The UI test plan of the replay lab page (lab/ui-test-plan.md), one test per row."""
import json
import re
import subprocess
import time

from conftest import repoRoot, rowCounts, waitDone

goldenCases = sorted(p.name for p in (repoRoot / "fixtures" / "interest").iterdir() if (p / "input").is_dir())
probes = [p["name"] for p in json.loads((repoRoot / "lab" / "probes.json").read_text())["probes"]]
tieCase = "i02-tie-half-cent"


def summaryRows(page):
    return page.locator("#summaryBody tr[data-case]")


def fixturesState():
    status = subprocess.run(["git", "status", "--short", "fixtures"], cwd=repoRoot, capture_output=True, text=True)
    files = [p for p in (repoRoot / "fixtures").rglob("*") if p.is_file()]
    return status.stdout.strip(), len(files), max(p.stat().st_mtime for p in files)


def runTie(lab):
    lab.click(f"#goldenList button[data-name={tieCase}]")
    waitDone(lab, 1)
    return lab.locator(f"#case-golden-{tieCase}")


def test_01_page_loads_clean_with_complete_header(lab):
    header = lab.locator("header")
    toolchain = header.locator("#toolchain").inner_text()
    for tool in ("GnuCOBOL", "Open COBOL ESQL", "Python", "Java", "PostgreSQL", "Docker"):
        assert re.search(rf"{tool}[^\n]*? \d+\.\d+", toolchain), f"no version of {tool} in: {toolchain}"
    sha = re.search(r'"sha":\s*"([0-9a-f]{40})"',
                    (repoRoot / "modernization/streams/interest/manifest.json").read_text()).group(1)
    clock = json.loads((repoRoot / "tools/runjcl/chains.json").read_text())["interest"]["clock"]
    assert header.locator("#baselineSha").inner_text() == sha
    assert header.locator("#businessDate").inner_text() == clock
    assert lab.locator("#gateVerdict").inner_text() == "NOT RUN"
    assert lab.consoleErrors == []


def test_02_golden_all_seven_cases_equal_the_gate(lab, gate):
    lab.click("#runAllGolden")
    waitDone(lab, 7)
    rows = summaryRows(lab)
    assert [rows.nth(i).get_attribute("data-case") for i in range(rows.count())] == goldenCases
    for name in goldenCases:
        assert rowCounts(lab.locator(f"#summaryBody tr[data-case={name}]")) == gate["cases"][name], name
        lab.click(f"#summaryBody tr[data-case={name}]")
        section = lab.locator(f"#case-golden-{name}")
        assert section.locator(".lane.legacy").is_visible() and section.locator(".lane.target").is_visible()
        for engine in ("legacy", "target"):
            lane = section.locator(f".lane.{engine}")
            assert "normal end, return code 0" in lane.inner_text()
            assert lane.locator("table").count() == 2 and lane.locator("pre").count() == 1
        assert "PASS" in section.locator(".lane.legacy").inner_text()      # rerun against the golden files
        assert section.locator("table.items").is_visible()
        assert section.locator("table.items tbody tr").count() == sum(gate["cases"][name].values())
        assert section.locator("table.verdicts tbody tr").count() == 20
    assert rowCounts(lab.locator("#summaryTotal")) == gate["totals"]["golden"]
    assert lab.consoleErrors == []


def test_03_tie_case_shows_the_two_documented_fields_with_the_register_class(lab):
    readme = (repoRoot / "fixtures/interest" / tieCase / "README.md").read_text()
    assert "TRAN-AMT and ACCT-CURR-BAL" in readme and "account 2003 only" in readme
    register = (repoRoot / "modernization/rules/interest/decisions.md").read_text()
    registerRow = next(line for line in register.splitlines() if line.startswith("| INT-Q01 |"))
    expectedClass = "UNRESOLVED" if registerRow.rstrip(" |").endswith("OPEN") else "INTENDED_CHANGE"
    section = runTie(lab)
    for where, field, value in (("balance 00000002003/01/0001", "TRAN-AMT", "12.35"),
                                ("account 00000002003", "ACCT-CURR-BAL", "1247.85")):
        row = section.locator(f'table.items tr[data-where="{where}"][data-field="{field}"]')
        assert row.count() == 1 and row.is_visible()
        assert row.get_attribute("data-class") == expectedClass
        cells = row.locator("td")
        assert cells.nth(3).inner_text() == value and cells.nth(4).inner_text() == value
        row.locator("summary").click()
        assert row.locator("a.decisionLink").get_attribute("data-decision") == "INT-Q01"
        assert "A (as coded) gives " + value in row.locator(".options").inner_text()


def test_04_unresolved_item_links_to_its_open_register_row(lab):
    section = runTie(lab)
    row = section.locator('table.items tr[data-class=UNRESOLVED][data-field="TRAN-AMT"]').first
    row.locator("summary").click()
    with lab.context.expect_page() as opened:
        row.locator("a.decisionLink").click()
    register = opened.value
    register.wait_for_load_state()
    assert register.url.endswith("/decisions#INT-Q01")
    registerRow = register.locator("tr#INT-Q01")
    assert registerRow.count() == 1 and registerRow.is_visible()
    assert "cut off at the cent, or rounded" in registerRow.inner_text()
    assert registerRow.locator("td").last.inner_text().strip() == "OPEN"
    assert register.evaluate("document.querySelector('tr:target').id") == "INT-Q01"
    assert summaryRows(lab).count() == 1                                   # the lab tab kept its result
    # a browser that opens the link in the same tab: Back returns to the lab with the result still there
    lab.goto(register.url)
    assert lab.locator("tr#INT-Q01 td").last.inner_text().strip() == "OPEN"
    lab.go_back()
    lab.wait_for_selector("#goldenList li")
    lab.wait_for_function("document.getElementById('status').textContent.startsWith('Back on the lab page')")
    assert summaryRows(lab).count() == 1 and summaryRows(lab).first.get_attribute("data-case") == tieCase
    assert lab.locator(f"#case-golden-{tieCase} table.items tr[data-class=UNRESOLVED]").count() == 11


def test_05_every_edge_probe_returns_a_result_and_fixtures_stay_untouched(lab):
    before = fixturesState()
    started = time.time()
    lab.click("#tab-probes")
    lab.click("#runAllProbes")
    waitDone(lab, len(probes))
    rows = summaryRows(lab)
    assert [rows.nth(i).get_attribute("data-case") for i in range(rows.count())] == probes
    for name in probes:
        lab.click(f"#summaryBody tr[data-case={name}]")
        section = lab.locator(f"#case-probe-{name}")
        assert sum(rowCounts(lab.locator(f"#summaryBody tr[data-case={name}]")).values()) > 0
        assert "No golden file for this case" in section.locator(".lane.legacy").inner_text()
        for engine in ("legacy", "target"):
            assert section.locator(f".lane.{engine} table").first.locator("tbody tr").count() >= 1
        assert section.locator("table.verdicts tbody tr").count() == 20
    after = fixturesState()
    assert before[0] == "" and after[0] == "", "git status --short fixtures is not empty"
    assert after[1] == before[1] and after[2] == before[2] and after[2] < started
    assert lab.consoleErrors == []


def test_06_custom_valid_runs_invalid_is_refused_and_the_page_stays_usable(lab):
    lab.click("#tab-custom")
    lab.click("#runCustom")
    waitDone(lab, 1)
    assert summaryRows(lab).count() == 1 and summaryRows(lab).first.get_attribute("data-kind") == "custom"
    example = lab.input_value("#customInput")
    lab.fill("#customInput", example.rstrip()[:-1])                        # the closing brace is gone
    lab.click("#runCustom")
    error = lab.locator("#customError")
    error.wait_for(state="visible")
    assert "not valid JSON" in error.inner_text() and "Nothing was run" in error.inner_text()
    assert summaryRows(lab).count() == 1
    lab.fill("#customInput", example.replace('"account": 4002', '"account": 4999'))
    lab.click("#runCustom")
    lab.wait_for_function("document.getElementById('customError').textContent.includes('4999')")
    assert "account 4999 is not in 'accounts'" in error.inner_text()
    assert summaryRows(lab).count() == 1 and lab.locator("#gateVerdict").inner_text() == "PASS"
    lab.click("#resetCustom")
    assert not error.is_visible()
    lab.click("#runCustom")
    waitDone(lab, 1)
    assert summaryRows(lab).count() == 2
    lab.click("#tab-golden")
    lab.click("#goldenList button[data-name=i01-baseline]")
    waitDone(lab, 1)
    assert summaryRows(lab).count() == 3
    assert lab.consoleErrors == []


def test_07_gate_on_the_page_equals_the_exit_code_of_the_gate(lab, gate):
    lab.click("#runAllGolden")
    waitDone(lab, 7)
    lab.click("#tab-probes")
    lab.click("#runAllProbes")
    waitDone(lab, len(probes))
    assert lab.locator("#gateVerdict").inner_text() == ("PASS" if gate["exitCode"] == 0 else "FAIL")
    assert rowCounts(lab.locator("#summaryTotal")) == gate["totals"]["all"]
    exercised, total = gate["coverage"]
    assert f"Rule coverage: {exercised} of {total} rules exercised" in lab.locator("#gateCoverage").inner_text()
    assert f"UNCLASSIFIED {gate['totals']['all']['UNCLASSIFIED']}" in lab.locator("#gateCounts").inner_text()


def test_08_link_to_the_rules_board_opens_the_board(lab):
    with lab.context.expect_page() as opened:
        lab.click("#boardLink")
    board = opened.value
    board.wait_for_load_state()
    assert board.url.endswith("/board")
    assert "Not SME sign-off" in board.locator("#notice").inner_text()
    assert board.locator("article.card").count() == 20
    lab.goto(board.url)                                                    # same-tab browsers: the board, then Back
    assert lab.locator("article.card").count() == 20
    lab.go_back()
    lab.wait_for_selector("#goldenList li")
    assert lab.locator("#gateVerdict").inner_text() == "NOT RUN" and lab.consoleErrors == []


def test_09_reload_during_a_run_shows_no_stale_result(lab):
    lab.click("#goldenList button[data-name=i01-baseline]")
    lab.wait_for_function("document.getElementById('status').className === 'busy'")
    lab.reload()
    lab.wait_for_selector("#goldenList li")
    assert summaryRows(lab).count() == 0 and lab.locator("#summaryEmpty").is_visible()
    assert lab.locator("#gateVerdict").inner_text() == "NOT RUN" and lab.locator("details.case").count() == 0
    lab.click("#goldenList button[data-name=i03-zero-rate-skipped]")
    waitDone(lab, 1)
    lab.wait_for_timeout(4000)                                             # the interrupted run has ended by now
    rows = summaryRows(lab)
    assert rows.count() == 1 and rows.first.get_attribute("data-case") == "i03-zero-rate-skipped"
    assert lab.locator("details.case").count() == 1
    assert lab.locator("details.case").get_attribute("data-case") == "i03-zero-rate-skipped"
    assert lab.consoleErrors == []
