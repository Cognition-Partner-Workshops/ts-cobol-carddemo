#!/usr/bin/env python3
"""Build the design of the interest stream (step 4).

    python3 modernization/design/tools/build_design.py [--check]

Joins design_data.py and tickets_data.py with the stream manifest, the rulebook
and the fixtures, and writes under modernization/design/: design.json (the data
behind the page), architecture.md, data-map-interest.md, decisions-release.md,
strategy-interest.md, strategy-interest.html, tickets/NN-<slug>.md and
tickets/README.md. --check fails when a committed file is out of date.
No target code is produced.
"""
from __future__ import annotations

import json
import sys
from collections import Counter
from decimal import Decimal
from pathlib import Path

import yaml

toolsDir = Path(__file__).resolve().parent
designDir = toolsDir.parent
repoRoot = designDir.parents[1]
sys.path.insert(0, str(toolsDir))
sys.path.insert(0, str(repoRoot))
sys.dont_write_bytecode = True

import design_data as dd  # noqa: E402
import tickets_data as td  # noqa: E402
from tools.parity.copybook import decode_record  # noqa: E402

manifest = json.loads((repoRoot / "modernization/streams/interest/manifest.json").read_text())
rulebook = yaml.safe_load((repoRoot / "modernization/rules/interest/rules.yaml").read_text())
baseline = manifest["meta"]["baseline"]
repository = manifest["meta"]["repository"]


# ------------------------------------------------------------ derived data
def buildDataMap() -> list[dict]:
    rows = []
    tableByCopybook = {t["copybook"]: t for t in dd.tables if t["copybook"]}
    for cb in manifest["scope"]["copybooks"]:
        mapped = dd.fieldMap[cb["name"]]
        table = tableByCopybook[cb["name"]]
        for f in cb["fields"]:
            m = mapped[f["name"]]
            pic = f["pic"] or "group"
            if f["pic"] is None:
                kind = "group"
            elif f["name"] == "FILLER":
                kind = "filler"
            elif pic.startswith("S9"):
                kind = "signed zoned"
            elif pic.startswith("9"):
                kind = "unsigned zoned"
            else:
                kind = "text"
            rule = {"group": "-", "filler": "E4", "signed zoned": "E2", "unsigned zoned": "E1", "text": "E3"}[kind]
            rows.append({"id": f"{cb['name']}/{f['name']}", "copybook": cb["name"], "table": table["name"], "field": f["name"],
                         "level": f["level"], "pic": pic, "offset": f["offset"], "length": f["length"], "kind": kind,
                         "encoding": rule, "column": m["column"], "pgType": m["pgType"], "role": m["role"], "note": m["note"],
                         "source": f"app/cpy/{cb['name']}.cpy:{f['line']}"})
    return rows


dataMap = buildDataMap()
kindCount = Counter(r["kind"] for r in dataMap)
outsideByDataset = Counter(o["dataset"].replace("AWS.M2.CARDDEMO.", "") for o in manifest["outside"])
fmt = {"signedCount": kindCount["signed zoned"], "textCount": kindCount["text"], "fieldCount": len(dataMap),
       "xrefUses": outsideByDataset["CARDXREF.VSAM.KSDS"], "pathUses": outsideByDataset["CARDXREF.VSAM.AIX.PATH"]}


def fill(value):
    if isinstance(value, str):
        for k, v in fmt.items():
            value = value.replace("{" + k + "}", str(v))
        return value
    if isinstance(value, list):
        return [fill(v) for v in value]
    if isinstance(value, dict):
        return {k: fill(v) for k, v in value.items()}
    return value


gaps = fill(dd.gaps)
moveMap = fill(dd.moveMap)
encodingRules = fill(dd.encodingRules)


def readRecords(path: Path, copybook: str, lrecl: int) -> list[dict]:
    if not path.exists():
        return []
    lines = [line.rstrip("\r\n") for line in path.read_text(encoding="latin-1").splitlines()]
    return [decode_record(copybook, line.ljust(lrecl)[:lrecl].encode("latin-1")) for line in lines if line.strip()]


def money(value: Decimal) -> str:
    return format(value.quantize(Decimal("0.01")), "f")


