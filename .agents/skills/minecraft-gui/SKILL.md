---
name: minecraft-gui
description: Design, implement, animate, and review smooth Minecraft Java client interfaces using the project's actual stack, shared tokens, reusable ModernUI components, and a version-pinned source starter. Use for client or mod menus, settings screens, dialogs, controls, tooltips, and toasts.
---

# Minecraft GUI

Use this as the entry point for the complete UI workflow. Read supporting guides
when their task becomes relevant; keep implementation decisions in the consuming
project rather than relying on conversation memory.

## Start in the consuming project

1. Read the project's agent rules and the user's request. Inspect its Gradle
   files, Minecraft version, loader, mappings, Java toolchain, existing renderer,
   shared UI components, and settings/navigation APIs. Existing build files are
   authoritative, including when the toolkit import record is out of date.
2. Inspect [the packaged source map](references/source-map.md),
   [compatibility metadata](references/compatibility.json), and the actual
   [version pins](templates/starter/gradle.properties). The bundled starter is for
   Minecraft 26.1.2, Fabric, and Java 25. Do not assume it runs on another stack.
3. For a new project, use the starter as a reproducible baseline. For an existing
   supported project, import only the shared source/resources and required build
   wiring, retaining its namespace and behavior. Do not replace its build files
   wholesale. Establish a working adapter before claiming a new stack works.
4. Read the relevant workflow below and carry the request through implementation
   and verification. Ask for input only when a missing decision or access is
   necessary and cannot be resolved from project evidence.

## Choose the relevant workflows

| Task | Read | Result |
| --- | --- | --- |
| New screen or visual redesign | [Design](references/design.md) | Clear hierarchy, layout, component states, shared theme choices. |
| Build or change a screen | [Implementation](references/implementation.md) | Actual Java components wired to real settings/actions. |
| Hover, press, opening, closing, popup, scrolling | [Motion](references/motion.md) | Responsive elapsed-time behavior with interruption and reduced motion. |
| Check quality or diagnose broken UI | [Review](references/review.md) | Reproducible findings, root causes, fixes, and observed evidence. |

For a complete screen, use all four in that order. For a focused change, inspect
the necessary references and still verify the affected behavior. These are
instructions for the current coding agent; no separate agent runtime is needed.

## Shared rules

- Reuse the project's shared theme and components. Start a new theme from the
  packaged [tokens](templates/starter/design/tokens.json) and
  [component specifications](templates/starter/design/component-specs/README.md). Keep project
  overrides separate from the installed package so upgrades preserve decisions.
- Use anti-aliased shapes and readable proportional text. Bundle licensed fonts,
  measure with the same renderer that draws them, and retain fallback support.
- Use the renderer's supported component, animation, input, and lifecycle APIs.
  Keep GPU creation out of per-frame drawing; dispose resources correctly.
- Base UI motion on monotonic elapsed time. Keep it independent of the game tick
  and render FPS, retarget interruptions from the displayed value, and implement
  reduced motion alongside the default treatment.
- Keep visual and input transforms aligned. Add keyboard focus, activation,
  Escape/modal behavior, disabled states, and translated-label handling.
- Read the selected version's real source/API before adapting code. Never mix
  mappings, shader pipelines, or screen lifecycle methods from different versions.
- Use focused tests, fix the root cause of failures, then rerun affected checks.
  Compile success alone does not prove font quality or animation smoothness.

## Finish

Report what changed, how the controls connect to real project behavior, the
tested dependency stack, checks performed, and any unverified runtime cases.
State actual in-game observations separately from automated test results. A
desktop mock or screenshot is not proof of Minecraft integration.

For continued work, use the same project components and theme. Do not regenerate
independent buttons, fonts, and timings for each screen or edit installed
templates as a substitute for the consuming project's owned source code.
