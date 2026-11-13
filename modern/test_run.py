"""Runner contract tests: all Java/comparator subprocesses are mocked."""
import contextlib
import io
import json
import subprocess
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import run as runner

ROOT = Path(__file__).resolve().parents[1]


class ContainerRunnerTest(unittest.TestCase):
    def invoke(self, output, chains, case=None, compare=True, results=None):
        with contextlib.redirect_stdout(io.StringIO()), patch("run.subprocess.run") as execute:
            if results is None:
                execute.return_value = subprocess.CompletedProcess([], 0)
            else:
                execute.side_effect = [subprocess.CompletedProcess([], code) for code in results]
            status = runner.run_cases(ROOT, Path("/opt/carddemo"), Path(output), chains, case,
                                      compare, Path(output) / "report.md")
            return status, execute.call_args_list

    def test_every_chain_and_fixture_uses_the_packaged_jar_and_unchanged_comparator(self):
        with tempfile.TemporaryDirectory() as output:
            status, calls = self.invoke(output, list(runner.CHAINS))
            count = sum(sum(path.is_dir() for path in (ROOT / "fixtures" / chain).iterdir())
                        for chain in runner.CHAINS)
            self.assertEqual(0, status)
            self.assertEqual(count * 2, len(calls))
            for java, compare in zip(calls[::2], calls[1::2]):
                command = java.args[0]
                self.assertEqual(["java", "-jar"], command[:2])
                self.assertTrue(command[2].startswith("/opt/carddemo/"))
                self.assertTrue(command[3].endswith("/input"))
                comparator = compare.args[0]
                self.assertEqual(str(ROOT / "tools/parity/compare.py"), comparator[1])
                self.assertEqual(["--label", "Java container"], comparator[-2:])

    def test_parameters_come_from_chains_and_jcl_not_runner_defaults(self):
        config = json.loads((ROOT / "tools/runjcl/chains.json").read_text())
        self.assertEqual([config["trantype-update"]["clock"]], runner.job_parameters(ROOT, "trantype-update"))
        posting = runner.job_parameters(ROOT, "posting")
        self.assertEqual(config["posting"]["clock"], posting[0])
        self.assertEqual(3, len(posting))
        jcl = (ROOT / "app/jcl/TRANREPT.jcl").read_text()
        self.assertIn("PARM-START-DATE,C'" + posting[1] + "'", jcl)
        self.assertIn("PARM-END-DATE,C'" + posting[2] + "'", jcl)

    def test_a_red_comparison_or_failed_java_process_makes_the_runner_fail(self):
        for results in ([0, 1], [1, 0]):
            with self.subTest(results=results), tempfile.TemporaryDirectory() as output:
                status, calls = self.invoke(output, ["posting"], "p07-empty-day", results=results)
                self.assertEqual(1, status)
                self.assertEqual(2, len(calls))

    def test_without_compare_only_java_is_dispatched(self):
        with tempfile.TemporaryDirectory() as output:
            status, calls = self.invoke(output, ["interest"], "i01-baseline", compare=False)
            self.assertEqual(0, status)
            self.assertEqual(1, len(calls))

    def test_unknown_fixture_is_rejected_before_execution(self):
        with tempfile.TemporaryDirectory() as output, self.assertRaisesRegex(ValueError, "no fixture"):
            self.invoke(output, ["posting"], "does-not-exist")

    def test_output_cannot_overwrite_protected_checkout_paths(self):
        with patch("run.subprocess.run") as execute, self.assertRaisesRegex(ValueError, "output must be outside"):
            runner.run_cases(ROOT, Path("/opt/carddemo"), ROOT / "fixtures", ["posting"],
                             "p07-empty-day", False, ROOT / "work/report.md")
        execute.assert_not_called()


if __name__ == "__main__":
    unittest.main()
