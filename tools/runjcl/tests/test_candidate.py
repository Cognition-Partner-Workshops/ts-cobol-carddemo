import json
import os
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import Mock, patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import candidate
import runjcl


class CandidateTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.impl_dir = self.root / "impl"
        self.impl_dir.mkdir()
        self.jcl = Mock(ROOT=self.root, JclError=runjcl.JclError)
        self.start_patch(patch.object(candidate, "IMPL_DIR", self.impl_dir))
        self.start_patch(patch.dict(os.environ, {"CARDDEMO_IMPL": ""}))

    def start_patch(self, patcher):
        value = patcher.start()
        self.addCleanup(patcher.stop)
        return value

    def spec(self, chain, default="legacy", programs=None, **target):
        target["programs"] = programs or {"SHARED": [chain + "-java"]}
        (self.impl_dir / f"{chain}.json").write_text(json.dumps({
            "chain": chain, "default": default, "implementations": {"java": target},
        }))

    def step(self, name="STEP15", program="SHARED", parm=None):
        return runjcl.Step(name, program, parm, None)

    def test_legacy_default_and_no_chain(self):
        self.spec("a")
        self.assertEqual({}, candidate.load(self.jcl, "a"))
        os.environ["CARDDEMO_IMPL"] = "a=java"
        self.assertEqual({}, candidate.load(self.jcl))
        self.assertEqual({}, candidate.load(self.jcl, "unknown"))

    def test_shared_program_is_isolated_by_chain(self):
        self.spec("a")
        self.spec("b")
        os.environ["CARDDEMO_IMPL"] = "a=java,b=legacy"
        self.assertEqual(["a-java"], candidate.load(self.jcl, "a")["SHARED"]["command"])
        self.assertEqual({}, candidate.load(self.jcl, "b"))
        os.environ["CARDDEMO_IMPL"] = "a=java,b=java"
        self.assertEqual(["b-java"], candidate.load(self.jcl, "b")["SHARED"]["command"])
        self.assertEqual(["a-java"], candidate.load(self.jcl, "a")["SHARED"]["command"])

    def test_chain_default_and_explicit_rollback(self):
        self.spec("a", default="java")
        self.assertIn("SHARED", candidate.load(self.jcl, "a"))
        os.environ["CARDDEMO_IMPL"] = "a=legacy"
        self.assertEqual({}, candidate.load(self.jcl, "a"))

    def test_unrelated_missing_artifact_does_not_block_chain(self):
        self.spec("a")
        self.spec("b", default="java", requires="missing.jar")
        self.assertEqual({}, candidate.load(self.jcl, "a"))
        with self.assertRaisesRegex(runjcl.JclError, "missing.jar missing"):
            candidate.load(self.jcl, "b")

    def test_unknown_selected_implementation_is_error(self):
        self.spec("a")
        os.environ["CARDDEMO_IMPL"] = "a=bogus"
        with self.assertRaisesRegex(runjcl.JclError, "unknown implementation"):
            candidate.load(self.jcl, "a")

    def test_step_override_precedence_and_program_fallback(self):
        self.spec("a", default="java", programs={
            "shared": ["program"], "step15": ["step"], "step15.shared": ["exact"],
        })
        targets = candidate.load(self.jcl, "a")
        self.assertEqual(["exact"], candidate.resolve(targets, self.step())["command"])
        self.assertEqual(["step"], candidate.resolve(targets, self.step(program="OTHER"))["command"])
        self.assertEqual(["program"], candidate.resolve(targets, self.step(name="STEP20"))["command"])
        self.assertIsNone(candidate.resolve(targets, self.step(name="STEP20", program="OTHER")))
        self.assertIsNone(candidate.resolve({}, self.step()))

    def runner(self, targets):
        runner = object.__new__(runjcl.Runner)
        runner.candidates = targets
        runner.log = Mock()
        runner.base_env = Mock(return_value={})
        runner._cobcrun = Mock(return_value=0)
        return runner

    def test_direct_program_dispatch_uses_step_override(self):
        runner = self.runner({"STEP15.SHARED": {"command": ["candidate"]}})
        with patch.object(candidate, "run_step", return_value=4) as run:
            self.assertEqual(4, runner.run_step(self.step(), self.root))
        self.assertEqual(["candidate"], run.call_args.args[-1]["command"])
        runner._cobcrun.assert_not_called()

    def test_direct_legacy_dispatch_is_unchanged(self):
        runner = self.runner({})
        self.assertEqual(0, runner.run_step(self.step(parm="'abc'"), self.root))
        runner._cobcrun.assert_called_once_with(["PARMDRV", "SHARED", "abc"], {}, stepdir=self.root)

    def test_ikjeft01_resolves_inner_program_and_parm(self):
        self.spec("a", default="java", programs={"STEP1.COBTUPDT": ["candidate"]})
        runner = self.runner(candidate.load(self.jcl, "a"))
        systsin = self.root / "SYSTSIN"
        systsin.write_text("DSN SYSTEM(DAZ1)\n RUN PROGRAM(COBTUPDT) PARMS('abc')\n")
        env = {"DD_SYSTSIN": str(systsin)}
        runner.base_env.return_value = env
        with patch.object(candidate, "run_step", return_value=8) as run:
            self.assertEqual(8, runner.run_step(self.step("STEP1", "IKJEFT01"), self.root))
        inner = run.call_args.args[2]
        self.assertEqual(("STEP1", "COBTUPDT", "abc"), (inner.name, inner.program, inner.parm))
        runner._cobcrun.assert_not_called()

    def test_ikjeft01_legacy_program_and_output_are_unchanged(self):
        runner = self.runner({})
        systsin = self.root / "SYSTSIN"
        systsin.write_text("RUN PROGRAM(COBTUPDT) PARMS('abc')\n")
        (self.root / "SYSOUT.txt").write_bytes(b"legacy output\n")
        output = self.root / "SYSTSPRT"
        env = {"DD_SYSTSIN": str(systsin), "DD_SYSTSPRT": str(output)}
        runner._cobcrun.return_value = 4
        self.assertEqual(4, runner.ikjeft01(self.step("STEP1", "IKJEFT01"), env, {}, self.root))
        runner._cobcrun.assert_called_once_with(["DSNRUN", "COBTUPDT", "abc"], env, stepdir=self.root)
        self.assertEqual(b"legacy output\n", output.read_bytes())


if __name__ == "__main__":
    unittest.main()
