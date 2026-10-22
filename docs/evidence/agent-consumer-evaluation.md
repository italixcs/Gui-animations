# Agent-consumption evaluation

This is an isolated consuming project installed with Gui-animations 0.1.0. All
implementation work after installation used this project's installed skill and
its own imported source; the original toolkit implementation was not consulted.

## Setup and discovery

Installed with:

```sh
python3 /workspace/Gui-animations/scripts/install.py --project /workspace/scratch/gui-agent-eval --agent codex --starter
```

The installer created 112 file changes. The project-local `AGENTS.md` pointed
to `.agents/skills/minecraft-gui/SKILL.md`. The entry skill resolved the design,
implementation, motion, review, source map, compatibility, and token references.
The installed package's payload SHA-256 was
`e7096327626eb50338f7a1787aedd496caa817d1537b5f49bbac85e1b3584138`.

## Second-screen contract and implementation

Implemented an informational **About this client** screen as a second screen
alongside the imported settings screen. Its purpose is to present build and UI
information with one real primary action: return to the previous Minecraft
screen.

- `adapters/fabric-modernui/src/main/java/dev/guianimations/modernui/AboutFragment.java`
  is consumer-owned screen composition.
- `examples/showcase-mod/src/main/java/dev/guianimations/showcase/ShowcaseClient.java`
  adds a real F9 route from the title screen or world to the About fragment.
  Existing F8 behavior still opens the settings showcase.
- Both screens reuse the exact imported `UiTheme`, `UiButton`, and `UiMotion`.
  No duplicate font, button, palette, easing engine, or resource namespace was
  created. The normal reference theme supplies Inter, text roles, panel radii,
  colors, safe insets, and animation durations.
- The header keeps Close outside the scrollable content. The body scrolls for
  smaller viewports and long labels. Width caps at the existing theme maximum;
  lengths use ModernUI dp and type sizes use its existing sp mapping.
- Close uses native keyboard/button semantics, requests initial focus, and has a
  tooltip. Escape uses the host's actual back dispatcher and the same close path.
  The host callback returns to the captured previous screen exactly once.
- Opening/closing reuse the elapsed-time motion recipe and the saved reduced
  motion setting. Lifecycle destruction cancels queued transitions and removes
  the back callback. A generation guard protects deferred initial focus/motion.

The About content is informational. It has no fake save or network actions and
does not pretend to own unrelated Minecraft settings.

## Checks performed

The installed project's actual pinned stack is Minecraft 26.1.2, Fabric Loader
0.19.2, Fabric API 0.149.0+26.1.2, ModernUI Fabric 3.13.0.5/core 3.13.0, JDK25,
Gradle9.4.0, and Loom1.16.3.

```sh
./gradlew build
python3 design/validate.py
python3 -m unittest discover -s tests/design -v
```

`build` passed in 13 seconds: the new About fragment and changed host route
compiled, all three module artifacts built, and all 34 Java tests passed with
zero failures/errors. The design validator passed its 13 contrast pairs plus
font/license integrity checks; all 14 design regression tests passed.

The environment supplied JDK25 at `/workspace/scratch/jdk25` and required its
standard command-local JVM proxy/CA options for dependency downloads. These
environment settings were not added to project source or build files.

The **installed copy** of `scripts/install.py` was then run against this
consumer project without `--starter`: it applied zero changes and preserved both
consumer-owned source files byte for byte. A `--starter --dry-run` correctly
rejected the modified host source as a local/unmanaged conflict, with no writes.
This demonstrates the documented separation between skill updates and owned
source adaptation.

## Limits

This agent did not launch another Minecraft client while the independent runtime
verifier owned the virtual display. The new screen's in-game appearance, F9
input path, focus behavior, and perceived animation smoothness remain unobserved.
Compilation and existing lifecycle tests are evidence of API compatibility and
reuse, not a claim of visual/runtime verification for this additional screen.
The original toolkit's showcase runtime is verified separately by the parent
task.

No original toolkit files were edited, committed, or pushed by this evaluation.
