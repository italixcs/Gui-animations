# Gui-animations

An AI agent toolkit for building smooth, modern Minecraft Java interfaces.

The goal: clean sans-serif text, anti-aliased shapes, consistent spacing, and
responsive opening, closing, click, and popup animations inside Minecraft.

**Status: researched blueprint.** This revision defines the idea and build plan.
It does not contain a GUI library, installable mod, or tested Minecraft adapter yet.

## Start here

- [Build plan](docs/PLAN.md): scope, architecture, repository layout, and milestones.
- [Using it in your client or mod](docs/USAGE.md): one installation, one main skill, and reusable UI code.
- [Examples and research](docs/RESEARCH.md): projects worth studying and their tradeoffs.
- [Design direction](docs/DESIGN.md): typography, components, motion, and quality standards.
- [Agent instructions](AGENTS.md): how to work in this repository.

## What this repository will provide

| Part | Purpose |
| --- | --- |
| Agent instructions | Give coding agents an agreed workflow and clear constraints. |
| One main skill | Guides the agent through design, implementation, motion, and review as one workflow. |
| Design tokens | Keep colors, spacing, typography, radii, and timings consistent. |
| Reusable Java recipes | Demonstrate working components and animation behavior on an explicitly supported stack. |
| In-game showcase | Let people and agents inspect the intended result in Minecraft. |
| Verification guides | Catch blurry fonts, broken input, inconsistent timing, and rendering regressions. |

## How you will use it

Install the versioned toolkit into your own client/mod repository, then ask your
coding agent: "Use minecraft-gui to build my settings screen."

The main skill will identify your stack, use the shared theme and components,
implement the screen, and guide verification in Minecraft. A short instruction
in your project's agent rules will make the toolkit the default for future UI
tasks. You will not have to choose and invoke each supporting workflow yourself.

The skill teaches the agent; Java components and the renderer run inside the
game. This installation flow is planned, not available yet. See the
[usage design](docs/USAGE.md) for the concrete project layout and examples.

## First build

One polished settings screen: a navigation area, scrollable settings, a button,
toggle, slider, confirmation popup, tooltip, and toast. It should demonstrate
opening and closing transitions, keyboard navigation, and reduced motion.

Java Edition client mods are the confirmed target. Fabric is the proposed first
loader. ModernUI is the leading renderer candidate; a small compatibility spike
will confirm it before we commit to a particular Minecraft/library version.

See the [milestone checklist](docs/PLAN.md#build-order) for the implementation order.

## License

Original repository content is MIT licensed. Referenced libraries, fonts, and
screenshots retain their own licenses and attribution; see [research](docs/RESEARCH.md).
