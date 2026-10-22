# Implementation workflow

## Select working source for the actual stack

Inspect the consuming project's Gradle and client entry points before editing.
The [source map](source-map.md) leads to a complete source starter with pinned
dependencies. Read its real source, resources, and module build files; use
version-matched APIs rather than treating snippets as generic Minecraft code.

For a new standalone mod project, the installation command's `--starter` option
imports the complete reference build. It keeps the validated demo namespace.
Adapt the Java packages, mod ID, resource identifiers, and metadata together if
renaming it. Keep font/license notices and required renderer dependencies.

For an existing mod:

1. Check the target against [compatibility metadata](compatibility.json).
2. Identify the existing settings model, save path, screen-opening entry point,
   renderer dependencies, and module boundaries.
3. Reuse already integrated core/adapter components. Otherwise copy the shared
   source and resources into the project's namespace once, preserving their
   dependencies and licenses. Adapt the demonstrated Gradle wiring to its build;
   do not overwrite working build files with the demo's root configuration.
4. Add the screen composition and real bindings to the project's client source.
   Avoid creating another implementation of a shared control for each screen.

The exact 26.1.2 / ModernUI 3.13.0.5 starter includes a host render-state bridge
and `guianimations.mixins.json`. Its two mixins restore Minecraft's cached
program/pipeline state after ModernUI's external OpenGL drawing. Include this
host wiring when adapting that exact integration, along with its metadata;
copying only the fragment can leave the compositor using a stale GL program.
Recheck the need and injection targets before using it on another version.

An unsupported Minecraft or renderer version requires a small adapter proof:
one screen, text, rounded shape, input, scaling, and resource lifecycle. Record
the exact dependency stack and evidence. Do not upgrade an existing project's
Minecraft version just to make a template compile.

## Connect behavior

Use renderer-native layout and controls where possible. Separate shared theme
and interaction logic from screen-specific settings and navigation. A toggle
must change the intended setting; a slider needs a real range and a keyboard
path; saving must report actual success/failure.

Implement modal focus capture, Escape order, input blocking behind a modal,
and focus return when it closes. Keep outside-click behavior deliberate. During
animation, visual transforms, hitboxes, clipping, popup anchoring, and tooltips
must use compatible coordinates.

Load and measure the bundled font with the renderer that draws it. Retain its
notice. Do not allocate GPU resources or load fonts in a per-frame method. Use
the host lifecycle for thread ownership, screen replacement, reload, resize,
and disposal. Cancel callbacks when their screen no longer owns the interaction.

## Verify the integration

Use the consuming project's documented wrapper and JDK. The starter uses Java 25:

```sh
./gradlew build
./gradlew :examples:showcase-mod:runClient
```

Use the actual module tasks for other projects. Fix the root cause of build or
test failures, then rerun the affected checks. Complete the
[motion](motion.md) and [review](review.md) workflows before reporting a finished
screen. Record remaining client checks explicitly if the environment cannot run
Minecraft; do not present a successful compilation as runtime verification.
