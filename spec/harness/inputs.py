"""Assemble one run's input: the upstream seed files, the overlay records and the daily file."""
import json
from pathlib import Path

from . import APP_DIR, SPEC_DIR
from .datasets import INPUTS, record_key
from .records import decode_record

SEED_DIR = APP_DIR / "data" / "ASCII"
FIXTURE_DIR = SPEC_DIR / "fixtures"


def read_lines(path):
    if not path.exists():
        return []
    return [line.rstrip("\r\n") for line in path.read_text().splitlines() if line.strip()]


def source_lines(dataset):
    if dataset.name == "dailytran":
        return read_lines(FIXTURE_DIR / "dailytran.txt")
    return read_lines(SEED_DIR / dataset.seed) + read_lines(FIXTURE_DIR / "overlay" / dataset.seed)


def build(run_dir):
    """Write input/ascii/*.txt for the COBOL run and input/*.jsonl for the Java run."""
    run_dir = Path(run_dir)
    ascii_dir = run_dir / "input" / "ascii"
    ascii_dir.mkdir(parents=True, exist_ok=True)
    counts = {}
    for ds in INPUTS.values():
        layout = ds.layout
        rows = []
        seen = set()
        for line in source_lines(ds):
            text = line.ljust(layout.length)[: layout.length]
            rec = decode_record(text, layout)
            key = record_key(ds, rec)
            if key in seen:
                raise ValueError(f"{ds.name}: duplicate key {key}")
            seen.add(key)
            rows.append((text, rec))
        if ds.name != "dailytran":
            rows.sort(key=lambda r: r[0][: sum(layout.field(k).length for k in ds.key)])
        (ascii_dir / ds.seed).write_text("".join(t + "\n" for t, _ in rows))
        with open(run_dir / "input" / f"{ds.name}.jsonl", "w") as fh:
            for _, rec in rows:
                fh.write(json.dumps(rec, sort_keys=True) + "\n")
        counts[ds.name] = len(rows)
    return counts


def read_jsonl(path):
    path = Path(path)
    if not path.exists():
        return []
    return [json.loads(line) for line in path.read_text().splitlines() if line.strip()]
