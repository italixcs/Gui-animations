# Examples and research

Research date: 2026-10-22. These findings come from the linked repositories and
their documentation. No referenced library was built or benchmarked in Minecraft
during this planning pass. Recommendations are distinguished from upstream claims.

## Existing project

[`italixcs/Gui-animations`](https://github.com/italixcs/Gui-animations) initially
contained a README title and an MIT license. Its existing purpose and licensing
are compatible with starting an original agent toolkit here.

## Comparison

| Reference | What the source shows | How it helps this project | Tradeoff |
| --- | --- | --- | --- |
| [ModernUI-MC](https://github.com/BloCamLimb/ModernUI-MC) | Minecraft integration for ModernUI; a text engine with TTF/OTF support, anti-aliasing, improved SDF rendering, fallback, and configurable UI effects. | Leading candidate for the desired clean text and rich screen rendering. | A substantial runtime with host/version integration to validate; LGPL-3.0-or-later. |
| [owo-lib / owo-ui](https://github.com/wisp-forest/owo-lib) | Declarative UI library for Fabric, with detailed docs and source for animation easing. | Reference for ergonomic screen construction and readable component examples. | Layout ergonomics alone do not establish the desired font and edge quality; MIT. |
| [Elementa](https://github.com/SparkUniverse/Elementa) | Declarative component tree, constraints, events, clipping effects, and animated constraints; host bindings through UniversalCraft. | Reference for component APIs, lifecycle, input, and animation composition. | Kotlin-oriented examples and version-specific UniversalCraft bindings; LGPL v3 license. |
| [PolyUI JVM reference](https://github.com/nextdayy/polyui-jvm) | Documentation separates UI logic from a renderer implementation and describes redraw-on-demand and transitions. | Useful architecture example for renderer separation and caching. | Treat as an architectural reference; current maintenance and target compatibility were not established. |
| [FancyMenu](https://github.com/Keksuccino/FancyMenu) | Minecraft menu customization project; its README identifies a custom DSMSLv3.1 license. | Related product category to inspect for menu customization ideas. | Do not assume a permissive license or copy its implementation. |

The old `EssentialGG/Elementa` URL redirects to `SparkUniverse/Elementa`.
The `Polyfrost/PolyUI` URL currently resolves to `Polyfrost/PolyIO`, a different
project; use the explicit JVM reference above when discussing the inspected UI
architecture. Repository names are not enough to establish an API or dependency.

## Visual examples

### ModernUI settings screen

[View the settings screenshot](https://cdn.modrinth.com/data/3sjzyvGR/images/2571f7372b1f9bbb116c118f29a93255338f4e41.png)

The inspected screenshot shows readable proportional text, curved toggles,
rounded settings groups, a navigation sidebar, and a contextual popup over a
Minecraft scene. It is a close visual example of the user's requested direction.
Our first showcase should use its own simpler layout, tokens, and implementation.

### ModernUI text comparison

[View the text-rendering comparison](https://cdn.modrinth.com/data/3sjzyvGR/images/d27f5d77555fd3f45392f5b8eb28efcb80f0b677.png)

The inspected comparison illustrates the difference between blocky game text
and smoother proportional text. It includes upstream FPS figures from a
specific scene; these are not our measurements or a performance guarantee.

Both images are linked by the [ModernUI-MC README](https://github.com/BloCamLimb/ModernUI-MC/blob/master/README.md),
which explicitly labels its screenshot section as outdated. They establish a
visual reference, not verified current appearance. Links preserve attribution;
the images are not redistributed as assets of this project.

### Elementa component examples

The [Elementa README](https://github.com/SparkUniverse/Elementa/blob/master/README.md)
contains a component showcase, constraint examples, event handling, and animation
examples. Use these to study a readable component API. Consult the exact version's
source before adapting an implementation.

## Recommended foundation

Validate Fabric plus ModernUI first. The strongest evidence is the match between
the requested text quality and ModernUI's documented text-rendering facilities,
alongside an existing Minecraft host integration.

This is a candidate selection, not a verified build combination. The ModernUI
core README describes the framework as intended for advanced UI scenarios rather
than turnkey standalone apps, and its linked documentation repository returned
404 during research. The linked Javadocs endpoint also returned 403 to an
automated availability check. Expect to consult version-matched source and
confirm documentation access during the spike.

If the compatibility spike cannot produce a straightforward reproducible
integration, evaluate a Fabric-native UI approach. Recheck font rendering,
anti-aliasing, input, and performance instead of assuming another library will
meet the visual goal automatically.

## Version-specific findings

- The inspected ModernUI-MC README lists 26.1–26.1.2 as mainline and 1.21.1 and
  several other older families as legacy. Do not translate this into a claim
  that every Minecraft release or loader combination is supported.
- Its development instructions require its Maven repository and distinguish
  development artifacts from mod distribution downloads. Resolve the exact
  required artifacts before publishing build instructions.
- [Fabric custom-screen docs](https://docs.fabricmc.net/develop/rendering/gui/custom-screens)
  explain lifecycle initialization and per-frame rendering. The current docs
  use APIs that differ from older releases, so copying current snippets into an
  old-version project is unsafe.
- [Fabric rendering concepts](https://docs.fabricmc.net/develop/rendering/basic-concepts)
  document render pipeline and matrix changes. Version/mapping-specific adapters
  are necessary; `render`-style and newer extraction-style examples must not be
  mixed into a single unverified recipe.

## Source list

1. [ModernUI-MC README and compatibility matrix](https://github.com/BloCamLimb/ModernUI-MC/blob/master/README.md)
2. [ModernUI core README](https://github.com/BloCamLimb/ModernUI/blob/master/README.md)
3. [ModernUI Javadocs](https://docs.icyllis.dev/api/modernui/index.html) — linked by upstream; returned 403 to the availability check.
4. [owo-lib README](https://github.com/wisp-forest/owo-lib/blob/HEAD/README.md)
5. [owo-ui documentation](https://docs.wispforest.io/owo/ui/) — linked by upstream; inspect during implementation.
6. [owo easing source](https://github.com/wisp-forest/owo-lib/blob/HEAD/src/main/java/io/wispforest/owo/ui/core/Easing.java)
7. [owo-lib MIT license](https://github.com/wisp-forest/owo-lib/blob/HEAD/LICENSE)
8. [Elementa README](https://github.com/SparkUniverse/Elementa/blob/master/README.md)
9. [Elementa license](https://github.com/SparkUniverse/Elementa/blob/master/LICENSE)
10. [PolyUI JVM README](https://github.com/nextdayy/polyui-jvm/blob/master/README.md)
11. [FancyMenu README](https://github.com/Keksuccino/FancyMenu/blob/HEAD/README.md)
12. [Fabric custom screens](https://docs.fabricmc.net/develop/rendering/gui/custom-screens)
13. [Fabric rendering concepts](https://docs.fabricmc.net/develop/rendering/basic-concepts)

## Attribution and reuse

Keep this repository's original work MIT licensed. External dependencies retain
their own terms. Referencing an LGPL library is different from copying or
redistributing its source or binaries; document the actual distribution model and
retain required notices when an adapter is packaged. Font assets need their own
license files. Restricted reference implementations are inspiration only.
