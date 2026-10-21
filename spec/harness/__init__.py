"""Execution harness for the CardDemo batch specification.

One shared loader decodes the ASCII seed files (zoned decimal with overpunch
signs) for every run, so the COBOL run and the Java run read the same values.
"""
from pathlib import Path

SPEC_DIR = Path(__file__).resolve().parent.parent
REPO_DIR = SPEC_DIR.parent
APP_DIR = REPO_DIR / "app"
