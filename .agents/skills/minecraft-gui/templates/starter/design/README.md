# Shared design system

[tokens.json](tokens.json) is the canonical machine-readable design contract.
The Minecraft adapter copies it into generated resources and reads the values
at runtime; the skill's starter includes the same snapshot. Do not maintain a
second handwritten palette or timing table in Java.

Validate the schema, color contrast, scale relationships, motion bounds, and
actual font/license checksums with Python 3.10 or newer:

```sh
python3 design/validate.py --verbose
python3 -m unittest discover -s tests/design -v
```

The validator uses only Python's standard library. It reads the JSON Schema
keywords used by [tokens.schema.json](tokens.schema.json) and adds cross-field
checks such as contrast pairs, responsive breakpoints, and font provenance.
Font paths are relative to the token file's directory and must stay inside it.

## Coordinate contract

Tokens describe logical design and type units, not framebuffer pixels. For the
Fabric/ModernUI adapter, one length unit maps to one ModernUI `dp` and one type
unit maps to one `sp`. Convert lengths with the host context's density helpers;
let the text API apply scaled density. The host renderer controls physical
pixel density. Do not multiply by Minecraft's GUI scale a second time.

Measure text with the loaded typeface and renderer. The packaged
[Inter font](fonts/README.md) provides the normal sans-serif style; leave the
renderer fallback active for scripts it does not contain. Geometry/text quality
and fallback coverage still require in-game checks on supported displays.

## Opaque surfaces and state colors

Base surfaces are opaque to keep contrast predictable. The scrim is the only
default translucent effect and belongs behind the panels. Blur and shadow are
off by default. Changing opacity or introducing texture behind text requires
checking contrast against the final composited result.

Text contrast is measured against every required surface/state, including the
pressed primary button and disabled labels. The focus color is measured against
a dark surface. An outline around a bright accent control needs a dark gap
before the focus ring so the ring remains distinguishable; see the
[button specification](component-specs/button.md).

## Component contracts

Use the [component catalog](component-specs/README.md) for states, input,
motion, accessibility, and layout rules. These contracts describe intended
behavior. The compatibility/verification documents identify which behaviors
were actually exercised in the current runtime; a specification is not a test
result.
