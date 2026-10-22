#!/usr/bin/env python3
"""Check tracked documentation, local links, and Gradle wrapper provenance."""

from __future__ import annotations

import hashlib
import ast
import json
from pathlib import Path
import re
import subprocess
import sys
from urllib.parse import unquote


def validate(root: Path) -> list[str]:
    errors: list[str] = []
    # Check the committed project and newly added source files, excluding build
    # output. This works before the initial implementation commit as well as CI.
    result = subprocess.run(
        ["git", "ls-files", "--cached", "--others", "--exclude-standard", "-z"],
        cwd=root, check=True, capture_output=True,
    )
    paths = sorted({root / part.decode() for part in result.stdout.split(b"\0") if part})
    markdown = [path for path in paths if path.suffix == ".md" and path.is_file()]
    local_links = 0
    for path in markdown:
        source = path.read_text(encoding="utf-8")
        if len(re.findall(r"^```", source, re.MULTILINE)) % 2:
            errors.append(f"{path.relative_to(root)}: unbalanced fenced code block")
        for raw in re.findall(r"\]\(([^)]+)\)", source):
            target = raw.strip().split(' "', 1)[0].strip("<>")
            if re.match(r"^[a-zA-Z][a-zA-Z0-9+.-]*:", target) or target.startswith("#"):
                continue
            filename = unquote(target.split("#", 1)[0])
            if not filename:
                continue
            local_links += 1
            resolved = (path.parent / filename).resolve()
            if not resolved.is_relative_to(root.resolve()) or not resolved.exists():
                errors.append(f"{path.relative_to(root)}: missing or external local link {raw}")

    properties = root / "gradle/wrapper/gradle-wrapper.properties"
    jar = root / "gradle/wrapper/gradle-wrapper.jar"
    if not properties.is_file() or not jar.is_file():
        errors.append("Gradle wrapper jar/properties missing")
    else:
        source = properties.read_text()
        if not re.search(r"^distributionSha256Sum=[0-9a-f]{64}$", source, re.MULTILINE):
            errors.append("Gradle distribution is missing its exact SHA-256")
        # Require both the distribution checksum and the wrapper jar identity.
        checksum_file = root / "gradle/wrapper/gradle-wrapper.jar.sha256"
        if checksum_file.is_file():
            words = checksum_file.read_text().split()
            expected = words[0] if words else ""
            actual = hashlib.sha256(jar.read_bytes()).hexdigest()
            if not re.fullmatch(r"[0-9a-f]{64}", expected) or expected != actual:
                errors.append("Gradle wrapper jar SHA-256 mismatch")
        else:
            errors.append("Gradle wrapper jar SHA-256 file missing")

    # A source snapshot, skill package, and runtime must describe one release.
    token_version = json.loads((root / "design/tokens.json").read_text())["toolkitVersion"]
    package_ast = ast.parse((root / "scripts/package_skill.py").read_text())
    package_versions = [
        node.value.value for node in package_ast.body
        if isinstance(node, ast.Assign) and isinstance(node.value, ast.Constant)
        and any(isinstance(target, ast.Name) and target.id == "VERSION" for target in node.targets)
    ]
    runtime_versions = re.findall(r"^\s*version\s*=\s*['\"]([^'\"]+)['\"]", (root / "build.gradle").read_text(), re.MULTILINE)
    if package_versions != [token_version] or runtime_versions != [token_version]:
        errors.append("Toolkit package, design tokens, and Gradle release versions differ")
    print(f"Checked {len(markdown)} Markdown files and {local_links} local links.")
    return errors


def main() -> int:
    root = Path(__file__).resolve().parents[1]
    errors = validate(root)
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print("Repository checks passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
