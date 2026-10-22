#!/usr/bin/env python3
"""Install a verified minecraft-gui skill package without overwriting local edits."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import stat
import sys
import tempfile
from dataclasses import dataclass


NAME = "minecraft-gui"
LOCK_NAME = "gui-toolkit.lock.json"
AGENTS = {
    "codex": (".agents/skills/minecraft-gui", "AGENTS.md"),
    "claude": (".claude/skills/minecraft-gui", "CLAUDE.md"),
}
BEGIN = "<!-- minecraft-gui:begin -->"
END = "<!-- minecraft-gui:end -->"


class InstallError(Exception):
    """A package, path, or local modification prevents safe installation."""


@dataclass
class Change:
    path: Path
    content: bytes | None
    mode: int
    old_content: bytes | None
    old_mode: int


def digest(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest()


def json_bytes(value: object) -> bytes:
    return (json.dumps(value, indent=2, sort_keys=True, ensure_ascii=False) + "\n").encode("utf-8")


def relative_path(value: object) -> str:
    if not isinstance(value, str) or not value or "\\" in value or ":" in value or "\0" in value:
        raise InstallError(f"Invalid package path: {value!r}")
    path = PurePosixPath(value)
    if path.is_absolute() or any(part in ("", ".", "..") for part in value.split("/")):
        raise InstallError(f"Unsafe relative path: {value!r}")
    return value


def checked_path(root: Path, relative: str) -> Path:
    relative_path(relative)
    target = root / relative
    current = root
    if root.is_symlink():
        raise InstallError(f"Refusing a symlink as project/package root: {root}")
    for index, part in enumerate(PurePosixPath(relative).parts):
        current /= part
        if current.is_symlink():
            raise InstallError(f"Refusing to traverse or overwrite a symlink: {current}")
        if current.exists() and index < len(PurePosixPath(relative).parts) - 1 and not current.is_dir():
            raise InstallError(f"A parent path is not a directory: {current}")
    if target.exists() and not target.is_file():
        raise InstallError(f"Expected a file: {target}")
    return target


def load_object(path: Path, purpose: str) -> dict:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError, UnicodeError) as exc:
        raise InstallError(f"Cannot read {purpose} at {path}: {exc}") from exc
    if not isinstance(value, dict):
        raise InstallError(f"{purpose} must be a JSON object: {path}")
    return value


def payload_digest(files: dict) -> str:
    return digest(json_bytes(files))


def validate_file_records(records: object, purpose: str) -> dict:
    if not isinstance(records, dict):
        raise InstallError(f"{purpose} must contain a files object")
    for relative, record in records.items():
        relative_path(relative)
        if not isinstance(record, dict) or not re.fullmatch(r"[0-9a-f]{64}", str(record.get("sha256", ""))):
            raise InstallError(f"Invalid hash record for {relative} in {purpose}")
        if record.get("mode") not in ("0644", "0755"):
            raise InstallError(f"Invalid file mode for {relative} in {purpose}")
    return records


def read_package(source: Path) -> tuple[dict, dict[str, tuple[bytes, int]]]:
    if not source.is_dir() or source.is_symlink():
        raise InstallError(f"Package directory does not exist or is a symlink: {source}")
    manifest_path = checked_path(source, "manifest.json")
    manifest = load_object(manifest_path, "package manifest")
    if manifest.get("schema_version") != 1 or manifest.get("name") != NAME:
        raise InstallError("Unsupported package manifest schema or name")
    if not re.fullmatch(r"\d+\.\d+\.\d+(?:[-+][A-Za-z0-9.-]+)?", str(manifest.get("version", ""))):
        raise InstallError("Package version must be a semantic version")
    records = validate_file_records(manifest.get("files"), "package manifest")
    if "SKILL.md" not in records or "manifest.json" in records:
        raise InstallError("Package must contain SKILL.md and must not hash its own manifest")
    source_info = manifest.get("source")
    if not isinstance(source_info, dict) or source_info.get("payload_sha256") != payload_digest(records):
        raise InstallError("Package payload identity does not match its file records")
    actual = set()
    for path in source.rglob("*"):
        if path.is_symlink():
            raise InstallError(f"Package contains a symlink: {path}")
        if path.is_file():
            actual.add(path.relative_to(source).as_posix())
    if actual != set(records) | {"manifest.json"}:
        raise InstallError("Package file list differs from manifest; rebuild the package before installing")
    contents = {}
    for relative, record in records.items():
        path = checked_path(source, relative)
        try:
            content = path.read_bytes()
        except OSError as exc:
            raise InstallError(f"Missing package file {relative}: {exc}") from exc
        if digest(content) != record["sha256"]:
            raise InstallError(f"Package integrity check failed: {relative}")
        contents[relative] = (content, int(record["mode"], 8))
    contents["manifest.json"] = (manifest_path.read_bytes(), 0o644)
    return manifest, contents


def make_rules_block(skill_path: str) -> bytes:
    return (
        f"{BEGIN}\n"
        "## Minecraft GUI toolkit\n\n"
        f"For Minecraft GUI work, use the [minecraft-gui skill]({skill_path}/SKILL.md). "
        "Reuse this project's shared theme and components, follow its design and motion rules, "
        "and verify against the project's actual Minecraft stack. Explicit user instructions "
        "and the project's existing rules still apply.\n"
        f"{END}"
    ).encode("utf-8")


def update_rules(content: bytes, block: bytes, previous_hash: str | None, path: Path) -> bytes:
    try:
        text = content.decode("utf-8")
    except UnicodeError as exc:
        raise InstallError(f"Agent rules must be UTF-8: {path}") from exc
    starts, ends = text.count(BEGIN), text.count(END)
    if starts != ends or starts > 1:
        raise InstallError(f"Malformed or duplicate minecraft-gui managed block in {path}")
    if starts:
        start, end = text.index(BEGIN), text.index(END) + len(END)
        if end < start:
            raise InstallError(f"Malformed minecraft-gui managed block in {path}")
        existing = text[start:end].encode("utf-8")
        if existing != block and (previous_hash is None or digest(existing) != previous_hash):
            raise InstallError(f"Local edits in managed rules block: {path}; preserve or remove them explicitly before retrying")
        return content[:len(text[:start].encode("utf-8"))] + block + content[len(text[:end].encode("utf-8")):]
    if previous_hash:
        raise InstallError(f"Managed rules block was removed from {path}; refusing to restore it without review")
    separator = b"" if not content else (b"\n" if content.endswith(b"\n") else b"\n\n")
    return content + separator + block + b"\n"


def file_record(content: bytes, mode: int) -> dict[str, str]:
    return {"sha256": digest(content), "mode": f"{mode:04o}"}


def same_mode(actual: int, desired: int) -> bool:
    # Windows has no POSIX executable bit; preserve metadata for portable exports
    # without treating that platform difference as a user modification.
    return os.name == "nt" or actual == desired


def propose_file(
    project: Path, relative: str, desired: tuple[bytes, int] | None,
    previous: dict | None, changes: list[Change],
) -> None:
    path = checked_path(project, relative)
    old = path.read_bytes() if path.exists() else None
    old_mode = stat.S_IMODE(path.stat().st_mode) if path.exists() else 0o644
    current_hash = digest(old) if old is not None else None
    desired_hash = digest(desired[0]) if desired is not None else None
    previous_hash = previous.get("sha256") if previous else None
    if old is None and previous is not None and desired is not None:
        raise InstallError(f"Previously installed file was removed: {relative}; refusing to restore local deletion")
    if old is not None and current_hash != desired_hash and (previous is None or current_hash != previous_hash):
        raise InstallError(f"Local/unmanaged file conflict: {relative}; no files were changed")
    if old is not None and previous is not None and not same_mode(old_mode, int(previous["mode"], 8)) and (desired is None or not same_mode(old_mode, desired[1])):
        raise InstallError(f"Local file-mode conflict: {relative}")
    if desired is None:
        if old is not None:
            changes.append(Change(path, None, old_mode, old, old_mode))
    else:
        content, mode = desired
        if old != content or not same_mode(old_mode, mode):
            changes.append(Change(path, content, mode, old, old_mode))


def reserved_import_path(relative: str) -> bool:
    return relative in (LOCK_NAME, "AGENTS.md", "CLAUDE.md") or PurePosixPath(relative).parts[0] in (".git", ".agents", ".claude")


def atomic_write(path: Path, content: bytes, mode: int) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = None
    try:
        with tempfile.NamedTemporaryFile(prefix=".minecraft-gui-", dir=path.parent, delete=False) as stream:
            temporary = Path(stream.name)
            stream.write(content)
            stream.flush()
            os.fsync(stream.fileno())
        temporary.chmod(mode)
        os.replace(temporary, path)
    finally:
        if temporary is not None and temporary.exists():
            temporary.unlink()


def apply_changes(changes: list[Change], project: Path) -> None:
    applied = []
    created_directories = set()
    try:
        for change in changes:
            parent = change.path.parent
            while parent != project.parent and not parent.exists():
                created_directories.add(parent)
                parent = parent.parent
            applied.append(change)
            if change.content is None:
                change.path.unlink()
            else:
                atomic_write(change.path, change.content, change.mode)
    except (OSError, InstallError) as exc:
        rollback_errors = []
        for change in reversed(applied):
            try:
                if change.old_content is None:
                    if change.path.exists():
                        change.path.unlink()
                else:
                    atomic_write(change.path, change.old_content, change.old_mode)
            except OSError as rollback_exc:
                rollback_errors.append(str(rollback_exc))
        for directory in sorted(created_directories, key=lambda value: len(value.parts), reverse=True):
            try:
                directory.rmdir()
            except OSError:
                pass
        detail = f"; rollback errors: {'; '.join(rollback_errors)}" if rollback_errors else "; previous files restored"
        raise InstallError(f"Installation could not complete: {exc}{detail}") from exc


def install(source: Path, project: Path, agent: str, *, dry_run: bool = False, starter: bool = False) -> dict:
    if agent not in AGENTS:
        raise InstallError(f"Unsupported agent: {agent}; choose codex or claude")
    source = Path(os.path.abspath(source.expanduser()))
    project = Path(os.path.abspath(project.expanduser()))
    if project.is_symlink() or (project.exists() and not project.is_dir()):
        raise InstallError(f"Project must be a real directory: {project}")
    if not project.exists() and not starter:
        raise InstallError(f"Project directory does not exist: {project}; create it first or use --starter")
    # Check existing ancestors too; a symlinked parent could redirect an installation.
    for parent in (project, *project.parents):
        if parent.is_symlink():
            raise InstallError(f"Refusing a symlinked project path: {parent}")
    manifest, package = read_package(source)
    skill_path, rules_name = AGENTS[agent]
    lock_path = checked_path(project, LOCK_NAME)
    lock = load_object(lock_path, "toolkit lock") if lock_path.exists() else {"schema_version": 1, "name": NAME, "installations": {}}
    if lock.get("schema_version") != 1 or lock.get("name") != NAME or not isinstance(lock.get("installations"), dict):
        raise InstallError("Unsupported toolkit lock schema")
    previous = lock["installations"].get(agent, {})
    if not isinstance(previous, dict):
        raise InstallError("Invalid toolkit lock installation record")
    old_files = validate_file_records(previous.get("files", {}), "toolkit lock")
    old_imports = validate_file_records(previous.get("imports", {}), "toolkit imports")
    for relative in old_files:
        if not relative.startswith(skill_path + "/"):
            raise InstallError(f"Lock file contains an unexpected managed path: {relative}")
    for relative in old_imports:
        if reserved_import_path(relative):
            raise InstallError(f"Lock file contains a reserved imported path: {relative}")
    desired = {f"{skill_path}/{relative}": value for relative, value in package.items()}
    changes = []
    for relative in sorted(set(old_files) | set(desired)):
        propose_file(project, relative, desired.get(relative), old_files.get(relative), changes)
    imports = dict(old_imports)
    imports_source = previous.get("imports_source")
    if starter:
        starter_prefix = "templates/starter/"
        starter_files = {relative.removeprefix(starter_prefix): value for relative, value in package.items() if relative.startswith(starter_prefix)}
        if not starter_files or "gradlew" not in starter_files:
            raise InstallError("This package has no complete runnable starter")
        for relative in starter_files:
            if reserved_import_path(relative):
                raise InstallError(f"Starter contains a reserved project path: {relative}")
        for relative in sorted(set(old_imports) | set(starter_files)):
            propose_file(project, relative, starter_files.get(relative), old_imports.get(relative), changes)
        imports = {relative: file_record(*value) for relative, value in starter_files.items()}
        imports_source = {"version": manifest["version"], "source": manifest["source"]}
    rules_path = checked_path(project, rules_name)
    existing_rules = rules_path.read_bytes() if rules_path.exists() else b""
    rules_mode = stat.S_IMODE(rules_path.stat().st_mode) if rules_path.exists() else 0o644
    block = make_rules_block(skill_path)
    rules = update_rules(existing_rules, block, previous.get("rules_block_sha256"), rules_path)
    if rules != existing_rules:
        changes.append(Change(rules_path, rules, rules_mode, existing_rules if rules_path.exists() else None, rules_mode))
    lock["installations"][agent] = {
        "version": manifest["version"], "source": manifest["source"], "skill_path": skill_path,
        "rules_path": rules_name, "rules_block_sha256": digest(block),
        "files": {relative: file_record(*value) for relative, value in desired.items()},
        "imports": imports,
        "imports_source": imports_source,
    }
    lock_content = json_bytes(lock)
    old_lock = lock_path.read_bytes() if lock_path.exists() else None
    if lock_content != old_lock:
        changes.append(Change(lock_path, lock_content, 0o644, old_lock, stat.S_IMODE(lock_path.stat().st_mode) if lock_path.exists() else 0o644))
    result = {
        "agent": agent, "project": str(project), "version": manifest["version"], "dry_run": dry_run,
        "starter": starter, "skill_path": skill_path,
        "changes": [{"path": change.path.relative_to(project).as_posix(), "action": "remove" if change.content is None else ("create" if change.old_content is None else "update")} for change in changes],
    }
    if not dry_run:
        apply_changes(changes, project)
    return result


def default_source() -> Path:
    script_root = Path(__file__).resolve().parent
    if (script_root.parent / "manifest.json").is_file():
        return script_root.parent
    return script_root.parent / ".agents" / "skills" / NAME


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", type=Path, required=True, help="Existing client/mod project, or new directory with --starter")
    parser.add_argument("--agent", choices=sorted(AGENTS), default="codex")
    parser.add_argument("--source", type=Path, default=None, help="Self-contained package directory; defaults to the repository or package containing this script")
    parser.add_argument("--starter", action="store_true", help="Also import the validated standalone demo project; refuses conflicting or locally edited files")
    parser.add_argument("--dry-run", action="store_true", help="Validate and list changes without writing any files")
    parser.add_argument("--json", action="store_true", help="Print a machine-readable result")
    args = parser.parse_args(argv)
    try:
        result = install(args.source or default_source(), args.project, args.agent, dry_run=args.dry_run, starter=args.starter)
    except (InstallError, OSError) as exc:
        print(f"minecraft-gui: {exc}", file=sys.stderr)
        return 2
    if args.json:
        print(json.dumps(result, indent=2, sort_keys=True))
    else:
        verb = "Would apply" if args.dry_run else "Applied"
        print(f"{verb} {len(result['changes'])} change(s) for minecraft-gui {result['version']} ({args.agent}) in {result['project']}")
        print(f"Entry point: {result['skill_path']}/SKILL.md")
        if args.dry_run:
            for change in result["changes"]:
                print(f"  {change['action']}: {change['path']}")
        if args.starter:
            print("Starter command: ./gradlew :examples:showcase-mod:runClient")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
