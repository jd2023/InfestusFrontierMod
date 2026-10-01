import os
from pathlib import Path
import tempfile
import unittest
from gradle.integration.supervisor import disposable_runs


class DisposableRunsTest(unittest.TestCase):
    def test_new_runs_directory_lives_in_memory_and_survives_a_lost_target(self):
        with tempfile.TemporaryDirectory() as raw:
            base, memory = Path(raw) / "base", Path(raw) / "memory"
            base.mkdir()
            memory.mkdir()
            runs = disposable_runs(base, memory)
            self.assertTrue(runs.is_symlink())
            target = Path(os.readlink(runs))
            self.assertEqual(memory, target.parent)
            target.rmdir()  # A reboot clears memory but leaves the link.
            self.assertTrue(disposable_runs(base, memory).is_dir())

    def test_disk_directory_migrates_with_launcher_folders_but_not_with_an_owned_run(self):
        with tempfile.TemporaryDirectory() as raw:
            base, memory = Path(raw) / "base", Path(raw) / "memory"
            (base / "runs/client").mkdir(parents=True)
            memory.mkdir()
            runs = disposable_runs(base, memory)
            self.assertTrue(runs.is_symlink())
            self.assertTrue((runs / "client").is_dir())
        with tempfile.TemporaryDirectory() as raw:
            base, memory = Path(raw) / "base", Path(raw) / "memory"
            (base / "runs/old-run").mkdir(parents=True)
            (base / "runs/old-run/owner.json").write_text("{}")
            memory.mkdir()
            runs = disposable_runs(base, memory)
            self.assertFalse(runs.is_symlink())
            self.assertTrue((runs / "old-run/owner.json").is_file())

    def test_falls_back_to_disk_without_writable_memory(self):
        with tempfile.TemporaryDirectory() as raw:
            base = Path(raw) / "base"
            base.mkdir()
            runs = disposable_runs(base, Path(raw) / "absent")
            self.assertTrue(runs.is_dir() and not runs.is_symlink())

    def test_a_reaped_run_leaves_no_world_behind(self):
        from gradle.integration.test_harness import harness
        with tempfile.TemporaryDirectory() as raw:
            output = Path(raw)
            self.assertEqual(0, harness.run_fixture_scenario("positive", output))
            self.assertEqual([], list((output / "build/integration/runs").iterdir()))


if __name__ == "__main__":
    unittest.main()