def buildRecon() -> list[dict]:
    """Reconciliation figures of every fixture case, decoded with the estate's own copybook codec."""
    out = []
    for caseDir in sorted((repoRoot / "fixtures/interest").iterdir()):
        inp, exp = caseDir / "input", caseDir / "expected" / "datasets"
        acct = readRecords(inp / "AWS.M2.CARDDEMO.ACCTDATA.PS.txt", "CVACT01Y", 300)
        xref = readRecords(inp / "AWS.M2.CARDDEMO.CARDXREF.PS.txt", "CVACT03Y", 50)
        bal = readRecords(inp / "AWS.M2.CARDDEMO.TCATBALF.PS.txt", "CVTRA01Y", 50)
        rate = readRecords(inp / "AWS.M2.CARDDEMO.DISCGRP.PS.txt", "CVTRA02Y", 50)
        tran = readRecords(exp / "AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt", "CVTRA05Y", 350)
        after = readRecords(exp / "AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt", "CVACT01Y", 300)
        total = lambda recs, field: sum((Decimal(r[field]) for r in recs), Decimal(0))  # noqa: E731
        lastAcct = max(int(r["TRANCAT-ACCT-ID"]) for r in bal) if bal else None
        desc = lambda r: int(r["TRAN-DESC"].split()[-1])  # noqa: E731
        lastInterest = sum((Decimal(r["TRAN-AMT"]) for r in tran if desc(r) == lastAcct), Decimal(0))
        before, afterSum, tranSum = total(acct, "ACCT-CURR-BAL"), total(after, "ACCT-CURR-BAL"), total(tran, "TRAN-AMT")
        out.append({"case": caseDir.name, "accounts": len(acct), "cards": len(xref), "balances": len(bal), "rates": len(rate),
                    "sumCurrBalBefore": money(before), "sumCatBal": money(total(bal, "TRAN-CAT-BAL")),
                    "sumRates": money(total(rate, "DIS-INT-RATE")), "acctIdHash": str(sum(int(r["ACCT-ID"]) for r in acct)),
                    "transactions": len(tran), "sumTranAmt": money(tranSum), "sumCurrBalAfter": money(afterSum),
                    "balanceChange": money(afterSum - before), "lastAccount": f"{lastAcct:011d}",
                    "lastAccountInterest": money(lastInterest),
                    "rc5Holds": tranSum == (afterSum - before) + lastInterest})
    return out


recon = buildRecon()
assert all(r["rc5Holds"] for r in recon), "RC5 control equation does not hold on a fixture"

ruleById = {r["id"]: r for r in rulebook["rules"]}
registerRows = []
for r in rulebook["rules"]:
    if r["type"] == "RULE":
        continue
    switch = next(s for s in dd.policySwitches if s["decision"] == r["id"])
    asCoded = next((o["label"] for o in r["options"] if o.get("asCoded")), None)
    registerRows.append({"id": r["id"], "type": r["type"], "question": r["question"], "status": "OPEN",
                         "asCoded": asCoded or "not determined by the source", "property": switch["property"],
                         "default": switch["default"], "where": switch["where"]})

tickets = []
for t in td.tickets:
    t = fill(dict(t))
    t["id"] = f"{t['n']:02d}"
    t["file"] = f"tickets/{t['id']}-{t['slug']}.md"
    t["workItem"] = f"INT-T{t['id']}"
    tickets.append(t)
ticketById = {t["n"]: t for t in tickets}
knownIds = set(ruleById) | {n["id"] for n in rulebook["technicalNotes"]}
for t in tickets:
    assert set(t["rules"]) <= knownIds, (t["id"], set(t["rules"]) - knownIds)
    assert all(d in ticketById and d < t["n"] for d in t["deps"])

dataItems = [dict(item, phase=p["phase"], phaseTitle=p["title"]) for p in dd.dataMoves for item in p["items"]]

buildSteps = [{"step": i + 1, "ticket": t["id"], "title": t["title"], "outcome": t["outcome"],
               "after": [f"{d:02d}" for d in t["deps"]], "group": t["group"],
               "owner": "engineering lead"} for i, t in enumerate(tickets)]

