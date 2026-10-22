# Project plan

## Idea

Make `Gui-animations` the repository an AI agent can consult or install skills
from when asked to build a polished Minecraft interface. Pair design guidance
with small, working reference implementations so the agent can produce a
consistent result and verify it in the actual game.

The repository should answer three questions:

1. What should a good screen look and feel like?
2. How do I implement it on this Minecraft stack?
3. How do I prove the result is usable, smooth, and compatible?

## Confirmed direction and proposed choices

| Decision | Status | Reason |
| --- | --- | --- |
| Use `italixcs/Gui-animations` | Confirmed by the user | The existing repo is the project home. |
| Minecraft Java client mods | Confirmed by the user | Provides control over shapes, fonts, rendering, and input. |
| Clean typography and smooth interactions | Confirmed by the user | The project's defining visual goal. |
| Fabric as the first showcase loader | Proposed | Start with one reproducible integration. |
| ModernUI as the first renderer | Leading candidate; verify in spike | Strongest researched fit for smooth text and rich UI rendering. |
| Java reference code | Proposed | Accessible to Minecraft mod authors; keep consumer integration explicit. |
| One exact Minecraft target initially | Proposed | Prevent a collection of snippets that only work on unspecified versions. |

The instructions and design principles can help projects on other stacks from
the start. Runnable adapters must list only combinations actually tested.

## Product boundaries

This is an agent toolkit and reference implementation. It should support
settings screens, menus, dialogs, tooltips, toasts, and later HUD widgets.
It should not replace the whole Minecraft UI, become a general game engine,
or introduce its own font rasterizer in the first release.

The first release will not include Bedrock support, server-only interfaces,
every mod loader, a visual editor, or a browser runtime embedded in Minecraft.
Inventory and container screens are a later milestone because slot interaction
and server synchronization add requirements beyond a settings screen.

## Technical direction

Use an existing rendering system for anti-aliased shapes and text, then add the
project's design conventions, reusable components, and agent workflow on top.

ModernUI is the first candidate to validate. Its Minecraft integration describes
TrueType/OpenType support, SDF text, font fallback, and rich UI rendering. Its
framework also has an existing component and lifecycle model; reuse those
facilities instead of implementing a competing layout or animation engine by
default. See [research](RESEARCH.md) for tradeoffs and alternative references.

Keep component state and token definitions reusable. Isolate unavoidable
Minecraft-version and renderer code in the first adapter. Do not build several
adapters or a broad abstraction until a second real integration needs them.

```text
Agent instructions + skills
          |
Design tokens + component specifications
          |
Reference components + interaction / motion recipes
          |
One Minecraft / renderer adapter
          |
In-game showcase + verification evidence
```

## Proposed repository layout

Only README, AGENTS, and the documentation files exist in this planning revision.
The rest of this tree is the intended implementation structure.

```text
README.md
AGENTS.md
docs/
  PLAN.md
  RESEARCH.md
  DESIGN.md
  getting-started.md
  compatibility.md
  verification.md
.agents/skills/
  minecraft-ui-design/SKILL.md
  minecraft-ui-implement/SKILL.md
  minecraft-ui-motion/SKILL.md
  minecraft-ui-review/SKILL.md
design/
  tokens.json
  component-specs/
reference/
  components/
  motion/
adapters/
  fabric-modernui/
examples/
  showcase-mod/
  prompts/
tests/
.github/workflows/
```

Use standard `SKILL.md` frontmatter with a name and description, concise task
steps, and links to supporting references. Keep each skill self-contained enough
to copy into another repository. Document installation paths for supported
agents; do not assume every agent automatically discovers `.agents/skills`.
Add tool-specific adapters only where needed, pointing back to canonical rules.

## Planned skills

| Skill | Takes in | Produces |
| --- | --- | --- |
| `minecraft-ui-design` | Screen purpose, target stack, constraints | Layout, hierarchy, component states, token choices, motion specification. |
| `minecraft-ui-implement` | Design plus exact project versions | Working screen using existing components and the project's actual APIs. |
| `minecraft-ui-motion` | Interaction and lifecycle requirements | Interruptible transitions, elapsed-time behavior, reduced-motion treatment. |
| `minecraft-ui-review` | Implementation plus in-game evidence | Specific findings about typography, layout, input, animation, and performance. |

Include sample prompts such as: "Build a settings screen using this project's
existing stack and the Gui-animations design rules. Include all input states,
a confirmation dialog, and reduced motion. Show in-game verification evidence."

## First showcase

