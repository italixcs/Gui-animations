"""Regression tests for design constraints that affect shipped UI quality."""

from copy import deepcopy
import importlib.util
from pathlib import Path
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location("design_validate", ROOT / "design" / "validate.py")
VALIDATOR = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(VALIDATOR)


class TokensTest(unittest.TestCase):
    def setUp(self):
        self.tokens = VALIDATOR.read_json(ROOT / "design" / "tokens.json")

    def reject(self, tokens, message):
        with self.assertRaisesRegex(VALIDATOR.ValidationError, message):
            VALIDATOR.validate_tokens(tokens, check_assets=False)

    def test_canonical_palette_and_bundled_font_are_valid(self):
        report = VALIDATOR.validate_tokens(self.tokens)
        self.assertEqual(13, len(report["contrastRatios"]))
        self.assertGreaterEqual(report["contrastRatios"]["textOnAccent/accentPressed"], 4.5)

    def test_standard_srgb_contrast_reference(self):
        self.assertAlmostEqual(21, VALIDATOR.contrast_ratio("#000000", "#FFFFFF"))
        self.assertAlmostEqual(1, VALIDATOR.contrast_ratio("#8BB6FF", "#8BB6FF"))
        self.assertAlmostEqual(4.478089, VALIDATOR.contrast_ratio("#777777", "#FFFFFF"), places=5)

    def test_low_contrast_pressed_state_is_rejected(self):
        self.tokens["colors"]["textOnAccent"] = "#8BB6FF"
        self.reject(self.tokens, "Contrast textOnAccent/accent")

    def test_required_pairs_cannot_be_removed_or_weakened(self):
        missing = deepcopy(self.tokens)
        missing["contrastPairs"].pop(0)
        self.reject(missing, "Missing required contrast pair")
        self.tokens["contrastPairs"][0]["minimum"] = 3
        self.reject(self.tokens, "minimum cannot be below 4.5")

    def test_misspelled_color_reference_is_rejected(self):
        self.tokens["contrastPairs"][0]["foreground"] = "textPrimray"
        self.reject(self.tokens, "Unknown contrast color")

    def test_motion_schema_rejects_unknown_easing_and_spatial_reduced_motion(self):
        easing = deepcopy(self.tokens)
        easing["motion"]["hover"]["easing"] = "bouncy"
        self.reject(easing, "expected one of")
        self.tokens["motion"]["reducedMotion"]["spatialMotion"] = True
        self.reject(self.tokens, "expected constant False")

    def test_reduced_motion_and_duration_bounds_are_enforced(self):
        self.tokens["motion"]["reducedMotion"]["durationMs"] = 81
        self.reject(self.tokens, "maximum 80")
        self.tokens["motion"]["reducedMotion"]["durationMs"] = 0
        self.tokens["motion"]["screenOpen"]["durationMs"] = -1
        self.reject(self.tokens, "minimum 0")

    def test_extreme_numeric_values_fail_as_validation_errors(self):
        self.tokens["metrics"]["controlHeight"] = 10 ** 400
        self.reject(self.tokens, "maximum 4096")
        self.tokens["metrics"]["controlHeight"] = float("inf")
        self.reject(self.tokens, "expected number")

    def test_close_and_press_timings_keep_responsive_order(self):
        closing = deepcopy(self.tokens)
        closing["motion"]["screenClose"]["durationMs"] = 900
        self.reject(closing, "closing duration must not exceed")
        self.tokens["motion"]["press"]["durationMs"] = 120
        self.reject(self.tokens, "Press feedback")

    def test_schema_rejects_missing_unknown_and_wrong_typed_tokens(self):
        missing = deepcopy(self.tokens)
        del missing["colors"]["focus"]
        self.reject(missing, "missing required properties")
        unknown = deepcopy(self.tokens)
        unknown["metrics"]["unknownMetric"] = 12
        self.reject(unknown, "unknown properties")
        self.tokens["motion"]["press"]["durationMs"] = True
        self.reject(self.tokens, "expected integer")

    def test_invalid_geometry_and_non_monotonic_type_scale_are_rejected(self):
        geometry = deepcopy(self.tokens)
        geometry["radii"]["control"] = 100
        self.reject(geometry, "Control radius")
        self.tokens["typography"]["sizes"]["title"] = 11
        self.reject(self.tokens, "typography.sizes must be strictly increasing")

    def test_font_asset_checksum_detects_modified_or_missing_font(self):
        with tempfile.TemporaryDirectory() as directory:
            base = Path(directory)
            (base / "fonts").mkdir()
            (base / "fonts" / "Inter-Regular.ttf").write_bytes(b"changed font")
            with self.assertRaisesRegex(VALIDATOR.ValidationError, "Asset checksum mismatch"):
                VALIDATOR.validate_tokens(self.tokens, base)
            (base / "fonts" / "Inter-Regular.ttf").unlink()
            with self.assertRaisesRegex(VALIDATOR.ValidationError, "Missing asset"):
                VALIDATOR.validate_tokens(self.tokens, base)

    def test_asset_paths_cannot_escape_token_directory(self):
        self.tokens["typography"]["font"]["file"] = "../outside.ttf"
        with self.assertRaisesRegex(VALIDATOR.ValidationError, "must stay inside"):
            VALIDATOR.validate_tokens(self.tokens)

    def test_json_reader_rejects_ambiguous_or_nonfinite_data(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "bad.json"
            path.write_text('{"color":1,"color":2}', encoding="utf-8")
            with self.assertRaisesRegex(VALIDATOR.ValidationError, "Duplicate JSON"):
                VALIDATOR.read_json(path)
            path.write_text('{"duration":NaN}', encoding="utf-8")
            with self.assertRaisesRegex(VALIDATOR.ValidationError, "Non-finite JSON"):
                VALIDATOR.read_json(path)


if __name__ == "__main__":
    unittest.main()
