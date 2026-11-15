#!/usr/bin/env python3
"""Write decisions.md: one row per QUIRK and HOLE of rules.yaml, with the effect
of every option on the fixtures as the rulebook evaluator derives it.

  python3 modernization/rules/interest/tools/build_register.py [--check]
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import evaluate  # noqa: E402

registerPath = evaluate.rulesDir / "decisions.md"
ownerPlaceholder = evaluate.ownerPlaceholder


def pathLabel(path):
    if path[0] == "balance":
        return f"balance {path[1]} {path[2]}"
    if path[0] == "account":
        return f"account {path[1]} {path[2]}"
    return " ".join(path)


def show(value):
    if value is None:
        return "(no transaction)"
    if isinstance(value, bool):
        return "yes" if value else "no"
    return str(value)


def optionEffects(rulebook):
    """{(ruleId, optionId): text} - what changes in each fixture against the as-coded outcome."""
    effects = {}
    for caseDir in evaluate.caseDirs(rulebook):
        baseRun, runs = evaluate.optionRuns(rulebook, caseDir)
        for rule, option, run in runs:
            if option.get("asCoded"):
                continue
            changed = evaluate.differences(baseRun, run)
            if not changed:
                continue
            shown = [f"{pathLabel(p)}: {show(baseRun['values'].get(p))} -> {show(run['values'].get(p))}"
                     for p in changed[:1]]
            more = f" (+{len(changed) - 1} more values)" if len(changed) > 1 else ""
            effects.setdefault((rule["id"], option["id"]), []).append(f"`{caseDir.name}`: " + "; ".join(shown) + more)
    return effects


def cellText(text):
    return str(text).replace("|", "\\|").replace("\n", " ")


def build():
    rulebook = evaluate.loadRulebook()
    effects = optionEffects(rulebook)
    caseCount = len(evaluate.caseDirs(rulebook))
    out = [
        "# Decision register - interest stream (INTCALC)",
        "",
        "> **Not SME sign-off.** Every row is a question an agent raised from the code. No business owner has",
        "> answered any of them; every status is OPEN and every owner is a placeholder.",
        "",
        "Generated from `rules.yaml` by `tools/build_register.py`; the effects are computed by the rulebook",
        f"evaluator (`tools/evaluate.py`) on the {caseCount} cases under `fixtures/interest`, from their `input/` files only.",
        "An effect is stated against the as-coded outcome with every other open rule left as coded.",
        "Review and record a view on each on the board, `rules-INTCALC.html`.",
        "",
        "| Id | Type | Question for the business | Options | Effect of each option on the fixtures | Owner | Status |",
        "|---|---|---|---|---|---|---|",
    ]
    for rule in evaluate.undecidedRules(rulebook):
        options, effectLines = [], []
        for option in rule["options"]:
            options.append(f"**{option['id']}** {option['label']}")
            if option.get("asCoded"):
                text = "as coded - the expectations' as-coded candidate"
                if not rule["fixtures"]:
                    text += f" (no fixture exercises it: {rule.get('fixtureNote', '')})"
            elif "notDerivable" in option:
                text = option["notDerivable"]
            elif (rule["id"], option["id"]) in effects:
                text = "; ".join(effects[(rule["id"], option["id"])])
            else:
                text = "no fixture changes" + (f" ({rule['fixtureNote']})" if rule.get("fixtureNote") else "")
            effectLines.append(f"**{option['id']}** {text}")
        out.append("| " + " | ".join(cellText(c) for c in [
            rule["id"], rule["type"], rule["question"] + " _(" + rule["statement"] + ")_",
            "<br>".join(options), "<br>".join(effectLines), ownerPlaceholder, "OPEN"]) + " |")
    out += ["", "## Unresolved cases", "",
            "Cases whose outcome the rules do not determine. The evaluator marks them UNRESOLVED in `expected/<case>.json`.",
            ""]
    unresolvedRows = []
    for caseDir in evaluate.caseDirs(rulebook):
        result = evaluate.expectation(rulebook, caseDir)
        for item in result.get("unresolved", []):
            unresolvedRows.append(f"| `{caseDir.name}` | {item['rule']} | {cellText(item['reason'])} | {ownerPlaceholder} | OPEN |")
    if unresolvedRows:
        out += ["| Case | Rule | Why the rules do not determine it | Owner | Status |", "|---|---|---|---|---|"] + unresolvedRows
    else:
        out.append(f"None: all {caseCount} cases are determined up to the open rules above (each is marked OPEN, with both "
                   "candidate outcomes, in its expectation). INT-H03 and INT-H05 would make a case UNRESOLVED; no fixture "
                   "has an account with several cards or an amount near the field limits.")
    return "\n".join(out) + "\n"


def main():
    text = build()
    if "--check" in sys.argv:
        if not registerPath.exists() or registerPath.read_text() != text:
            print("FAIL: decisions.md is out of date with rules.yaml; run `make rules`")
            sys.exit(1)
        print("decisions.md is current with rules.yaml")
        return
    registerPath.write_text(text)
    print("wrote decisions.md")


if __name__ == "__main__":
    main()
