"""Fixtures for the acceptance suite.

The same tests run against any implementation's outputs:
    pytest spec/tests --run-dir RUN --impl cobol
    pytest spec/tests --run-dir RUN --impl java
With no --run-dir the suite runs against the frozen COBOL outputs in spec/golden.
"""
import sys
from pathlib import Path

import pytest

SPEC = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(SPEC))

from harness import inputs as harness_inputs  # noqa: E402
from harness.datasets import INPUTS, OUTPUTS, record_key  # noqa: E402


def pytest_addoption(parser):
    parser.addoption("--run-dir", default=None)
    parser.addoption("--impl", default="cobol", choices=["cobol", "java"])


@pytest.fixture(scope="session")
def run_dir(request, tmp_path_factory):
    given = request.config.getoption("--run-dir")
    if given:
        return Path(given)
    path = tmp_path_factory.mktemp("golden")
    harness_inputs.build(path)
    (path / "cobol").mkdir()
    for f in (SPEC / "golden").glob("*.jsonl"):
        (path / "cobol" / f.name).write_text(f.read_text())
    return path


@pytest.fixture(scope="session")
def impl(request):
    return request.config.getoption("--impl")


@pytest.fixture(scope="session")
def outputs(run_dir, impl):
    """outputs['posted']['<key>'] -> record, for the implementation under test."""
    return {name: {record_key(ds, r): r for r in harness_inputs.read_jsonl(run_dir / impl / f"{name}.jsonl")}
            for name, ds in OUTPUTS.items()}


@pytest.fixture(scope="session")
def run_inputs(run_dir):
    return {name: {record_key(ds, r): r for r in harness_inputs.read_jsonl(run_dir / "input" / f"{name}.jsonl")}
            for name, ds in INPUTS.items()}
