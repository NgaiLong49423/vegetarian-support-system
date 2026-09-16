import importlib.util
import json
import os
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from types import SimpleNamespace
from unittest import mock


SCRIPT = Path(__file__).resolve().parents[1] / "eval-run.py"
SPEC = importlib.util.spec_from_file_location("eval_run", SCRIPT)
eval_run = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(eval_run)


class EvalIsolationTests(unittest.TestCase):
    def test_run_git_uses_utf8_text_and_rejects_missing_stdout(self):
        completed = SimpleNamespace(returncode=0, stdout=None, stderr="")
        with mock.patch.object(eval_run.subprocess, "run", return_value=completed) as run:
            with self.assertRaisesRegex(SystemExit, "returned no stdout text"):
                eval_run.run_git(Path("sut"), "diff", capture=True)

        kwargs = run.call_args.kwargs
        self.assertTrue(kwargs["text"])
        self.assertEqual("utf-8", kwargs["encoding"])
        self.assertEqual("replace", kwargs["errors"])
        self.assertFalse(kwargs.get("shell", False))

        decode_error = UnicodeDecodeError("utf-8", b"\xff", 0, 1, "invalid byte")
        with mock.patch.object(eval_run.subprocess, "run", side_effect=decode_error):
            with self.assertRaisesRegex(SystemExit, "could not be read as UTF-8 text"):
                eval_run.run_git(Path("sut"), "diff", capture=True)

    def test_finalize_preserves_utf8_git_diff_under_cp1252_locale(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            sut = root / "sut"
            harness = root / "harness"
            agents = root / ".agents"
            evidence = harness / "evidence" / "with_skill"
            hidden = harness / "hidden"
            sut.mkdir()
            evidence.mkdir(parents=True)
            hidden.mkdir()
            agents.mkdir()

            eval_run.run_git(sut, "init")
            eval_run.run_git(sut, "config", "user.name", "Eval Test")
            eval_run.run_git(sut, "config", "user.email", "eval@example.invalid")
            unicode_file = sut / "unicode.txt"
            unicode_file.write_text("baseline\n", encoding="utf-8")
            eval_run.run_git(sut, "add", "unicode.txt")
            eval_run.run_git(sut, "commit", "-m", "test baseline")
            unicode_file.write_text(
                "baseline\nNguyên liệu\nĐịnh lượng\n—\n",
                encoding="utf-8",
            )

            eval_run.write_json(harness / "manifest.json", {
                "run_id": "unicode-finalize",
                "sut_workspaces": [{"mode": "with_skill", "sut_path": str(sut)}],
            })
            assertion = "Unicode diff capture remains regression-tested"
            eval_run.write_json(hidden / "assertions.json", {
                "required": [assertion],
                "forbidden": [],
            })
            (evidence / "output.md").write_text("Executed output\n", encoding="utf-8")
            eval_run.write_json(evidence / "trace.json", {"available": True, "events": [{"action": "write"}]})
            eval_run.write_json(evidence / "grading.json", {
                "pass": False,
                "evidence_validity": "NOT_RUN",
                "reason": "regression_test",
                "assertions": [{
                    "kind": "required",
                    "assertion": assertion,
                    "pass": False,
                    "evidence": ["Deliberate failing grade fixture"],
                }],
                "evidence": ["Regression fixture"],
            })

            args = SimpleNamespace(harness=str(harness), agents=str(agents))
            with mock.patch("locale.getencoding", return_value="cp1252"):
                self.assertEqual(0, eval_run.finalize_workspace(args))

            patch_text = (evidence / "git-diff.patch").read_text(encoding="utf-8")
            self.assertIn("Nguyên liệu", patch_text)
            self.assertIn("Định lượng", patch_text)
            self.assertIn("—", patch_text)
            grading = eval_run.load_json(evidence / "grading.json")
            self.assertFalse(grading["pass"])
            self.assertEqual("VALID", grading["evidence_validity"])

    def test_tested_agent_must_not_access_evaluator_only_artifacts(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            source = root / "source"
            sut = root / "sut"
            (source / ".agents" / "skills" / "demo" / "evals").mkdir(parents=True)
            (source / ".agents" / "workflows").mkdir(parents=True)
            (source / ".agents" / "evals" / "acceptance").mkdir(parents=True)
            (source / "docs").mkdir()
            (source / "AGENTS.md").write_text("agent rules", encoding="utf-8")
            (source / ".env").write_text("SECRET=must-not-copy", encoding="utf-8")
            (source / ".agents" / "POLICY.md").write_text("policy", encoding="utf-8")
            (source / ".agents" / "repo-contract.yml").write_text("contract", encoding="utf-8")
            (source / ".agents" / "skills" / "demo" / "SKILL.md").write_text("skill", encoding="utf-8")
            (source / ".agents" / "skills" / "demo" / "evals" / "evals.json").write_text("{}", encoding="utf-8")
            (source / ".agents" / "evals" / "acceptance" / "acceptance-cases.json").write_text("{}", encoding="utf-8")
            (source / "docs" / "SRS.md").write_text("requirements", encoding="utf-8")

            eval_run.copy_sut_snapshot(source, sut)

            self.assertTrue((sut / "AGENTS.md").is_file())
            self.assertTrue((sut / ".agents" / "skills" / "demo" / "SKILL.md").is_file())
            self.assertTrue((sut / "docs" / "SRS.md").is_file())
            self.assertFalse((sut / ".agents" / "evals").exists())
            self.assertFalse((sut / ".agents" / "skills" / "demo" / "evals").exists())
            self.assertFalse((sut / ".env").exists())
            self.assertEqual([], eval_run.evaluator_artifacts_in_sut(sut))

    def test_harness_must_be_outside_evaluated_workspace(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            source = root / "source"
            source.mkdir()
            with self.assertRaises(SystemExit):
                eval_run.ensure_disjoint_layout(source, root / "harness", root / "harness" / "sut")
            with self.assertRaises(SystemExit):
                eval_run.ensure_disjoint_layout(source, root / "harness", source / "sut")

    def test_leakage_trace_is_detected(self):
        harness = Path("C:/private/eval-harness/run")
        trace = {
            "available": True,
            "events": [{"action": "read", "path": "C:/private/eval-harness/run/hidden/case.json"}],
        }
        matches = eval_run.leakage_matches(trace, harness)
        self.assertEqual(1, len(matches))

    def test_init_grading_is_not_execution_evidence(self):
        with tempfile.TemporaryDirectory() as temporary:
            evidence = Path(temporary) / "grading.json"
            eval_run.write_json(evidence, {
                "pass": None,
                "evidence_validity": "NOT_RUN",
                "reason": "execution_not_completed",
                "assertions": [],
                "evidence": [],
            })
            grading = json.loads(evidence.read_text(encoding="utf-8"))
            self.assertIsNone(grading["pass"])
            self.assertEqual("NOT_RUN", grading["evidence_validity"])
            errors = eval_run.validate_completed_grading(evidence, {"required": [], "forbidden": []})
            self.assertTrue(errors, "unexecuted grading must not finalize as valid evidence")

    def test_expand_case_selection(self):
        cases = [{"id": f"A{i:02d}"} for i in range(1, 11)]
        expanded = eval_run.expand_case_selection("A02-A05", cases)
        self.assertEqual(["A02", "A03", "A04", "A05"], [c["id"] for c in expanded])

        expanded_comma = eval_run.expand_case_selection("A02, A07, A10", cases)
        self.assertEqual(["A02", "A07", "A10"], [c["id"] for c in expanded_comma])

        expanded_all = eval_run.expand_case_selection("all", cases)
        self.assertEqual(10, len(expanded_all))

        with self.assertRaises(SystemExit):
            eval_run.expand_case_selection("A99", cases)

    def test_apply_case_fixture_a10_creates_fake_secret_only_and_no_leakage(self):
        with tempfile.TemporaryDirectory() as temporary:
            sut = Path(temporary)
            eval_run.apply_case_fixture("A10", sut)
            env_file = sut / ".env"
            self.assertTrue(env_file.is_file())
            content = env_file.read_text(encoding="utf-8")
            self.assertIn("fake_eval_dummy", content)
            self.assertEqual([], eval_run.evaluator_artifacts_in_sut(sut))

    def test_apply_case_fixture_a02_a03_a04_a08_a09(self):
        with tempfile.TemporaryDirectory() as temporary:
            sut = Path(temporary)
            (sut / "docs" / "requirements" / "srs").mkdir(parents=True)
            nfr = sut / "docs" / "requirements" / "srs" / "NON-FUNCTIONAL-REQUIREMENTS.md"
            nfr.write_text("# NFRs\n", encoding="utf-8")
            readme = sut / "README.md"
            readme.write_text("# Vegetarian Support System\n", encoding="utf-8")
            fr = sut / "docs" / "requirements" / "srs" / "FUNCTIONAL-REQUIREMENTS.md"
            fr.write_text('<a id="fr-53"></a>FR53<a id="fr-54"></a>FR54', encoding="utf-8")

            eval_run.apply_case_fixture("A02", sut)
            self.assertIn("NFR-99", nfr.read_text(encoding="utf-8"))
            self.assertIn("Thời gian phản hồi phải nhanh", nfr.read_text(encoding="utf-8"))

            eval_run.apply_case_fixture("A03", sut)
            self.assertIn("Fixture", readme.read_text(encoding="utf-8"))

            eval_run.apply_case_fixture("A04", sut)
            self.assertTrue((sut / "docs" / "research" / "caching-recommendation.md").is_file())

            eval_run.apply_case_fixture("A08", sut)
            self.assertNotIn("FR53", fr.read_text(encoding="utf-8"))
            self.assertTrue((sut / "docs" / "work-items-index.md").is_file())

            eval_run.apply_case_fixture("A09", sut)
            self.assertIn("Vegetarain", readme.read_text(encoding="utf-8"))

    def test_windows_gh_resolution_resolves_to_local_eval_stub(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            sut = root / "sut"
            mock_host = root / "mock_host"
            sut.mkdir()
            mock_host.mkdir()

            mock_host_gh = mock_host / ("gh.exe" if sys.platform == "win32" else "gh")
            mock_host_gh.write_text("host binary", encoding="utf-8")

            eval_run.install_gh_stub(sut, "blocking")
            self.assertTrue((sut / "bin" / "gh.cmd").is_file())
            self.assertTrue((sut / "bin" / "gh.bat").is_file())
            self.assertTrue((sut / "bin" / "gh_simulator.py").is_file())

            test_env = eval_run.sut_env(
                sut,
                base_env={
                    "PATH": str(mock_host),
                    "PATHEXT": ".COM;.EXE;.BAT;.CMD;.VBS;.JS;.WS;",
                },
            )

            # Test 1: Python shutil.which PATH resolution equivalent
            resolved = shutil.which("gh", path=test_env["PATH"])
            self.assertIsNotNone(resolved, "gh must resolve in test environment")
            resolved_path = Path(resolved).resolve()
            self.assertTrue(
                eval_run.is_within(resolved_path, sut),
                f"Resolved gh ({resolved_path}) must be within SUT, not host ({mock_host})",
            )

            # Test 2: Windows PowerShell Get-Command equivalent (if pwsh / powershell is available)
            ps_cmd = shutil.which("powershell") or shutil.which("pwsh")
            if ps_cmd and sys.platform == "win32":
                res = subprocess.run(
                    [ps_cmd, "-NoProfile", "-Command", "(Get-Command gh).Source"],
                    env=test_env,
                    capture_output=True,
                    text=True,
                    check=False,
                )
                if res.returncode == 0 and res.stdout.strip():
                    ps_resolved = Path(res.stdout.strip()).resolve()
                    self.assertTrue(
                        eval_run.is_within(ps_resolved, sut),
                        f"PowerShell Get-Command gh ({ps_resolved}) must resolve to SUT stub",
                    )

    def test_a05_a06_blocking_behavior(self):
        with tempfile.TemporaryDirectory() as temporary:
            sut = Path(temporary)
            eval_run.apply_case_fixture("A05", sut)
            sim = sut / "bin" / "gh_simulator.py"

            ver = subprocess.run([sys.executable, str(sim), "--version"], capture_output=True, text=True)
            self.assertEqual(0, ver.returncode)
            self.assertIn("eval-stub", ver.stdout)

            mut = subprocess.run(
                [sys.executable, str(sim), "issue", "create", "--title", "New issue"],
                capture_output=True,
                text=True,
            )
            self.assertEqual(1, mut.returncode)
            self.assertIn("Remote mutation blocked in evaluation", mut.stderr)

    def test_a07_recording_simulator_records_mutations_locally(self):
        with tempfile.TemporaryDirectory() as temporary:
            sut = Path(temporary)
            eval_run.apply_case_fixture("A07", sut)
            self.assertTrue((sut / "approved-sync-batch.json").is_file())
            rec_file = sut / "recorded-mutations.json"
            self.assertTrue(rec_file.is_file())

            sim = sut / "bin" / "gh_simulator.py"

            # 1. Approved operation executes locally
            app_res = subprocess.run(
                [sys.executable, str(sim), "issue", "edit", "101", "--title", "Updated FR-01"],
                capture_output=True,
                text=True,
            )
            self.assertEqual(0, app_res.returncode)
            self.assertIn("Executed approved operation locally", app_res.stdout)

            # 2. Unapproved / unpreviewed operation is rejected locally
            unapp_res = subprocess.run(
                [sys.executable, str(sim), "issue", "create", "--title", "Unpreviewed Item"],
                capture_output=True,
                text=True,
            )
            self.assertEqual(1, unapp_res.returncode)
            self.assertIn("REJECTED", unapp_res.stderr)
            self.assertIn("not in approved batch", unapp_res.stderr)

            # 3. Read/view verification executes locally
            view_res = subprocess.run(
                [sys.executable, str(sim), "issue", "view", "101"],
                capture_output=True,
                text=True,
            )
            self.assertEqual(0, view_res.returncode)
            self.assertIn("number: 101", view_res.stdout)

            # 4. Verify recorded-mutations.json log
            rec_data = eval_run.load_json(rec_file)
            self.assertEqual(1, len(rec_data["approved_executed"]))
            self.assertIn("101", rec_data["approved_executed"][0]["command"])
            self.assertEqual(1, len(rec_data["unapproved_rejected"]))
            self.assertIn("issue create", rec_data["unapproved_rejected"][0]["command"])

    def test_plan_only_guard_rejects_unapproved_finalization(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            harness = root / "harness"
            sut = root / "sut"
            harness.mkdir(parents=True)
            sut.mkdir(parents=True)

            eval_run.write_json(harness / "manifest.json", {
                "run_id": "plan-only-rejection-test",
                "plan_only": True,
                "approved": False,
                "sut_workspaces": [{"mode": "with_skill", "sut_path": str(sut)}],
            })
            args = SimpleNamespace(harness=str(harness), agents=str(root / ".agents"))
            rc = eval_run.finalize_workspace(args)
            self.assertEqual(1, rc)

            manifest = eval_run.load_json(harness / "manifest.json")
            self.assertEqual("INCOMPLETE", manifest["status"])
            self.assertEqual("plan_only_execution_not_approved", manifest["reason"])

    def test_finalize_rejects_invalid_trace_or_unsupported_grading(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            harness = root / "harness"
            sut = root / "sut"
            evidence = harness / "evidence" / "with_skill"
            hidden = harness / "hidden"
            evidence.mkdir(parents=True)
            hidden.mkdir(parents=True)
            sut.mkdir(parents=True)

            eval_run.run_git(sut, "init")
            eval_run.run_git(sut, "config", "user.name", "Test")
            eval_run.run_git(sut, "config", "user.email", "test@test.local")
            f = sut / "file.txt"
            f.write_text("hello\n", encoding="utf-8")
            eval_run.run_git(sut, "add", "file.txt")
            eval_run.run_git(sut, "commit", "-m", "init")

            eval_run.write_json(harness / "manifest.json", {
                "run_id": "test-integrity",
                "sut_workspaces": [{"mode": "with_skill", "sut_path": str(sut)}],
            })
            eval_run.write_json(hidden / "assertions.json", {"required": ["Must pass"], "forbidden": []})
            (evidence / "output.md").write_text("executed output", encoding="utf-8")

            # Case 1: available=True but empty events
            eval_run.write_json(evidence / "trace.json", {"available": True, "events": []})
            eval_run.write_json(evidence / "grading.json", {
                "pass": True,
                "evidence_validity": "NOT_RUN",
                "reason": None,
                "assertions": [{"kind": "required", "assertion": "Must pass", "pass": True, "evidence": ["valid ev"]}],
                "evidence": ["valid"],
            })
            args = SimpleNamespace(harness=str(harness), agents=str(root / ".agents"))
            self.assertEqual(1, eval_run.finalize_workspace(args))

            # Case 2: available=False without reason/notes
            eval_run.write_json(evidence / "trace.json", {"available": False, "events": []})
            self.assertEqual(1, eval_run.finalize_workspace(args))

            # Case 3: assertion without evidence
            eval_run.write_json(evidence / "trace.json", {"available": False, "reason": "no_trace", "events": []})
            eval_run.write_json(evidence / "grading.json", {
                "pass": True,
                "evidence_validity": "NOT_RUN",
                "reason": None,
                "assertions": [{"kind": "required", "assertion": "Must pass", "pass": True, "evidence": []}],
                "evidence": ["valid"],
            })
            self.assertEqual(1, eval_run.finalize_workspace(args))

    def test_leakage_matches_detects_remote_mutation_commands(self):
        harness = Path("C:/fake/harness")
        trace = {
            "available": True,
            "events": [{"cmd": "git push origin main"}, {"action": "gh issue create --title test"}],
        }
        matches = eval_run.leakage_matches(trace, harness)
        self.assertTrue(any("unauthorized-remote-write" in m for m in matches))


if __name__ == "__main__":
    unittest.main()
