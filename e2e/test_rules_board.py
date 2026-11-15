"""One test per row of modernization/rules/interest/ui-test-plan.md.

The board is driven through clicks and typing only; nothing calls its functions.
"""
import csv

from playwright.sync_api import expect

from conftest import repoRoot

reviewerName = "Demo Reviewer"
demoComment = 'Demo comment, with a comma and a "quote"'


def card(board, ruleId):
    return board.locator(f"article.card#{ruleId}")


def badge(board, ruleId):
    return card(board, ruleId).locator('[data-role="decision"]')


def totals(board):
    return {name: int(board.locator(f"#total-{name}").inner_text())
            for name in ("approved", "rejected", "commented", "undecided", "cards")}


def decideThree(board):
    board.locator("#reviewer").fill(reviewerName)
    card(board, "INT-R01").get_by_role("button", name="Approve").click()
    card(board, "INT-Q01").get_by_role("button", name="Reject").click()
    card(board, "INT-H01").get_by_label("Comment on INT-H01").fill(demoComment)
    card(board, "INT-H01").get_by_role("button", name="Save comment").click()


def expectThreeDecisions(board, cardCount):
    expect(badge(board, "INT-R01")).to_have_text("APPROVED")
    expect(badge(board, "INT-Q01")).to_have_text("REJECTED")
    expect(card(board, "INT-H01").locator('[data-role="meta"]')).to_contain_text(demoComment)
    expect(card(board, "INT-H01").locator('[data-role="meta"]')).to_contain_text(reviewerName)
    assert totals(board) == {"approved": 1, "rejected": 1, "commented": 1,
                             "undecided": cardCount - 2, "cards": cardCount}


def test_01_page_loads_clean(board, rulebook):
    expect(board.locator("#summary")).to_be_visible()
    expect(board.locator("#summary")).to_contain_text(rulebook["summary"].split()[0])
    for rule in rulebook["rules"]:
        ruleCard = card(board, rule["id"])
        ruleCard.scroll_into_view_if_needed()
        expect(ruleCard).to_be_visible()
        expect(ruleCard.locator(".statement")).to_have_text(rule["statement"])
        expect(ruleCard.locator(".type")).to_have_text(rule["type"])
    assert board.consoleErrors == []


def test_02_card_count_per_type(board, rulebook):
    for ruleType in ("RULE", "QUIRK", "HOLE"):
        entries = sum(1 for rule in rulebook["rules"] if rule["type"] == ruleType)
        assert entries > 0
        expect(board.locator(f'article.card[data-type="{ruleType}"]')).to_have_count(entries)
        expect(board.locator(f"#count-{ruleType}")).to_have_text(str(entries))
    expect(board.locator("article.card")).to_have_count(len(rulebook["rules"]))
    assert totals(board)["cards"] == len(rulebook["rules"])


def test_03_source_excerpt(board, rulebook):
    rule = next(r for r in rulebook["rules"] if r["id"] == "INT-R05")
    excerpt = card(board, "INT-R05").locator("details.excerpt")
    firstBlock = excerpt.locator("pre").first
    expect(firstBlock).to_be_hidden()
    excerpt.locator("summary").click()
    expect(firstBlock).to_be_visible()
    for index, cite in enumerate(rule["source"]):
        path, _, span = cite.rpartition(":")
        first, _, last = span.partition("-")
        first, last = int(first), int(last or first)
        sourceLines = (repoRoot / path).read_text(errors="replace").splitlines()[first - 1:last]
        block = excerpt.locator("pre").nth(index)
        assert block.locator(".ln").all_inner_texts() == [str(n) for n in range(first, last + 1)]
        assert block.locator(".src").all_text_contents() == [line.rstrip() for line in sourceLines]


def test_04_approve_reject_comment(board, rulebook):
    decideThree(board)
    expectThreeDecisions(board, len(rulebook["rules"]))


def test_05_reviewer_name_required(board, rulebook):
    before = totals(board)
    expect(board.locator("#reviewer")).to_have_value("")
    card(board, "INT-R02").get_by_role("button", name="Approve").click()
    expect(badge(board, "INT-R02")).to_have_text("UNDECIDED")
    expect(card(board, "INT-R02").locator('[data-role="error"]')).to_be_visible()
    expect(card(board, "INT-R02").locator('[data-role="error"]')).to_contain_text("name")
    expect(board.locator("#reviewer")).to_have_class("missing")
    assert totals(board) == before
    board.reload()
    expect(badge(board, "INT-R02")).to_have_text("UNDECIDED")


def test_06_decisions_survive_reload(board, rulebook):
    decideThree(board)
    board.reload()
    expectThreeDecisions(board, len(rulebook["rules"]))
    expect(board.locator("#reviewer")).to_have_value(reviewerName)


def test_07_export_csv(board, rulebook, tmp_path):
    decideThree(board)
    with board.expect_download() as downloadInfo:
        board.get_by_role("button", name="Export CSV").click()
    download = downloadInfo.value
    assert download.suggested_filename == "rules-INTCALC-decisions.csv"
    target = tmp_path / download.suggested_filename
    download.save_as(target)
    with target.open(newline="") as handle:
        rows = list(csv.DictReader(handle))
    assert [row["id"] for row in rows] == [rule["id"] for rule in rulebook["rules"]]
    byId = {row["id"]: row for row in rows}
    assert (byId["INT-R01"]["decision"], byId["INT-R01"]["reviewer"]) == ("APPROVED", reviewerName)
    assert (byId["INT-Q01"]["decision"], byId["INT-Q01"]["reviewer"]) == ("REJECTED", reviewerName)
    assert (byId["INT-H01"]["decision"], byId["INT-H01"]["comment"], byId["INT-H01"]["reviewer"]) == (
        "UNDECIDED", demoComment, reviewerName)
    for ruleId, row in byId.items():
        if ruleId not in ("INT-R01", "INT-Q01", "INT-H01"):
            assert (row["decision"], row["comment"], row["reviewer"]) == ("UNDECIDED", "", "")


def test_08_clear_decisions(board, rulebook):
    cardCount = len(rulebook["rules"])
    decideThree(board)
    board.get_by_role("button", name="Clear decisions").click()
    board.get_by_role("button", name="Yes, clear all").click()
    for _ in range(2):
        expect(board.locator('[data-role="decision"]', has_text="UNDECIDED")).to_have_count(cardCount)
        expect(card(board, "INT-H01").get_by_label("Comment on INT-H01")).to_have_value("")
        expect(card(board, "INT-H01").locator('[data-role="meta"]')).to_have_text("")
        assert totals(board) == {"approved": 0, "rejected": 0, "commented": 0,
                                 "undecided": cardCount, "cards": cardCount}
        board.reload()


def test_09_notice_visible_without_scrolling(board):
    notice = board.locator("#notice")
    expect(notice).to_contain_text("Not SME sign-off")
    expect(notice).to_be_in_viewport(ratio=1)
    board.mouse.wheel(0, 3000)
    expect(card(board, "INT-R01")).not_to_be_in_viewport()
    expect(notice).to_be_in_viewport(ratio=1)
