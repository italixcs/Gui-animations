"""Consumer-level installer regression tests; no network or external packages."""

from __future__ import annotations

import importlib.util
import json
import os
from pathlib import Path
import stat
import subprocess
import sys
import tempfile
import unittest
from unittest import mock


ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location("minecraft_gui_install", ROOT / "scripts/install.py")
installer = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = installer
SPEC.loader.exec_module(installer)


def write_package(path: Path, files: dict | None = None, version: str = "0.1.0") -> Path:
    files = files or {
        "SKILL.md": (b"---\nname: minecraft-gui\ndescription: Test fixture\n---\n# Skill\n", 0o644),
        "references/design.md": (b"# Design\n", 0o644),
        "templates/starter/gradlew": (b"#!/bin/sh\nexit 0\n", 0o755),
        "templates/starter/build.gradle": (b"plugins { id 'java' }\n", 0o644),
    }
    path.mkdir(parents=True)
    records = {}
    for relative, (content, mode) in files.items():
        target = path / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(content)
        target.chmod(mode)
        records[relative] = installer.file_record(content, mode)
    manifest = {
        "schema_version": 1, "name": "minecraft-gui", "version": version,
        "source": {"repository": "https://github.com/italixcs/Gui-animations", "revision": None, "payload_sha256": installer.payload_digest(records)},
        "files": records,
    }
    (path / "manifest.json").write_bytes(installer.json_bytes(manifest))
    return path


def snapshot(root: Path) -> dict:
    if not root.exists():
        return {}
    return {path.relative_to(root).as_posix(): (path.read_bytes(), stat.S_IMODE(path.stat().st_mode)) for path in root.rglob("*") if path.is_file()}


class InstallTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.base = Path(self.temp.name)
        self.source = write_package(self.base / "package")
        self.project = self.base / "consumer"
        self.project.mkdir()

    def tearDown(self):
        self.temp.cleanup()

    def install(self, **kwargs):
        return installer.install(self.source, self.project, "codex", **kwargs)

    def test_preserves_rules_and_is_byte_idempotent(self):
        rules = self.project / "AGENTS.md"
        original = "# Project\nKeep real settings.\n日本語".encode()
        rules.write_bytes(original)
        result = self.install()
        self.assertTrue(result["changes"])
        self.assertTrue(rules.read_bytes().startswith(original))
        self.assertEqual(rules.read_text().count(installer.BEGIN), 1)
        before = snapshot(self.project)
        self.assertEqual(self.install()["changes"], [])
        self.assertEqual(snapshot(self.project), before)
        lock = json.loads((self.project / installer.LOCK_NAME).read_text())
        self.assertEqual(lock["installations"]["codex"]["version"], "0.1.0")

    def test_dry_run_never_creates_project_or_files(self):
        missing = self.base / "new-project"
        result = installer.install(self.source, missing, "codex", dry_run=True, starter=True)
        self.assertTrue(result["changes"])
        self.assertFalse(missing.exists())
        self.assertEqual(self.install(dry_run=True)["dry_run"], True)
        self.assertEqual(list(self.project.iterdir()), [])

    def test_normal_install_does_not_touch_build_or_create_source(self):
        build = self.project / "build.gradle"
        build.write_text("// Existing project\n")
        self.install()
        self.assertEqual(build.read_text(), "// Existing project\n")
        self.assertFalse((self.project / "gradlew").exists())

    def test_starter_creates_project_and_keeps_executable_wrapper(self):
        target = self.base / "new-project"
        installer.install(self.source, target, "codex", starter=True)
        self.assertTrue((target / "build.gradle").is_file())
        self.assertEqual(stat.S_IMODE((target / "gradlew").stat().st_mode), 0o755)
        lock = json.loads((target / installer.LOCK_NAME).read_text())
        self.assertIn("gradlew", lock["installations"]["codex"]["imports"])
        self.assertEqual(installer.install(self.source, target, "codex", starter=True)["changes"], [])

    def test_missing_project_requires_starter(self):
        with self.assertRaisesRegex(installer.InstallError, "does not exist"):
            installer.install(self.source, self.base / "missing", "codex")

    def test_installs_for_both_agents_without_losing_first_record(self):
        self.install()
        installer.install(self.source, self.project, "claude")
        lock = json.loads((self.project / installer.LOCK_NAME).read_text())
        self.assertEqual(set(lock["installations"]), {"codex", "claude"})
        self.assertTrue((self.project / ".claude/skills/minecraft-gui/SKILL.md").is_file())
        self.assertTrue((self.project / "CLAUDE.md").is_file())
        self.assertEqual(self.install()["changes"], [])

    def test_existing_unmanaged_file_conflict_is_atomic(self):
        path = self.project / ".agents/skills/minecraft-gui/SKILL.md"
        path.parent.mkdir(parents=True)
        path.write_text("Local skill")
        before = snapshot(self.project)
        with self.assertRaisesRegex(installer.InstallError, "conflict"):
            self.install()
        self.assertEqual(snapshot(self.project), before)

    def test_local_skill_edit_blocks_upgrade_without_partial_writes(self):
        self.install()
        local = self.project / ".agents/skills/minecraft-gui/references/design.md"
        local.write_text("Keep this local design")
        before = snapshot(self.project)
        with self.assertRaisesRegex(installer.InstallError, "conflict"):
            self.install()
        self.assertEqual(snapshot(self.project), before)

    def test_local_starter_edits_survive_normal_skill_upgrade(self):
        self.install(starter=True)
        build = self.project / "build.gradle"
        build.write_text("// Customized project\n")
        self.install()
        self.assertEqual(build.read_text(), "// Customized project\n")
        before = snapshot(self.project)
        with self.assertRaisesRegex(installer.InstallError, "conflict"):
            self.install(starter=True)
        self.assertEqual(snapshot(self.project), before)

    def test_import_origin_is_preserved_when_only_skill_version_changes(self):
        self.install(starter=True)
        old_lock = json.loads((self.project / installer.LOCK_NAME).read_text())
        upgraded = write_package(self.base / "upgrade", version="0.2.0")
        installer.install(upgraded, self.project, "codex")
        installation = json.loads((self.project / installer.LOCK_NAME).read_text())["installations"]["codex"]
        self.assertEqual(installation["version"], "0.2.0")
        self.assertEqual(installation["imports_source"], old_lock["installations"]["codex"]["imports_source"])
        self.assertEqual(installation["imports_source"]["version"], "0.1.0")

    def test_rules_outside_managed_block_survive_reinstallation(self):
        self.install()
        rules = self.project / "AGENTS.md"
        rules.write_bytes(b"Existing rules\n\n" + rules.read_bytes() + b"\nMore rules\n")
        before = rules.read_bytes()
        self.install()
        self.assertEqual(rules.read_bytes(), before)

    def test_rules_block_edits_and_deletion_are_protected(self):
        self.install()
        rules = self.project / "AGENTS.md"
        original = rules.read_text()
        rules.write_text(original.replace("shared theme", "local theme"))
        before = snapshot(self.project)
        with self.assertRaisesRegex(installer.InstallError, "Local edits"):
            self.install()
        self.assertEqual(snapshot(self.project), before)
        rules.write_text("Rules without the toolkit block\n")
        with self.assertRaisesRegex(installer.InstallError, "was removed"):
            self.install()

    def test_deleted_managed_file_is_not_silently_restored(self):
        self.install()
        (self.project / ".agents/skills/minecraft-gui/references/design.md").unlink()
        with self.assertRaisesRegex(installer.InstallError, "local deletion"):
            self.install()

    def test_clean_upgrade_replaces_and_removes_owned_files(self):
        self.install()
        upgraded = write_package(self.base / "upgrade", {
            "SKILL.md": (b"# Updated skill\n", 0o644),
            "references/new.md": (b"# New reference\n", 0o644),
        }, "0.2.0")
        installer.install(upgraded, self.project, "codex")
        self.assertEqual((self.project / ".agents/skills/minecraft-gui/SKILL.md").read_bytes(), b"# Updated skill\n")
        self.assertFalse((self.project / ".agents/skills/minecraft-gui/references/design.md").exists())
        self.assertEqual(installer.install(upgraded, self.project, "codex")["changes"], [])

    def test_local_mode_edit_protects_removed_file(self):
        self.install()
        changed = self.project / ".agents/skills/minecraft-gui/references/design.md"
        changed.chmod(0o600)
        upgraded = write_package(self.base / "upgrade", {"SKILL.md": (b"# Skill", 0o644)}, "0.2.0")
        before = snapshot(self.project)
        with self.assertRaisesRegex(installer.InstallError, "mode conflict"):
            installer.install(upgraded, self.project, "codex")
        self.assertEqual(snapshot(self.project), before)

    def test_tampered_package_and_extra_files_are_rejected(self):
        (self.source / "SKILL.md").write_text("Tampered")
        with self.assertRaisesRegex(installer.InstallError, "integrity"):
            self.install()
        (self.source / "extra").write_text("Unexpected")
        with self.assertRaisesRegex(installer.InstallError, "file list"):
            self.install()
        self.assertEqual(list(self.project.iterdir()), [])

    def test_unsafe_package_path_is_rejected(self):
        manifest_path = self.source / "manifest.json"
        manifest = json.loads(manifest_path.read_text())
        manifest["files"]["../escape"] = installer.file_record(b"escape", 0o644)
        manifest_path.write_bytes(installer.json_bytes(manifest))
        with self.assertRaisesRegex(installer.InstallError, "Unsafe relative"):
            self.install()
        self.assertFalse((self.base / "escape").exists())

    def test_reserved_import_in_corrupt_lock_cannot_delete_git_config(self):
        self.install(starter=True)
        config = self.project / ".git/config"
        config.parent.mkdir()
        config.write_text("Important git settings\n")
        lock_path = self.project / installer.LOCK_NAME
        lock = json.loads(lock_path.read_text())
        lock["installations"]["codex"]["imports"][".git/config"] = installer.file_record(config.read_bytes(), 0o644)
        lock_path.write_bytes(installer.json_bytes(lock))
        before = snapshot(self.project)
        with self.assertRaisesRegex(installer.InstallError, "reserved imported path"):
            self.install(starter=True)
        self.assertEqual(snapshot(self.project), before)

    def test_symlinked_destination_does_not_write_outside_project(self):
        outside = self.base / "outside"
        outside.mkdir()
        (self.project / ".agents").symlink_to(outside, target_is_directory=True)
        with self.assertRaisesRegex(installer.InstallError, "symlink"):
            self.install()
        self.assertEqual(list(outside.iterdir()), [])

    def test_symlinked_package_is_rejected(self):
        link = self.base / "package-link"
        link.symlink_to(self.source, target_is_directory=True)
        with self.assertRaisesRegex(installer.InstallError, "symlink"):
            installer.install(link, self.project, "codex")

    def test_unsupported_agent_has_clear_error(self):
        with self.assertRaisesRegex(installer.InstallError, "Unsupported agent"):
            installer.install(self.source, self.project, "unknown")

    def test_failed_write_rolls_back_previously_applied_changes(self):
        (self.project / "AGENTS.md").write_text("# Keep original rules\n")
        before = snapshot(self.project)
        original = installer.atomic_write
        calls = 0

        def failing_write(path, content, mode):
            nonlocal calls
            calls += 1
            if calls == 3:
                raise OSError("simulated disk failure")
            return original(path, content, mode)

        with mock.patch.object(installer, "atomic_write", side_effect=failing_write):
            with self.assertRaisesRegex(installer.InstallError, "previous files restored"):
                self.install()
        self.assertEqual(snapshot(self.project), before)

    def test_cli_json_and_failure_exit_codes(self):
        command = [sys.executable, str(ROOT / "scripts/install.py"), "--source", str(self.source), "--project", str(self.project), "--json", "--dry-run"]
        result = subprocess.run(command, text=True, capture_output=True)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertTrue(json.loads(result.stdout)["dry_run"])
        failed = subprocess.run(command + ["--agent", "bad-agent"], text=True, capture_output=True)
        self.assertEqual(failed.returncode, 2)


if __name__ == "__main__":
    unittest.main()
