"""Fixtures for the rules review board suite: one fresh browser context per test."""
import os
from pathlib import Path

import pytest
import yaml
from playwright.sync_api import sync_playwright

repoRoot = Path(__file__).resolve().parents[1]
rulesDir = repoRoot / "modernization" / "rules" / "interest"
boardUrl = (rulesDir / "rules-INTCALC.html").as_uri()


@pytest.fixture(scope="session")
def rulebook():
    return yaml.safe_load((rulesDir / "rules.yaml").read_text())


@pytest.fixture(scope="session")
def browser():
    with sync_playwright() as playwright:
        channel = os.environ.get("BOARD_E2E_CHANNEL")
        launched = playwright.chromium.launch(channel=channel) if channel else playwright.chromium.launch()
        yield launched
        launched.close()


@pytest.fixture
def board(browser):
    """The board in a new context (empty localStorage), with its console and page errors collected."""
    context = browser.new_context(viewport={"width": 1280, "height": 800}, accept_downloads=True)
    page = context.new_page()
    page.consoleErrors = []
    page.on("console", lambda message: page.consoleErrors.append(message.text) if message.type == "error" else None)
    page.on("pageerror", lambda error: page.consoleErrors.append(str(error)))
    page.goto(boardUrl)
    yield page
    context.close()
