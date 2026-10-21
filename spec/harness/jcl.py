"""Read job steps from the JCL members so the program list is discovered, never typed in."""
import re
from functools import lru_cache
from pathlib import Path

import yaml

from . import APP_DIR, SPEC_DIR

EXEC_RE = re.compile(r"^//(\S+)\s+EXEC\s+PGM=([A-Z0-9#@$]+)(?:,PARM='([^']*)')?", re.I)
UTILITIES = {"IDCAMS", "IEFBR14", "SORT", "IEBGENER", "IKJEFT01", "IEBCOPY", "DFSRRC00"}


@lru_cache(maxsize=None)
def manifest():
    return yaml.safe_load((SPEC_DIR / "manifest.yaml").read_text())


def jcl_path(job):
    for entry in manifest()["batch_path"]:
        if entry["job"] == job:
            return APP_DIR / entry["jcl"]
    raise KeyError(f"{job} is not on the batch path in spec/manifest.yaml")


def parse_jcl(path):
    steps = []
    for line in Path(path).read_text(errors="replace").splitlines():
        m = EXEC_RE.match(line)
        if m:
            steps.append({"step": m.group(1), "program": m.group(2).upper(), "parm": m.group(3) or ""})
    return steps


def job_steps(job):
    """Application programs executed by a job, in step order (utility steps left out)."""
    return [s for s in parse_jcl(jcl_path(job)) if s["program"] not in UTILITIES]


def batch_programs(include_optional=False):
    out = []
    for entry in manifest()["batch_path"]:
        if entry.get("optional") and not include_optional:
            continue
        for step in job_steps(entry["job"]):
            out.append({"job": entry["job"], "jcl": entry["jcl"], **step})
    return out
