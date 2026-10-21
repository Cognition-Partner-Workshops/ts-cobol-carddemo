"""Merge the per-group assessment entries into inventory.json and waves.json.

    python3 -m harness.assessment      (run from the spec/ directory)

Entries come from spec/assessment/entries/<group>.json. The wave plan follows the
job order in spec/manifest.yaml; programs per job are read from the JCL.
"""
import json
import re

from . import APP_DIR, REPO_DIR, SPEC_DIR
from .jcl import job_steps, manifest

ASSESSMENT = SPEC_DIR / "assessment"
GROUPS = ["batch", "cics", "db2", "ims-mq", "utility"]
GROUP_NOTES = ("issues", "unresolved", "open_points")
NAME_RE = re.compile(r"^[A-Z][A-Z0-9#@$]{0,7}$")
DYNAMIC = "CDEMO-TO-PROGRAM"


def load_groups():
    return {g: json.loads((ASSESSMENT / "entries" / f"{g}.json").read_text()) for g in GROUPS}


def band(score):
    return None if score is None else "low" if score < 40 else "medium" if score < 80 else "high"


def check_complexity(p):
    c = p.get("complexity") or {}
    if c.get("score") is None:
        return None
    score = c["paragraphs"] + 2 * c["file_io_verbs"] + 3 * c["external_calls"]
    if score != c["score"] or band(score) != c["band"]:
        return f"{p['name']}: complexity {c['score']}/{c['band']} recomputes to {score}/{band(score)}"
    return None


def call_targets(p):
    """(target, kind) for every static CALL/XCTL/LINK target; dynamic targets are skipped."""
    kinds = p.get("external_call_kinds") or {}
    out = []
    for c in p.get("external_calls", []):
        if isinstance(c, dict):
            text, kind = c.get("target", ""), c.get("kind", "CALL")
        else:
            text, kind = c, None
        if DYNAMIC in text:
            continue
        name = text.split()[0] if text else ""
        if kind is None:
            m = re.search(r"\((XCTL|LINK|CALL)\)", text)
            kind = m.group(1) if m else "/".join(kinds.get(name, [])) or "CALL"
        if NAME_RE.match(name):
            out.append((name, kind))
    return out


def dataset_node(d):
    if isinstance(d, str):
        return d
    for k in ("dsn", "queue", "cics_file", "file", "ims_db", "dd"):
        if d.get(k):
            return d[k] if k == "dsn" else f"{k.upper()}:{d[k]}" if k in ("cics_file", "ims_db") else d[k]
    return None


def dataset_via(d):
    if isinstance(d, str):
        return ""
    name = d.get("dd") or d.get("file") or d.get("cics_file") or d.get("queue") or d.get("ims_db") or ""
    return " ".join(x for x in (d.get("access", ""), name) if x).strip()


def jcl_step(ref):
    """'jcl/POSTTRAN.jcl:POSTTRAN.STEP15' -> 'POSTTRAN.STEP15'; a bare path -> its member name."""
    if ":" in ref:
        return ref.split(":", 1)[1]
    return ref.rsplit("/", 1)[-1].split(".")[0]


def merge_programs(groups):
    programs, seen, dupes = [], {}, []
    for g in GROUPS:
        for p in groups[g]["programs"]:
            p = dict(p, group=p.get("group", g))
            if p["name"] in seen:
                kept = seen[p["name"]]
                kept.setdefault("also_in", []).append(g)
                if p.get("evidence") and not kept.get("evidence"):
                    kept["evidence"] = p["evidence"]
                dupes.append(f"{p['name']} is in {kept['group']} and {g}; kept the {kept['group']} entry")
                continue
            seen[p["name"]] = p
            programs.append(p)
    return programs, dupes


def edges_for(programs):
    edges = []
    for p in programs:
        n = p["name"]
        refs = [f"{s['job']}.{s['step']}" for s in p.get("jcl_steps", [])] if not any(":" in r for r in p.get("jcl", [])) else []
        refs += [jcl_step(r) for r in p.get("jcl", []) if ":" in r or not p.get("jcl_steps")]
        for r in dict.fromkeys(refs):
            edges.append({"from": r, "to": n, "via": "EXEC PGM"})
        for t in p.get("cics_transactions", []):
            edges.append({"from": f"TRAN:{t}", "to": n, "via": "CICS transaction"})
        for target, kind in call_targets(p):
            edges.append({"from": n, "to": target, "via": kind})
        for d in p.get("datasets", []):
            if isinstance(d, str):
                continue
            node, via, access = dataset_node(d), dataset_via(d), (d.get("access") or "").lower()
            if not node:
                continue
            if "write" in access or "insert" in access or "delete" in access or "replace" in access:
                edges.append({"from": n, "to": node, "via": via})
            if "read" in access or not access:
                edges.append({"from": node, "to": n, "via": via})
    return edges


