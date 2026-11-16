"""Per-chain implementation switch for the strangler-fig migration.

Each ``tools/runjcl/impl/<chain>.json`` names the programs of one chain that
have a migrated implementation and the chain's ``default`` (``legacy`` until
the chain is cut over).  ``CARDDEMO_IMPL=<chain>=<impl>[,<chain>=<impl>...]``
overrides the default per run, so a chain is cut over or rolled back on its
own without touching the JCL: the same job runs, the same DDs are allocated,
only the program behind ``EXEC PGM=`` changes.

A migrated program is any executable with the batch step contract:
``DD_<ddname>`` paths, the PARM text as its argument, STDOUT as SYSOUT, the
exit status as the step return code.  KSDS clusters are handed to it as flat
images (unloaded before the step, reloaded afterwards when the program
changed them, alternate indexes included), so it never needs the Berkeley DB
file format.
"""
from __future__ import annotations

import hashlib
import json
import os
import subprocess
from pathlib import Path

IMPL_DIR = Path(__file__).resolve().parent / "impl"


def selected() -> dict[str, str]:
    choice = {}
    for item in filter(None, (os.environ.get("CARDDEMO_IMPL") or "").replace(" ", "").split(",")):
        chain, _, impl = item.partition("=")
        choice[chain] = impl or "legacy"
    return choice


def load(jcl) -> dict[str, dict]:
    """program -> {chain, impl, command} for every chain switched off legacy."""
    choice = selected()
    programs: dict[str, dict] = {}
    for path in sorted(IMPL_DIR.glob("*.json")):
        spec = json.loads(path.read_text())
        chain = spec["chain"]
        impl = choice.get(chain, spec.get("default", "legacy"))
        if impl == "legacy":
            continue
        if impl not in spec.get("implementations", {}):
            raise jcl.JclError(f"chain {chain}: unknown implementation {impl!r} ({path.name})")
        target = spec["implementations"][impl]
        required = target.get("requires")
        if required and not (jcl.ROOT / required).exists():
            raise jcl.JclError(f"chain {chain}={impl}: {required} missing; run {target.get('build', 'its build')}")
        for program, command in target["programs"].items():
            programs[program.upper()] = {"chain": chain, "impl": impl, "command": command}
    return programs


def _digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest() if path.exists() else ""


def run_step(runner, jcl, step, env: dict, infos: dict, stepdir: Path, target: dict) -> int:
    runner.log(f"    {step.program} -> {target['chain']}={target['impl']}: {' '.join(target['command'])}")
    reload = []
    for ddname, info in infos.items():
        dsn = info.get("dsn")
        if info.get("kind") == "path":
            aix = runner.catalog.entry(dsn) or {}
            dsn = (runner.catalog.entry(aix.get("aix") or dsn) or {}).get("relate")
        elif info.get("kind") != "dataset" or not runner.catalog.is_ksds(dsn):
            continue
        if not dsn or not jcl.dataset_path(dsn).exists():
            continue
        entry = runner.catalog.entry(dsn)
        flat = stepdir / f"{ddname}.flat"
        rc = runner._cobcrun([jcl.ksds_module(entry, unload=True)],
                             {"DD_KSDSFILE": str(jcl.dataset_path(dsn)), "DD_FLATFILE": str(flat)}, quiet=True)
        if rc:
            raise jcl.JclError(f"{step.name} {ddname}: unload of {dsn} failed")
        env[f"DD_{ddname}"] = str(flat)
        if info.get("kind") == "dataset" and info.get("disp") in ("OLD", "SHR", "MOD"):
            reload.append((dsn, entry, flat, _digest(flat)))
    argv = [*target["command"], *([step.parm.strip("'")] if step.parm else [])]
    result = subprocess.run(argv, cwd=jcl.ROOT, env=env, capture_output=True, text=True)
    (stepdir / "SYSOUT.txt").write_text(result.stdout + (("\n" + result.stderr) if result.stderr else ""))
    if result.stderr:
        runner.log("    " + result.stderr.strip().replace("\n", "\n    "))
    for dsn, entry, flat, before in reload:
        if _digest(flat) != before:
            rc = runner._cobcrun([jcl.ksds_module(entry)],
                                 {"DD_FLATFILE": str(flat), "DD_KSDSFILE": str(jcl.dataset_path(dsn))}, quiet=True)
            if rc:
                raise jcl.JclError(f"{step.name}: reload of {dsn} failed")
    return result.returncode
