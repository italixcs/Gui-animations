#!/usr/bin/env python3
"""Validate the repository's design-token schema, contrast, geometry, and assets.

Only Python's standard library is required. The schema walker intentionally
supports the JSON Schema keywords used by tokens.schema.json, not arbitrary
third-party schemas. Runtime and package copies must preserve token-relative
font paths. Call validate_tokens() in tests or run this file directly.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
from pathlib import Path
import re
import sys
from typing import Any


DESIGN_DIR = Path(__file__).resolve().parent
SCHEMA_PATH = DESIGN_DIR / "tokens.schema.json"
TYPE_NAMES = {
    "object": lambda value: isinstance(value, dict),
    "array": lambda value: isinstance(value, list),
    "string": lambda value: isinstance(value, str),
    "boolean": lambda value: isinstance(value, bool),
    "integer": lambda value: isinstance(value, int) and not isinstance(value, bool),
    "number": lambda value: isinstance(value, (int, float)) and not isinstance(value, bool) and (not isinstance(value, float) or math.isfinite(value)),
}
REQUIRED_CONTRAST = {
    ("textPrimary", "background"): 4.5,
    ("textPrimary", "panel"): 4.5,
    ("textPrimary", "raised"): 4.5,
    ("textSecondary", "raised"): 4.5,
    ("textDisabled", "raised"): 4.5,
    ("textOnAccent", "accent"): 4.5,
    ("textOnAccent", "accentHover"): 4.5,
    ("textOnAccent", "accentPressed"): 4.5,
    ("border", "raised"): 3.0,
    ("focus", "raised"): 3.0,
    ("textPrimary", "border"): 3.0,
    ("danger", "dangerSurface"): 4.5,
    ("success", "successSurface"): 4.5,
}


class ValidationError(ValueError):
    """A token document or required asset violates the design contract."""


def _unique_object(pairs: list[tuple[str, Any]]) -> dict[str, Any]:
    result: dict[str, Any] = {}
    for key, value in pairs:
        if key in result:
            raise ValidationError(f"Duplicate JSON property: {key}")
        result[key] = value
    return result


def read_json(path: Path) -> Any:
    def reject_constant(value: str) -> None:
        raise ValidationError(f"Non-finite JSON number: {value}")

    return json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=_unique_object, parse_constant=reject_constant)


def _equal_json(left: Any, right: Any) -> bool:
    # Python's True == 1 must not make a JSON boolean pass a numeric const.
    if isinstance(left, bool) or isinstance(right, bool):
        return type(left) is type(right) and left == right
    return left == right


def _validate_schema(value: Any, schema: dict[str, Any], root: dict[str, Any], path: str = "$") -> None:
    if "$ref" in schema:
        reference = schema["$ref"]
        if not reference.startswith("#/"):
            raise ValidationError(f"Unsupported schema reference: {reference}")
        target = root
        for segment in reference[2:].split("/"):
            target = target[segment.replace("~1", "/").replace("~0", "~")]
        _validate_schema(value, target, root, path)
        return

    if "type" in schema and not TYPE_NAMES[schema["type"]](value):
        raise ValidationError(f"{path}: expected {schema['type']}")
    if "const" in schema and not _equal_json(value, schema["const"]):
        raise ValidationError(f"{path}: expected constant {schema['const']!r}")
    if "enum" in schema and not any(_equal_json(value, member) for member in schema["enum"]):
        raise ValidationError(f"{path}: expected one of {schema['enum']}")

    if isinstance(value, dict):
        missing = set(schema.get("required", [])) - value.keys()
        if missing:
            raise ValidationError(f"{path}: missing required properties {sorted(missing)}")
        properties = schema.get("properties", {})
        if schema.get("additionalProperties") is False:
            unexpected = value.keys() - properties.keys()
            if unexpected:
                raise ValidationError(f"{path}: unknown properties {sorted(unexpected)}")
        for key, child in value.items():
            if key in properties:
                _validate_schema(child, properties[key], root, f"{path}.{key}")
    elif isinstance(value, list):
        if len(value) < schema.get("minItems", 0):
            raise ValidationError(f"{path}: too few items")
        if "maxItems" in schema and len(value) > schema["maxItems"]:
            raise ValidationError(f"{path}: too many items")
        if schema.get("uniqueItems") and len({json.dumps(item, sort_keys=True) for item in value}) != len(value):
            raise ValidationError(f"{path}: duplicate items")
        for index, child in enumerate(value):
            if "items" in schema:
                _validate_schema(child, schema["items"], root, f"{path}[{index}]")
    elif isinstance(value, str):
        if len(value) < schema.get("minLength", 0):
            raise ValidationError(f"{path}: string is too short")
        if "pattern" in schema and re.search(schema["pattern"], value) is None:
            raise ValidationError(f"{path}: does not match {schema['pattern']}")
    elif isinstance(value, (int, float)) and not isinstance(value, bool):
        if isinstance(value, float) and not math.isfinite(value):
            raise ValidationError(f"{path}: number must be finite")
        for key, fails in (
            ("minimum", lambda bound: value < bound),
            ("maximum", lambda bound: value > bound),
            ("exclusiveMinimum", lambda bound: value <= bound),
        ):
            if key in schema and fails(schema[key]):
                raise ValidationError(f"{path}: violates {key} {schema[key]}")


def luminance(color: str) -> float:
    channels = [int(color[index:index + 2], 16) / 255 for index in (1, 3, 5)]
    linear = [channel / 12.92 if channel <= 0.04045 else ((channel + 0.055) / 1.055) ** 2.4 for channel in channels]
    return sum(channel * weight for channel, weight in zip(linear, (0.2126, 0.7152, 0.0722)))


def contrast_ratio(foreground: str, background: str) -> float:
    lower, upper = sorted((luminance(foreground), luminance(background)))
    return (upper + 0.05) / (lower + 0.05)


def _asset(base: Path, relative: str) -> Path:
    name = Path(relative)
    if name.is_absolute() or ".." in name.parts:
        raise ValidationError(f"Asset path must stay inside the token directory: {relative}")
    target = (base / name).resolve()
    if not target.is_relative_to(base.resolve()):
        raise ValidationError(f"Asset path escapes the token directory: {relative}")
    if not target.is_file():
        raise ValidationError(f"Missing asset: {relative}")
    return target


def validate_tokens(data: dict[str, Any], base_dir: Path = DESIGN_DIR, *, check_assets: bool = True) -> dict[str, Any]:
    schema = read_json(SCHEMA_PATH)
    _validate_schema(data, schema, schema)
    problems: list[str] = []
    colors = data["colors"]
    ratios: dict[str, float] = {}
    observed: dict[tuple[str, str], float] = {}
    for pair in data["contrastPairs"]:
        foreground, background = pair["foreground"], pair["background"]
        key = (foreground, background)
        if key in observed:
            problems.append(f"Duplicate contrast pair: {foreground}/{background}")
        observed[key] = pair["minimum"]
        if foreground not in colors or background not in colors:
            problems.append(f"Unknown contrast color: {foreground}/{background}")
            continue
        ratio = contrast_ratio(colors[foreground], colors[background])
        ratios[f"{foreground}/{background}"] = round(ratio, 4)
        if ratio + 1e-9 < pair["minimum"]:
            problems.append(f"Contrast {foreground}/{background}: {ratio:.2f}:1 is below {pair['minimum']}:1")
    for key, minimum in REQUIRED_CONTRAST.items():
        if key not in observed:
            problems.append(f"Missing required contrast pair: {'/'.join(key)}")
        elif observed[key] < minimum:
            problems.append(f"Required contrast {'/'.join(key)} minimum cannot be below {minimum}:1")

    for label, values in (
        ("spacing", [data["spacing"][name] for name in ("xs", "sm", "md", "lg", "xl")]),
        ("typography.sizes", [data["typography"]["sizes"][name] for name in ("caption", "body", "section", "title")]),
    ):
        if any(left >= right for left, right in zip(values, values[1:])):
            problems.append(f"{label} must be strictly increasing")

    metrics, motion = data["metrics"], data["motion"]
    if data["radii"]["control"] > metrics["compactControlHeight"] / 2:
        problems.append("Control radius exceeds half the compact control height")
    if metrics["compactControlHeight"] > metrics["controlHeight"]:
        problems.append("Compact control height exceeds regular control height")
    if metrics["toggleWidth"] <= metrics["toggleHeight"]:
        problems.append("Toggle width must exceed toggle height")
    if metrics["sliderTrackHeight"] >= metrics["compactControlHeight"]:
        problems.append("Slider track must be thinner than its interactive control")
    if not metrics["minViewportWidth"] < metrics["sidebarBreakpoint"] <= metrics["maxContentWidth"]:
        problems.append("Sidebar breakpoint must lie above minimum viewport width and within max content width")
    if metrics["minViewportWidth"] <= 2 * metrics["safeInset"] + metrics["controlHeight"]:
        problems.append("Safe insets leave no usable minimum-width content")
    for stem in ("screen", "popup", "toast"):
        if motion[f"{stem}Close"]["durationMs"] > motion[f"{stem}Open"]["durationMs"]:
            problems.append(f"{stem} closing duration must not exceed opening duration")
    if motion["press"]["durationMs"] > motion["release"]["durationMs"]:
        problems.append("Press feedback must not be slower than release feedback")

    if problems:
        raise ValidationError("\n".join(problems))

    if check_assets:
        font = data["typography"]["font"]
        for file_key, hash_key in (("file", "sha256"), ("licenseFile", "licenseSha256")):
            asset = _asset(base_dir, font[file_key])
            digest = hashlib.sha256(asset.read_bytes()).hexdigest()
            if digest != font[hash_key]:
                raise ValidationError(f"Asset checksum mismatch: {font[file_key]}")
        signature = _asset(base_dir, font["file"]).read_bytes()[:4]
        if signature != b"\x00\x01\x00\x00":
            raise ValidationError("Font asset must be the declared static TrueType font")
        if "SIL OPEN FONT LICENSE Version 1.1" not in _asset(base_dir, font["licenseFile"]).read_text(encoding="utf-8"):
            raise ValidationError("Font notice must retain the SIL Open Font License 1.1 text")

    return {"name": data["name"], "schemaVersion": data["schemaVersion"], "contrastRatios": ratios}


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("tokens", type=Path, nargs="?", default=DESIGN_DIR / "tokens.json")
    parser.add_argument("--verbose", action="store_true", help="Print the measured contrast ratios")
    args = parser.parse_args(argv)
    try:
        result = validate_tokens(read_json(args.tokens), args.tokens.resolve().parent)
    except (ValidationError, OSError, json.JSONDecodeError) as error:
        print(f"Design validation failed: {error}", file=sys.stderr)
        return 1
    print(f"Validated {result['name']} tokens v{result['schemaVersion']}: {len(result['contrastRatios'])} contrast pairs and font/license checksums.")
    if args.verbose:
        for pair, ratio in result["contrastRatios"].items():
            print(f"  {pair}: {ratio:.2f}:1")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