ownerRows = []
ownerSources = {"moveMap": moveMap, "dataMoves": dataItems, "releaseDecisions": dd.releaseDecisions, "gaps": gaps, "buildSteps": buildSteps}
for role in sorted({x["owner"] for src in ownerSources.values() for x in src}):
    row = {"role": role}
    for key, src in ownerSources.items():
        row[key] = sum(1 for x in src if x["owner"] == role)
    row["total"] = sum(row[k] for k in ownerSources)
    ownerRows.append(row)

counts = {
    "moveMap": len(moveMap), "moves": sum(m["disposition"] == "MOVES" for m in moveMap),
    "bridged": sum(m["disposition"] == "BRIDGED" for m in moveMap), "stays": sum(m["disposition"] == "STAYS" for m in moveMap),
    "components": len(dd.components), "tables": len(dd.tables), "replicaTables": sum(t["kind"] == "replica" for t in dd.tables),
    "fields": len(dataMap), "copybooks": len(manifest["scope"]["copybooks"]), "encodingRules": len(encodingRules), "gaps": len(gaps),
    "buildSteps": len(buildSteps), "tickets": len(tickets), "gateTickets": sum(t["group"] == "gate" for t in tickets),
    "dataItems": len(dataItems), "phases": len(dd.dataMoves),
    "registerDecisions": len(registerRows), "registerOpen": sum(r["status"] == "OPEN" for r in registerRows),
    "quirks": sum(r["type"] == "QUIRK" for r in registerRows), "holes": sum(r["type"] == "HOLE" for r in registerRows),
    "releaseDecisions": len(dd.releaseDecisions), "policySwitches": len(dd.policySwitches),
    "frozenInterfaces": len(dd.frozenInterfaces), "ownerRoles": len(ownerRows), "fixtureCases": len(recon),
    "outsideUsers": len(manifest["outside"]), "proposedItems": len(moveMap) + len(buildSteps) + len(dataItems),
}

design = {
    "meta": {"stream": "interest", "root": dd.streamRoot, "repository": repository, "baseline": baseline,
             "designBase": {"branch": dd.baseBranch, "sha": dd.baseSha}, "designBranch": dd.designBranch,
             "architectureVersion": dd.architectureVersion, "targetStack": dd.targetStack, "status": "PROPOSED - nothing here is approved",
             "generatedBy": "modernization/design/tools/build_design.py"},
    "counts": counts, "moveMap": moveMap, "components": dd.components, "tables": dd.tables, "policySwitches": dd.policySwitches,
    "frozenInterfaces": dd.frozenInterfaces, "dataMap": dataMap, "encodingRules": encodingRules, "gaps": gaps,
    "buildSteps": buildSteps, "dataMoves": [{k: v for k, v in p.items() if k != "items"} for p in dd.dataMoves],
    "dataItems": dataItems, "reconciliation": recon, "registerDecisions": registerRows, "releaseDecisions": dd.releaseDecisions,
    "owners": ownerRows,
    "tickets": [{k: t[k] for k in ("id", "workItem", "slug", "title", "group", "outcome", "deps", "rules", "file")} for t in tickets],
}

import render_md  # noqa: E402
import render_html  # noqa: E402

outputs: dict[str, str] = {"design.json": json.dumps(design, indent=1) + "\n"}
outputs.update(render_md.render(design, tickets, dd, td, manifest))
outputs["strategy-interest.html"] = render_html.render(design, tickets, outputs, dd, td)


def main() -> int:
    check = "--check" in sys.argv
    stale = []
    for name, text in outputs.items():
        path = designDir / name
        if check:
            if not path.exists() or path.read_text() != text:
                stale.append(name)
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text)
    extra = [p.name for p in (designDir / "tickets").glob("*.md") if f"tickets/{p.name}" not in outputs]
    if check and (stale or extra):
        print("out of date: " + ", ".join(stale + extra) + " - run `make design`")
        return 1
    print(("checked " if check else "wrote ") + f"{len(outputs)} files under modernization/design")
    return 0


if __name__ == "__main__":
    sys.exit(main())