Build a compact settings screen with a navigation area and scrollable content.
At narrow viewports, collapse the side navigation into a compact selector.
The screen should exercise:

- A readable sans-serif title, body, labels, and helper text.
- Rounded panels and controls with crisp edges.
- Buttons, toggle, slider, tooltip, confirmation dialog, and toast.
- Opening and closing transitions plus immediate click feedback.
- Hover, focus, pressed, disabled, and error states where applicable.
- Tab/Shift+Tab navigation, keyboard activation, and sensible Escape handling.
- A reduced-motion setting that preserves clear state feedback.

Use the [design direction](DESIGN.md) as the visual contract. Show working state
changes rather than decorative controls. Persist real showcase settings locally;
do not imply the demo controls unrelated Minecraft settings.

## Build order

### 0. Research and blueprint

- [x] Inspect the existing repository and license.
- [x] Confirm Minecraft edition and client-side scope.
- [x] Compare relevant libraries and inspect visual references.
- [x] Record design principles, architecture, and acceptance criteria.

### 1. Compatibility spike

- [ ] Pick one Minecraft release supported by the chosen loader and renderer.
- [ ] Resolve and pin Minecraft, loader, mappings, JDK, build plugin, renderer,
      and required companion dependencies to exact versions.
- [ ] Verify those artifacts resolve and produce a reproducible build with a
      committed build wrapper and checksums.
- [ ] Open a screen in game with one anti-aliased rounded panel and sans-serif
      text, using the renderer's supported host integration.
- [ ] Verify mouse coordinates, keyboard input, GUI scaling, resource reload,
      screen disposal, and re-opening.
- [ ] Record the working version matrix and a screenshot before scaling up.

Start by evaluating the 26.1.x combination listed by ModernUI's current README.
That README also lists older versions, including 1.21.1, as legacy. These are
candidate families, not verified dependency pins or a claim of latest support.
If there is a required modpack version, it should determine the spike target.

**Exit criterion:** one screen builds and runs in Minecraft on a documented,
exact stack. If this fails, evaluate another renderer before writing the toolkit
against an assumed API. A desktop-only demo does not satisfy this milestone.

### 2. Design system and agent pack

- [ ] Encode the agreed design and motion values in `design/tokens.json`.
- [ ] Define the mapping between design units, logical GUI coordinates, and
      device-space font sizes for the selected renderer.
- [ ] Document component states and responsive layout rules.
- [ ] Implement the four planned skills and installation instructions.
- [ ] Add reusable prompt examples and a short end-to-end agent walkthrough.

**Exit criterion:** an agent can discover the rules, identify its target stack,
and produce a concrete screen specification without inventing UI conventions.

### 3. Components and motion recipes

- [ ] Implement panel, button, toggle, slider, tooltip, modal, and toast using
      the validated renderer's existing primitives where possible.
- [ ] Add elapsed-time transition recipes and cancellation / retargeting rules.
- [ ] Keep input coordinates aligned with displayed transforms and clipping.
- [ ] Implement keyboard focus, modal input capture and return, and reduced motion.
- [ ] Add focused tests for timing, interruption, action dispatch, and lifecycle.

**Exit criterion:** components work independently and handle repeated, rapid,
interrupted interaction correctly. A click produces its action exactly once.

### 4. Showcase and verification

- [ ] Assemble the settings screen from the shared components and tokens.
- [ ] Verify at 30, 60, and 144 FPS caps, several GUI scales, and small / large
      logical viewports. Record observed timing and any limits.
- [ ] Test high-DPI displays, Unicode fallback, long translated labels, keyboard
      operation, reduced motion, and repeated screen open / close cycles.
- [ ] Test resize, resource reload, returning to the parent, disconnecting from
      a world, and transitions interrupted by another screen.
- [ ] Capture screenshots and a short video in the actual game.
- [ ] Record frame-time measurements, hardware, versions, and blur on/off results.
- [ ] Add CI for reproducible compilation, focused tests, and token validation.

**Exit criterion:** users can build and launch the showcase from documented
commands, and an agent can reproduce its style in a second small screen.

### 5. Extend after the first release

Evaluate additional screen recipes, HUD widgets, inventory integration, themes,
and a second loader only after the first workflow is proven. Each added adapter
gets its own tested compatibility entry and evidence.

## Definition of done for the first release

An agent can take a screen request, identify the supported stack, apply a skill,
reuse the components, build the project, and demonstrate the result in game.
Text remains readable across the tested scales; shapes are anti-aliased; motion
is consistent across frame rates; keyboard and mouse input agree with the visual
layout; reduced motion works; and all distributed assets have attribution.
