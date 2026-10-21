"""The merged assessment covers every app/ member and orders the waves by job dependency."""
import json

from harness import APP_DIR, SPEC_DIR, site

ASSESSMENT = SPEC_DIR / "assessment"


def load(name):
    return json.loads((ASSESSMENT / name).read_text())


def test_every_app_member_is_inventoried_or_unreferenced():
    inv = load("inventory.json")
    members = {m["member"]: m for m in inv["members"]}
    on_disk = {str(p.relative_to(APP_DIR.parent)) for p in APP_DIR.rglob("*") if p.is_file() and p.name != ".gitkeep"}
    assert on_disk == set(members)
    assert all(m["covered_by"] for m in members.values())
    assert inv["coverage"]["neither"] == []


def test_program_names_are_unique():
    names = [p["name"] for p in load("inventory.json")["programs"]]
    assert len(names) == len(set(names))


def test_posttran_runs_before_intcalc_and_creastmt_is_deferred_last():
    waves = load("waves.json")["waves"]
    jobs = [w["job"] for w in waves]
    assert jobs.index("POSTTRAN") < jobs.index("INTCALC")
    assert waves[-1]["job"] == "CREASTMT" and "deferred" in waves[-1]["status"]
    intcalc = next(w for w in waves if w["job"] == "INTCALC")
    assert any("TCATBALF" in d for a in intcalc["after"] if a["job"] == "POSTTRAN" for d in a["datasets"])


def test_unreferenced_members_and_utilities_stay_out_of_the_waves():
    inv = load("inventory.json")
    in_waves = {m for w in load("waves.json")["waves"] for m in w["members"]}
    for p in inv["programs"]:
        if p.get("unreferenced") or p["group"] == "utility":
            assert p["name"] not in in_waves


def test_page_shows_the_assessment_sections(tmp_path):
    html, md, _ = site.assessment(tmp_path, "test")
    for heading in ("Inventory", "Complexity per program", "Dependency graph", "Unreferenced members", "Wave plan", "Open issues"):
        assert f"<h2>{heading}</h2>" in html and f"## {heading}" in md
    assert "CREASTMT (deferred)" in html and "UNUSED1Y" in html
