#!/usr/bin/env python3
"""Build or check the self-contained minecraft-gui skill and runnable starter."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import stat
import sys
import tempfile


VERSION = "0.1.0"
REPOSITORY = "https://github.com/italixcs/Gui-animations"
SEED_FILES = (
    "SKILL.md", "references/design.md", "references/implementation.md",
    "references/motion.md", "references/review.md",
)
ROOT_STARTER_FILES = (
    "LICENSE", "THIRD_PARTY_NOTICES.md", "licenses/Gradle-LICENSE.txt", "licenses/Gradle-NOTICE.txt",
    "settings.gradle", "build.gradle", "gradle.properties", "gradlew", "gradlew.bat",
    "gradle/wrapper/gradle-wrapper.jar", "gradle/wrapper/gradle-wrapper.properties",
    "gradle/wrapper/gradle-wrapper.jar.sha256",
    "design/validate.py", "tests/design/test_tokens.py",
)
MODULES = ("reference/core", "adapters/fabric-modernui", "examples/showcase-mod")
IGNORED_PARTS = {"build", "run", ".gradle", ".git", "__pycache__", ".idea", ".vscode"}


class PackageError(Exception):
    pass


def digest(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest()


def json_bytes(value: object) -> bytes:
    return (json.dumps(value, indent=2, sort_keys=True, ensure_ascii=False) + "\n").encode("utf-8")


def add_file(payload: dict, target: str, source: Path) -> None:
    if not source.is_file() or source.is_symlink():
        raise PackageError(f"Required package input is missing or a symlink: {source}")
    if target in payload:
        raise PackageError(f"Duplicate package destination: {target}")
    executable = source.name == "gradlew" or bool(source.stat().st_mode & stat.S_IXUSR)
    payload[target] = (source.read_bytes(), 0o755 if executable else 0o644)


def source_files(directory: Path) -> list[Path]:
    if not directory.is_dir() or directory.is_symlink():
        raise PackageError(f"Required source directory is missing or a symlink: {directory}")
    result = []
    for path in sorted(directory.rglob("*")):
        relative = path.relative_to(directory)
        if any(part in IGNORED_PARTS for part in relative.parts):
            continue
        if path.is_symlink():
            raise PackageError(f"Refusing source symlink: {path}")
        if path.is_file():
            if path.name.endswith((".class", ".log", ".tmp", ".pyc")):
                continue
            result.append(path)
    return result


def build_payload(root: Path) -> dict[str, tuple[bytes, int]]:
    seed = root / ".agents/skills/minecraft-gui"
    payload = {}
    for relative in SEED_FILES:
        add_file(payload, relative, seed / relative)
    add_file(payload, "scripts/install.py", root / "scripts/install.py")
    add_file(payload, "LICENSE", root / "LICENSE")
    for source in source_files(root / "design"):
        relative = source.relative_to(root / "design")
        if not (
            relative.as_posix() in ("tokens.json", "tokens.schema.json", "README.md")
            or (relative.parts[0] == "fonts" and source.suffix in (".ttf", ".otf", ".txt", ".md"))
            or (relative.parts[0] == "component-specs" and source.suffix == ".md")
        ):
            continue
        add_file(payload, "templates/starter/" + source.relative_to(root).as_posix(), source)
    for relative in ROOT_STARTER_FILES:
        add_file(payload, "templates/starter/" + relative, root / relative)
    for module in MODULES:
        files = source_files(root / module)
        if not any(path.name == "build.gradle" for path in files):
            raise PackageError(f"Module has no build.gradle: {module}")
        for source in files:
            relative = source.relative_to(root / module)
            if not (relative.as_posix() in ("build.gradle", "gradle.properties", "README.md") or (relative.parts[0] == "src" and not any(part.startswith(".") for part in relative.parts))):
                continue
            add_file(payload, "templates/starter/" + source.relative_to(root).as_posix(), source)
    source_map = (
        "# Packaged source map\n\n"
        "This package contains the exact versioned starter sources and build pins shipped with this toolkit.\n\n"
        "- [Standalone starter](../templates/starter/README.md): build and run the three-module demo.\n"
        "- [Core module](../templates/starter/reference/core/build.gradle): optional elapsed-time motion helpers.\n"
        "- [ModernUI adapter](../templates/starter/adapters/fabric-modernui/build.gradle): shared visual components and resources.\n"
        "- [Showcase module](../templates/starter/examples/showcase-mod/build.gradle): client entry point and working settings screen.\n"
        "- [Version pins](../templates/starter/gradle.properties): authoritative Minecraft, loader, renderer, and build-tool versions.\n"
        "- [Design tokens](../templates/starter/design/tokens.json) and [component specifications](../templates/starter/design/component-specs/README.md).\n\n"
        "- [Third-party notices](../templates/starter/THIRD_PARTY_NOTICES.md): font, wrapper, and dependency attribution.\n\n"
        "Use sources matching the consuming project's build. A published source snapshot is not evidence that another Minecraft version works.\n"
    )
    payload["references/source-map.md"] = (source_map.encode("utf-8"), 0o644)
    starter_readme = (
        "# Gui-animations starter\n\n"
        "This is a self-contained source snapshot of the toolkit's Minecraft showcase. "
        "It keeps the validated demo namespace; rename packages and resource identifiers deliberately when adapting it.\n\n"
        "Install JDK 25, then run:\n\n"
        "```sh\n./gradlew build\n./gradlew :examples:showcase-mod:runClient\n```\n\n"
        "Press **F8** from the title screen or a world to open the showcase. Escape closes the top dialog first, then the screen. "
        "For automated launch, use `./gradlew :examples:showcase-mod:runClient -Dguianimations.demo.autoOpen=true`.\n\n"
        "Check `gradle.properties` and the module build files for exact dependency pins. "
        "The development client uses the mod's demo account; this is a client-mod starter, not an official Minecraft client distribution.\n\n"
        "Use the installed minecraft-gui skill for design and implementation guidance. "
        "Keep the bundled font's OFL notice and the renderer's own licenses with redistributed dependencies.\n"
    )
    payload["templates/starter/README.md"] = (starter_readme.encode("utf-8"), 0o644)
    payload["templates/starter/.gitignore"] = (b".gradle/\n**/build/\n**/run/\n*.log\n", 0o644)
    compatibility = {
        "schema_version": 1, "toolkit_version": VERSION,
        "target": {
            "minecraft": "26.1.2", "loader": "fabric", "java": 25,
            "mappings": "official Mojang names (unobfuscated Minecraft)",
            "version_pins": "../templates/starter/gradle.properties",
        },
        "scope": "Packaged starter sources target this stack only. Consult release verification evidence for observed runtime coverage.",
    }
    payload["references/compatibility.json"] = (json_bytes(compatibility), 0o644)
    return payload


def package_files(root: Path, source_revision: str | None = None) -> dict[str, tuple[bytes, int]]:
    payload = build_payload(root)
    records = {relative: {"sha256": digest(content), "mode": f"{mode:04o}"} for relative, (content, mode) in sorted(payload.items())}
    manifest = {
        "schema_version": 1, "name": "minecraft-gui", "version": VERSION,
        "source": {"repository": REPOSITORY, "revision": source_revision, "payload_sha256": digest(json_bytes(records))},
        "files": records,
    }
    payload["manifest.json"] = (json_bytes(manifest), 0o644)
    return payload


def compare(output: Path, payload: dict) -> list[str]:
    actual = set()
    mismatches = []
    if output.is_dir():
        for path in output.rglob("*"):
            if path.is_symlink():
                mismatches.append(f"symlink: {path.relative_to(output)}")
            elif path.is_file():
                actual.add(path.relative_to(output).as_posix())
    for relative, (content, mode) in payload.items():
        path = output / relative
        if path.is_symlink() or not path.is_file() or path.read_bytes() != content or (os.name != "nt" and stat.S_IMODE(path.stat().st_mode) != mode):
            mismatches.append(relative)
    mismatches.extend(f"unexpected: {relative}" for relative in sorted(actual - set(payload)))
    return mismatches


def build(root: Path, output: Path, *, check: bool = False, source_revision: str | None = None) -> dict:
    if output.is_symlink() or (output.exists() and not output.is_dir()):
        raise PackageError(f"Package output must be a real directory: {output}")
    payload = package_files(root, source_revision)
    mismatches = compare(output, payload)
    if check:
        if mismatches:
            raise PackageError("Skill package is stale or incomplete; run scripts/package_skill.py:\n  " + "\n  ".join(mismatches))
    elif mismatches:
        output.parent.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(prefix="minecraft-gui-package-", dir=output.parent) as directory:
            staged = Path(directory) / "minecraft-gui"
            for relative, (content, mode) in payload.items():
                path = staged / relative
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(content)
                path.chmod(mode)
            # Preserve the seed until a complete staged package exists.
            backup = output.with_name(output.name + ".package-backup")
            if backup.exists():
                raise PackageError(f"Package backup already exists: {backup}; inspect it before rebuilding")
            if output.exists():
                output.rename(backup)
            try:
                staged.rename(output)
            except OSError:
                if backup.exists():
                    backup.rename(output)
                raise
            if backup.exists():
                shutil.rmtree(backup)
    return {"output": str(output), "files": len(payload), "version": VERSION, "check": check}


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    root = Path(__file__).resolve().parent.parent
    parser.add_argument("--root", type=Path, default=root, help="Canonical toolkit checkout")
    parser.add_argument("--output", type=Path, default=None, help="Package directory; defaults to .agents/skills/minecraft-gui in checkout")
    parser.add_argument("--check", action="store_true", help="Fail when generated package differs from canonical inputs")
    parser.add_argument("--source-revision", default=None, help="Optional exact clean source revision for release provenance")
    args = parser.parse_args(argv)
    if args.source_revision is not None and not re_revision(args.source_revision):
        parser.error("--source-revision must be a full 40-character Git SHA")
    root = args.root.resolve()
    output = args.output or root / ".agents/skills/minecraft-gui"
    try:
        print(json.dumps(build(root, output, check=args.check, source_revision=args.source_revision), sort_keys=True))
    except (PackageError, OSError) as exc:
        print(f"minecraft-gui package: {exc}", file=sys.stderr)
        return 2
    return 0


def re_revision(value: str) -> bool:
    return len(value) == 40 and all(char in "0123456789abcdef" for char in value)


if __name__ == "__main__":
    raise SystemExit(main())