def plan_waves(by_name):
    waves, writes = [], {}
    for i, entry in enumerate(manifest()["batch_path"], 1):
        job = entry["job"]
        members = [s["program"] for s in job_steps(job)]
        for m in list(members):
            members += [t for t, _ in call_targets(by_name.get(m, {})) if t in by_name and by_name[t].get("type", "").endswith("subprogram")]
        members = list(dict.fromkeys(members))
        reads = {dataset_node(d): d for m in members for d in by_name.get(m, {}).get("datasets", [])
                 if isinstance(d, dict) and "read" in (d.get("access") or "")}
        after = []
        for up in entry.get("reads_output_of", []):
            shared = sorted(ds for ds in reads if ds in writes.get(up, set()))
            after.append({"job": up, "datasets": shared})
        writes[job] = {dataset_node(d) for m in members for d in by_name.get(m, {}).get("datasets", [])
                       if isinstance(d, dict) and "write" in (d.get("access") or "")}
        deferred = bool(entry.get("optional"))
        waves.append({
            "name": f"Wave {i}: {job}" + (" (deferred)" if deferred else ""),
            "job": job, "jcl": f"app/{entry['jcl']}", "members": members,
            "status": "deferred: inventoried, not specified or proven yet" if deferred else "specify and prove",
            "after": after,
        })
        for m in members:
            if m in by_name:
                by_name[m]["wave"] = waves[-1]["name"]
    return waves


def wave_edges(waves):
    return [{"from": a["job"], "to": w["job"], "via": "reads " + ", ".join(a["datasets"]) if a["datasets"] else "job order"}
            for w in waves for a in w["after"]]


def member_kind(rel):
    parts = rel.split("/")
    return parts[-2] if len(parts) > 2 else "module"


def mapset_names(p):
    m = p.get("bms_mapset") or (p.get("bms") or {}).get("mapset") or []
    return [m] if isinstance(m, str) else m


def references(rel, needles):
    """Other members under app/ (data excluded) and harness modules that name any of the needles."""
    hits = []
    for path in sorted([*APP_DIR.rglob("*"), *(SPEC_DIR / "harness").glob("*.py")]):
        r = str(path.relative_to(REPO_DIR))
        if not path.is_file() or r == rel or "/data/" in r or r.endswith("assessment.py"):
            continue
        body = path.read_text(errors="replace")
        if any(re.search(rf"(?<![A-Za-z0-9.]){re.escape(n)}(?![A-Za-z0-9])", body) for n in needles):
            hits.append(r)
    return hits


def group_coverage(rel, stem, programs, groups, text):
    sweep = {c["member"]: c["covered_by"] for c in groups["utility"].get("sweep", {}).get("coverage", [])}
    by_source = {p.get("source"): p for p in programs if p.get("source")}
    copybooks = {c.upper(): p["name"] for p in programs for c in [*p.get("copybooks", []), *p.get("dcl", [])]}
    mapsets = {m.upper(): p["name"] for p in programs for m in mapset_names(p)}
    module = groups["db2"].get("module", {})
    ctl = {c.upper(): j["name"] for j in module.get("jobs", []) for s in j.get("steps", []) for c in s.get("ctl", [])}
    kind = member_kind(rel)
    p = by_source.get(rel)
    if p and p.get("unreferenced"):
        return {"unreferenced": True, "covered_by": p.get("evidence", f"marked unreferenced by {p['group']}")}
    if p:
        return {"covered_by": f"program entry {p['name']} ({p['group']})"}
    if rel in sweep:
        return {"covered_by": sweep[rel]}
    if kind in ("cpy", "cpy-bms", "dcl") and stem in copybooks:
        return {"covered_by": f"copybook of {copybooks[stem]}"}
    if kind == "bms" and stem in mapsets:
        return {"covered_by": f"mapset of {mapsets[stem]}"}
    if kind == "ctl" and rel.startswith("app/app-transaction-type-db2/"):
        if stem in ctl:
            return {"covered_by": f"control card of job {ctl[stem]} (db2 module)"}
        if stem in [c.upper() for c in module.get("unused_ctl", [])]:
            return {"unreferenced": True, "covered_by": "db2 group: listed in module.unused_ctl"}
    if rel in text or (kind == "EBCDIC" and path_dsn(rel) in text):
        return {"covered_by": "named in a group file (resources, module or issues)"}
    return None


