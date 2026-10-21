"""Compare two runs of the batch path record by record.

Four measures per output dataset: record count, field profile, keyed record
hash and sampled value differences. A difference is expected only when an
entry in spec/expected-differences.yaml covers it, and every entry links the
change request that approved it. Anything else is unexplained and fails.
"""
import hashlib
import json
from collections import Counter
from decimal import Decimal
from pathlib import Path

import yaml

from . import SPEC_DIR
from .datasets import OUTPUTS, record_key
from .inputs import read_jsonl


def load_expected(path=None):
    path = Path(path or SPEC_DIR / "expected-differences.yaml")
    if not path.exists():
        return []
    doc = yaml.safe_load(path.read_text()) or {}
    entries = doc.get("expected_differences") or []
    for e in entries:
        if not e.get("change_request"):
            raise ValueError(f"expected difference {e.get('id')} has no change_request link")
    return entries


def _matches(entry, dataset, field, rec_left, rec_right, key):
    if entry.get("dataset") != dataset:
        return False
    fields = entry.get("fields")
    if fields and field not in fields:
        return False
    keys = entry.get("keys")
    if keys and key not in [str(k) for k in keys]:
        return False
    where = entry.get("where") or {}
    for rec in (rec_left, rec_right):
        if rec and all(str(rec.get(k, "")) == str(v) for k, v in where.items()):
            return True
    return not where


def profile(records, layout):
    out = {}
    for f in layout.data_fields:
        values = [r.get(f.name, "") for r in records]
        if f.numeric:
            nums = [Decimal(v) for v in values] or [Decimal(0)]
            out[f.name] = {"sum": f"{sum(nums):f}", "min": f"{min(nums):f}", "max": f"{max(nums):f}"}
        else:
            out[f.name] = {"distinct": len(set(values)), "blank": sum(1 for v in values if v == "")}
    return out


def digest(rec):
    return hashlib.sha256(json.dumps(rec, sort_keys=True).encode()).hexdigest()


def compare_dataset(name, left, right, expected, sample_size):
    ds = OUTPUTS[name]
    lmap = {record_key(ds, r): r for r in left}
    rmap = {record_key(ds, r): r for r in right}
    diffs = []
    for side, records in (("left", left), ("right", right)):
        seen = Counter(record_key(ds, r) for r in records)
        for key, n in sorted(seen.items()):
            if n > 1:
                diffs.append({"key": key, "field": f"duplicate key on {side}", "left": None, "right": None,
                              "expected": None, "change_request": None})
    matched = 0
    for key in sorted(set(lmap) | set(rmap)):
        lr, rr = lmap.get(key), rmap.get(key)
        if lr is not None and rr is not None and digest(lr) == digest(rr):
            matched += 1
            continue
        fields = ["*"] if lr is None or rr is None else [f for f in sorted(set(lr) | set(rr)) if lr.get(f) != rr.get(f)]
        for field in fields:
            entry = next((e for e in expected if _matches(e, name, field, lr, rr, key)), None)
            diffs.append({
                "key": key, "field": field if field != "*" else ("missing on right" if rr is None else "missing on left"),
                "left": None if lr is None else (lr.get(field) if field != "*" else "present"),
                "right": None if rr is None else (rr.get(field) if field != "*" else "present"),
                "expected": entry["id"] if entry else None,
                "change_request": entry["change_request"] if entry else None,
            })
    lp, rp = profile(left, ds.layout), profile(right, ds.layout)
    unexplained = [d for d in diffs if not d["expected"]]
    return {
        "dataset": name,
        "record_count": {"left": len(left), "right": len(right), "match": len(left) == len(right)},
        "field_profile": {"fields": len(lp), "differing": sorted(f for f in lp if lp[f] != rp.get(f)),
                          "left": lp, "right": rp},
        "keyed_record_hash": {"matched": matched, "differing": len({d["key"] for d in diffs}),
                              "left_only": len(set(lmap) - set(rmap)), "right_only": len(set(rmap) - set(lmap))},
        "sampled_value_differences": unexplained[:sample_size] + [d for d in diffs if d["expected"]][:sample_size],
        "unexplained": len(unexplained),
        "expected": len(diffs) - len(unexplained),
        "verdict": "match" if not diffs else ("expected differences only" if not unexplained else "unexplained differences"),
    }


def compare(run_dir, left="cobol", right="java", expected_path=None, sample_size=10):
    run_dir = Path(run_dir)
    expected = load_expected(expected_path)
    datasets = []
    for name in OUTPUTS:
        paths = {side: run_dir / side / f"{name}.jsonl" for side in (left, right)}
        d = compare_dataset(name, read_jsonl(paths[left]), read_jsonl(paths[right]), expected, sample_size)
        missing = [side for side, p in paths.items() if not p.exists()]
        if missing:
            d["sampled_value_differences"].insert(0, {"key": "*", "field": f"no {name}.jsonl on {', '.join(missing)}",
                                                      "left": None, "right": None, "expected": None, "change_request": None})
            d.update(missing_output=missing, unexplained=d["unexplained"] + 1, verdict="missing output")
        datasets.append(d)
    meta = {}
    for side in (left, right):
        p = run_dir / side / "run.json"
        meta[side] = json.loads(p.read_text()) if p.exists() else {}
    unexplained = sum(d["unexplained"] for d in datasets)
    report = {
        "left": left, "right": right,
        "left_jobs": meta[left].get("jobs"), "right_jobs": meta[right].get("jobs"),
        "datasets": datasets, "unexplained": unexplained,
        "expected": sum(d["expected"] for d in datasets),
        "expected_entries": expected,
        "verdict": "fail" if unexplained else "pass",
    }
    out = run_dir / "report"
    out.mkdir(parents=True, exist_ok=True)
    (out / "comparison.json").write_text(json.dumps(report, indent=2))
    return report
