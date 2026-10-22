#!/usr/bin/env python3
"""Exercise the installed toolkit from a separate consuming project.

This is intentionally an external-process check: it imports neither installer
implementation nor test helpers. Pass --build-starter to compile the packaged
starter after copying it out of the installed skill into its own directory.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import time
from urllib.parse import unquote


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def snapshot(root: Path) -> dict[str, str]:
    return {
        str(path.relative_to(root)): digest(path)
        for path in root.rglob("*")
        if path.is_file() and not path.is_symlink()
    }


def run(command: list[str], cwd: Path, *, success: bool = True) -> subprocess.CompletedProcess[str]:
    result = subprocess.run(command, cwd=cwd, text=True, capture_output=True)
    if (result.returncode == 0) != success:
        raise AssertionError(
            f"Unexpected exit {result.returncode}: {' '.join(command)}\n"
            f"{result.stdout[-12000:]}\n{result.stderr[-12000:]}"
        )
    return result


def check_manifest(package: Path) -> int:
    manifest = json.loads((package / "manifest.json").read_text())
    assert manifest["name"] == "minecraft-gui", "Unexpected installed package name"
    declared = manifest["files"]
    # The package manifest itself is excluded; nested manifests are payload.
    actual = {
        path.relative_to(package).as_posix()
        for path in package.rglob("*")
        if path.is_file() and path != package / "manifest.json"
    }
    assert actual == set(declared), f"Installed payload differs from manifest: {actual ^ set(declared)}"
    for relative, spec in declared.items():
        path = package / relative
        assert not path.is_symlink(), f"Installed symlink: {relative}"
        assert digest(path) == spec["sha256"], f"Wrong installed hash: {relative}"
        if os.name != "nt":
            actual_mode = path.stat().st_mode & 0o777
            assert actual_mode == int(spec["mode"], 8), f"Wrong installed mode: {relative}"
    return len(actual)


def check_links(package: Path) -> int:
    count = 0
    broken: list[str] = []
    for path in package.rglob("*.md"):
        for raw in re.findall(r"\]\(([^)]+)\)", path.read_text()):
            target = raw.strip().split(' "', 1)[0]
            if target.startswith(("http://", "https://", "mailto:", "#", "data:")):
                continue
            target = unquote(target.split("#", 1)[0].strip("<>"))
            if not target:
                continue
            resolved = (path.parent / target).resolve()
            count += 1
            if not resolved.is_relative_to(package.resolve()) or not resolved.exists():
                broken.append(f"{path.relative_to(package)} -> {raw}")
    assert not broken, "Broken or external local package links:\n" + "\n".join(broken)
    return count


def verify(repo: Path, workspace: Path, build_starter: bool) -> list[dict[str, object]]:
    results: list[dict[str, object]] = []
    package = repo / ".agents/skills/minecraft-gui"
    installer = repo / "scripts/install.py"
    assert installer.is_file(), "Installer has not been built"
    assert (package / "manifest.json").is_file(), "Build the self-contained skill package first"

    for agent, directory, rules_file in (
        ("codex", ".agents", "AGENTS.md"),
        ("claude", ".claude", "CLAUDE.md"),
    ):
        project = workspace / agent
        project.mkdir()
        rules = project / rules_file
        sentinel = "# Existing project instructions\n\nPreserve this repository rule.\n"
        rules.write_text(sentinel)
        (project / "local-code.java").write_text("// Existing client code remains untouched.\n")
        command = [sys.executable, str(installer), "--project", str(project), "--agent", agent,
                   "--source", str(package), "--json"]

        original = snapshot(project)
        run([*command, "--dry-run"], repo)
        assert snapshot(project) == original, "Dry-run modified consumer files"
        run(command, repo)
        installed = project / directory / "skills/minecraft-gui"
        files = check_manifest(installed)
        links = check_links(installed)
        assert rules.read_text().startswith(sentinel), "Installer overwrote existing project rules"
        assert "minecraft-gui" in rules.read_text(), "Project rules do not reference the entry skill"
        assert (project / "local-code.java").read_text() == "// Existing client code remains untouched.\n"
        assert (project / "gui-toolkit.lock.json").is_file(), "No toolkit install record"

        first = snapshot(project)
        run(command, repo)
        assert snapshot(project) == first, "Repeated installation is not content-idempotent"
        results.append({"check": f"{agent} separate-project install, links, hashes, preservation, repeat", "status": "pass", "payload_files": files, "local_links": links})

        managed = installed / "SKILL.md"
        managed.write_text(managed.read_text() + "\nConsumer-local change.\n")
        modified = snapshot(project)
        run(command, repo, success=False)
        assert snapshot(project) == modified, "Conflict handling partially modified the consumer"
        results.append({"check": f"{agent} local edit conflict leaves all files untouched", "status": "pass"})

    mixed = workspace / "both-agents"
    mixed.mkdir()
    for agent in ("codex", "claude"):
        run([sys.executable, str(installer), "--project", str(mixed), "--agent", agent,
             "--source", str(package), "--json"], repo)
    check_manifest(mixed / ".agents/skills/minecraft-gui")
    check_manifest(mixed / ".claude/skills/minecraft-gui")
    mixed_lock = (mixed / "gui-toolkit.lock.json").read_text()
    assert '"codex"' in mixed_lock and '"claude"' in mixed_lock, "Installing a second agent lost the first record"
    mixed_snapshot = snapshot(mixed)
    for agent in ("codex", "claude"):
        run([sys.executable, str(installer), "--project", str(mixed), "--agent", agent,
             "--source", str(package), "--json"], repo)
    assert snapshot(mixed) == mixed_snapshot, "Two-agent installation is not content-idempotent"
    results.append({"check": "two agents coexist and retain their install records", "status": "pass"})

    # Inputs are supplied as files, so hash failure must not mutate a consumer.
    corrupted = workspace / "corrupted-source"
    shutil.copytree(package, corrupted)
    source_skill = corrupted / "SKILL.md"
    source_skill.write_text(source_skill.read_text() + "\nUnrecorded source change.\n")
    corrupt_consumer = workspace / "corrupt-consumer"
    corrupt_consumer.mkdir()
    (corrupt_consumer / "AGENTS.md").write_text("Keep existing instructions.\n")
    original = snapshot(corrupt_consumer)
    run([sys.executable, str(installer), "--project", str(corrupt_consumer), "--agent", "codex",
         "--source", str(corrupted), "--json"], repo, success=False)
    assert snapshot(corrupt_consumer) == original, "Invalid source partially modified consumer"
    results.append({"check": "corrupted package is rejected before consumer writes", "status": "pass"})

    if os.name != "nt":
        outside = workspace / "outside-project"
        outside.mkdir()
        (outside / "sentinel.txt").write_text("Do not write through project symlinks.\n")
        symlink_consumer = workspace / "symlink-consumer"
        symlink_consumer.mkdir()
        (symlink_consumer / ".agents").symlink_to(outside, target_is_directory=True)
        original_outside = snapshot(outside)
        original_consumer = snapshot(symlink_consumer)
        run([sys.executable, str(installer), "--project", str(symlink_consumer), "--agent", "codex",
             "--source", str(package), "--json"], repo, success=False)
        assert (symlink_consumer / ".agents").is_symlink(), "Installer replaced project symlink"
        assert snapshot(outside) == original_outside, "Installer wrote outside project through symlink"
        assert snapshot(symlink_consumer) == original_consumer, "Symlink refusal partially modified consumer"
        results.append({"check": "destination symlink is refused without external or partial writes", "status": "pass"})

    # Skill text was edited above; the installed template remains its original payload.
    template = workspace / "codex/.agents/skills/minecraft-gui/templates/starter"
    if build_starter:
        assert (template / "gradlew").is_file(), "Packaged standalone starter is missing"
        starter = workspace / "standalone-starter"
        shutil.copytree(template, starter)
        result = run([str(starter / "gradlew"), "build", "--no-daemon"], starter)
        assert "BUILD SUCCESSFUL" in result.stdout, "Starter did not report successful compilation"
        results.append({"check": "standalone packaged starter build outside toolkit checkout", "status": "pass"})
    else:
        results.append({"check": "standalone packaged starter build", "status": "not-run", "reason": "Pass --build-starter to compile the exact packaged source snapshot."})

    return results


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument("--output", type=Path)
    parser.add_argument("--workdir", type=Path, help="Keep consumer projects in a new empty directory")
    parser.add_argument("--build-starter", action="store_true")
    args = parser.parse_args()
    started = time.monotonic()
    temporary = None
    if args.workdir:
        workspace = args.workdir.resolve()
        workspace.mkdir(parents=True, exist_ok=False)
    else:
        temporary = tempfile.TemporaryDirectory(prefix="gui-toolkit-consumer-")
        workspace = Path(temporary.name)
    try:
        checks = verify(args.repo.resolve(), workspace, args.build_starter)
        report = {"status": "pass", "elapsed_seconds": round(time.monotonic() - started, 2), "checks": checks}
    except (AssertionError, KeyError, OSError, ValueError, subprocess.SubprocessError) as error:
        report = {"status": "fail", "elapsed_seconds": round(time.monotonic() - started, 2), "error": str(error)}
    finally:
        if temporary:
            temporary.cleanup()
    content = json.dumps(report, indent=2) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(content)
    print(content, end="")
    return 0 if report["status"] == "pass" else 1


if __name__ == "__main__":
    raise SystemExit(main())
