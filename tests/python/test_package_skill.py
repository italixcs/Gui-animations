"""Package construction tests using a deliberately tiny source fixture."""

from __future__ import annotations

import importlib.util
import json
from pathlib import Path
import stat
import sys
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location("minecraft_gui_package", ROOT / "scripts/package_skill.py")
builder = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = builder
SPEC.loader.exec_module(builder)


class PackageTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name) / "source"
        self.root.mkdir()
        seed = self.root / ".agents/skills/minecraft-gui"
        for relative in builder.SEED_FILES:
            self.write(seed / relative, f"# {relative}\n".encode())
        for relative in builder.ROOT_STARTER_FILES:
            self.write(self.root / relative, f"fixture: {relative}\n".encode())
        (self.root / "gradlew").chmod(0o755)
        self.write(self.root / "scripts/install.py", b"# fixture installer\n")
        self.write(self.root / "design/tokens.json", b'{}\n')
        self.write(self.root / "design/tokens.schema.json", b'{}\n')
        self.write(self.root / "design/fonts/Inter-Regular.ttf", b"fixture font")
        self.write(self.root / "design/fonts/OFL.txt", b"fixture font license")
        self.write(self.root / "design/component-specs/README.md", b"# Components\n")
        for module in builder.MODULES:
            self.write(self.root / module / "build.gradle", b"// fixture module\n")
            self.write(self.root / module / "src/main/java/Example.java", b"class Example {}\n")
        self.output = Path(self.temp.name) / "package"

    def tearDown(self):
        self.temp.cleanup()

    @staticmethod
    def write(path, content):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(content)

    def test_package_is_deterministic_and_check_detects_stale_inputs(self):
        builder.build(self.root, self.output)
        first = {path.relative_to(self.output): path.read_bytes() for path in self.output.rglob("*") if path.is_file()}
        builder.build(self.root, self.output, check=True)
        builder.build(self.root, self.output)
        second = {path.relative_to(self.output): path.read_bytes() for path in self.output.rglob("*") if path.is_file()}
        self.assertEqual(first, second)
        self.write(self.root / "design/tokens.json", b'{"changed": true}\n')
        with self.assertRaisesRegex(builder.PackageError, "stale"):
            builder.build(self.root, self.output, check=True)

    def test_only_source_and_approved_assets_are_packaged(self):
        self.write(self.root / "examples/showcase-mod/build/logs/secret.log", b"do not include")
        self.write(self.root / "examples/showcase-mod/run/config/private.json", b"do not include")
        self.write(self.root / "examples/showcase-mod/.env", b"do not include")
        self.write(self.root / "design/.env", b"do not include")
        self.write(self.root / "design/tests/test.py", b"do not include")
        builder.build(self.root, self.output)
        files = [path.relative_to(self.output).as_posix() for path in self.output.rglob("*") if path.is_file()]
        self.assertFalse(any("secret" in path or "private" in path or ".env" in path or "test.py" in path for path in files))
        self.assertEqual(len([path for path in files if path.endswith("Inter-Regular.ttf")]), 1)
        self.assertEqual(stat.S_IMODE((self.output / "templates/starter/gradlew").stat().st_mode), 0o755)

    def test_manifest_payload_hash_and_file_hashes_identify_snapshot(self):
        builder.build(self.root, self.output, source_revision="a" * 40)
        manifest = json.loads((self.output / "manifest.json").read_text())
        self.assertEqual(manifest["source"]["revision"], "a" * 40)
        self.assertEqual(manifest["source"]["payload_sha256"], builder.digest(builder.json_bytes(manifest["files"])))
        self.assertNotIn("manifest.json", manifest["files"])
        for relative, record in manifest["files"].items():
            self.assertEqual(builder.digest((self.output / relative).read_bytes()), record["sha256"])

    def test_missing_canonical_input_is_a_clear_error(self):
        (self.root / "gradle/wrapper/gradle-wrapper.jar").unlink()
        with self.assertRaisesRegex(builder.PackageError, "Required package input"):
            builder.build(self.root, self.output)
        self.assertFalse(self.output.exists())

    def test_nested_source_symlink_is_rejected(self):
        outside = Path(self.temp.name) / "outside.java"
        outside.write_text("class Outside {}")
        (self.root / "reference/core/src/main/java/Outside.java").symlink_to(outside)
        with self.assertRaisesRegex(builder.PackageError, "symlink"):
            builder.build(self.root, self.output)

    def test_build_can_replace_in_place_seed_without_losing_workflows(self):
        output = self.root / ".agents/skills/minecraft-gui"
        builder.build(self.root, output)
        builder.build(self.root, output, check=True)
        self.assertTrue((output / "references/review.md").is_file())
        self.assertFalse(output.with_name("minecraft-gui.package-backup").exists())


if __name__ == "__main__":
    unittest.main()