def path_dsn(rel):
    return rel.rsplit("/", 1)[-1].removesuffix(".dat")


def merge_coverage(rel, stem):
    """Evidence found by the merge for a member no group file covers."""
    name = rel.rsplit("/", 1)[-1]
    kind = member_kind(rel)
    if name.lower() == "readme.md" or kind == "catlg":
        return {"covered_by": "merge cross-check: documentation (module README or catalog listing), not a source member",
                "documentation": True}
    needles = {"EBCDIC": [path_dsn(rel)], "ASCII": [name, f"AWS.M2.CARDDEMO.{stem}.PS"]}.get(kind, [name, stem])
    hits = references(rel, needles)
    if hits:
        return {"covered_by": "merge cross-check: named in " + ", ".join(hits)}
    return {"unreferenced": True, "covered_by": "merge cross-check: no member under app/ or spec/harness names it"}


def coverage(programs, groups):
    """Every file under app/ with what covers it: a group file first, else the merge's own cross-check."""
    text = json.dumps(groups)
    members, not_in_groups = [], []
    for path in sorted(APP_DIR.rglob("*")):
        if not path.is_file() or path.name == ".gitkeep":
            continue
        rel = str(path.relative_to(REPO_DIR))
        stem = path.name.split(".")[0].upper()
        found = group_coverage(rel, stem, programs, groups, text)
        if found is None:
            not_in_groups.append(rel)
            found = dict(merge_coverage(rel, stem), source="merge")
        else:
            found["source"] = "group"
        members.append({"member": rel, "kind": member_kind(rel), "unreferenced": False, **found})
    return members, not_in_groups


def build():
    groups = load_groups()
    programs, dupes = merge_programs(groups)
    by_name = {p["name"]: p for p in programs}
    waves = plan_waves(by_name)
    edges = wave_edges(waves) + edges_for(programs)
    issues = [{"group": g, "kind": k, "text": t} for g in GROUPS for k in GROUP_NOTES for t in groups[g].get(k, [])]
    issues += [{"group": "merge", "kind": "dedupe", "text": t} for t in dupes]
    issues += [{"group": "merge", "kind": "complexity", "text": t} for t in filter(None, map(check_complexity, programs))]
    members, not_in_groups = coverage(programs, groups)
    if not_in_groups:
        issues.append({"group": "merge", "kind": "coverage",
                       "text": f"{len(not_in_groups)} app/ members are in no group file; the merge cross-check covers them (members[].source = merge)"})
    neither = [m["member"] for m in members if not m.get("covered_by")]
    group_meta = {g: {k: v for k, v in groups[g].items() if k not in ("programs", *GROUP_NOTES)} for g in GROUPS}
    inventory = {
        "baseline": "baseline/sdd-2610",
        "complexity": "paragraphs + 2 x I/O verbs + 3 x external calls; low < 40, medium 40-79, high >= 80",
        "counts": {g: sum(1 for p in programs if p["group"] == g) for g in GROUPS},
        "programs": programs,
        "members": members,
        "coverage": {"members": len(members), "unreferenced": sorted(m["member"] for m in members if m.get("unreferenced")),
                     "not_in_group_files": not_in_groups, "neither": neither},
        "issues": issues,
        "groups": group_meta,
    }
    (ASSESSMENT / "inventory.json").write_text(json.dumps(inventory, indent=2) + "\n")
    (ASSESSMENT / "waves.json").write_text(json.dumps({"edges": edges, "waves": waves}, indent=2) + "\n")
    return inventory, waves, edges


if __name__ == "__main__":
    inv, waves, edges = build()
    print(json.dumps({"counts": inv["counts"], "coverage": {k: v for k, v in inv["coverage"].items()},
                      "waves": [(w["name"], w["members"], w["after"]) for w in waves], "edges": len(edges)}, indent=1))
