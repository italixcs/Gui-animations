# Using the toolkit in a client or mod

Gui-animations 0.1.0 provides a self-contained main skill, installer, shared
design system, Java components, and source starter. See
[getting started](getting-started.md) for commands and the
[verification report](verification-report.md) for observed coverage.

## One installation, one entry point

You work in your own Minecraft client/mod repository with your usual coding
agent. Install the versioned Gui-animations package there once, then ask the
agent to use **`minecraft-gui`** for UI work. The main skill selects the design,
implementation, motion, and review instructions needed for your request.

You do not need to tell the agent which guide to open for every change. The
main skill acts as the map of the toolkit, loading relevant references and
templates as needed. It does not require loading the entire repository into
the model's context on every task.

The package has two connected parts:

| Part | Used by | Job |
| --- | --- | --- |
| Main skill, guides, tokens, component catalog, templates | Your coding agent | Explain the workflow and provide verified patterns and source code. |
| Imported Java components, resources, and pinned renderer dependencies | Your mod in Minecraft | Draw the UI, handle input, and run animations. |

Installing a skill alone does not change Minecraft's rendering. The agent also
integrates the actual Java code and renderer for the supported target. Players
run the resulting mod and its required runtime dependencies; they do not need
the coding agent or skill files.

## First-time setup

The installer:

1. Copy a pinned release's self-contained main skill package to the selected
   agent's supported project-local skill location.
2. Add a short instruction to the consuming project's existing agent rules,
   preserving its other content.
3. Record the toolkit release and imported files so later tasks use a consistent
   source of truth and upgrades can identify local changes.

For Codex, installation produces this layout:

```text
my-client/
  AGENTS.md                          # Existing rules plus a toolkit instruction
  .agents/skills/minecraft-gui/
    SKILL.md                         # Main entry point
    references/                      # Design, implementation, motion, review
    templates/starter/design/        # Shared tokens, font, component specs
    templates/starter/               # Complete version-pinned source project
    manifest.json                    # Payload identities and file modes
  gui-toolkit.lock.json               # Toolkit release and import manifest
  ...existing Minecraft project files...
```

For Claude Code, the installer uses `.claude/skills/minecraft-gui` and
`CLAUDE.md` instead. Skill discovery differs between agents and editors; the
first prompt should explicitly name the main skill. The installer does not
assume automatic discovery in every tool.

The added project instruction should convey:

> For Minecraft GUI work, use the installed minecraft-gui skill. Reuse the
> project's shared UI theme and components, follow its design and motion rules,
> and verify the result against the target Minecraft stack.

The project's original rules and explicit user requirements still apply. A
toolkit instruction should be short and point to its installed entry point.

## What you ask the agent

For a new client interface:

> Use minecraft-gui to build my client's settings interface. I want a dark theme,
> a sidebar for categories, a search field, toggles, sliders, and a confirmation
> popup. Use clean sans-serif text and smooth opening and click animations.
> Connect the controls to my existing settings system.

For one mod screen:

> Use minecraft-gui to add a configuration screen to this mod. Follow the
> existing Minecraft version and loader. Reuse the shared UI components and
> include keyboard navigation and reduced motion.

For a later change:

> Add a Profiles tab using the existing UI theme. Animate profile selection and
> show a toast after saving. Use minecraft-gui.

The user describes the result; the agent resolves the implementation details
from the installed package and the consuming project's actual code.

## What the main skill makes the agent do

1. **Inspect the project.** Read build files, mappings, loader, Java version,
   existing UI, settings APIs, and the toolkit import record. Build files remain
   authoritative; do not trust a stale manifest over actual dependencies.
2. **Check compatibility.** Select a verified template matching that stack.
   For a new unsupported target, establish a working adapter before claiming
   support. Do not silently change Minecraft versions or replace an existing
   renderer to make a recipe fit.
3. **Set the visual rules.** Reuse the existing project theme or establish its
   initial font, colors, spacing, radii, and motion values. Keep project theme
   overrides separate from the package's default tokens.
4. **Build from shared code.** Integrate the required renderer dependencies and
   import the validated source components into the project's namespace once.
   Future screens use those same components instead of copying another button
   implementation into every screen. Keep font/resource namespaces and notices
   correct for the consuming project.
5. **Connect real behavior.** Bind controls to the client's or mod's settings,
   navigation, and actions. Add the required states, animations, keyboard focus,
   and reduced-motion behavior using the supporting workflows.
6. **Verify and report.** Compile, run relevant tests, and inspect the screen in
   Minecraft when a client is available. Record actual evidence and remaining
   checks; compilation alone is not evidence of appearance or smoothness.

The main skill is a set of instructions used by the existing coding agent. It
does not run a separate agent service, need an MCP server, or spawn other agents.

## Consistent use across the whole client

The first screen establishes shared theme and components in the consuming
project. Every later screen should use those same assets and classes. The
project-level rule points the agent back to the main skill across sessions;
the actual theme, component code, and build files preserve implementation
decisions without relying on conversational memory.

The `--starter` option imports versioned source templates. That gives projects
working code they can adapt in their own namespace without waiting for a
separate Gui-animations Maven library. A published runtime library can be
evaluated later if shared-code maintenance justifies it. The renderer and its
companion libraries still need real, pinned build dependencies.

Re-running the installer is idempotent. Upgrades compare the previous manifest
with local files and preserve project edits. Conflicting customized components,
fonts, or managed-rule sections cause a preflight failure before files change.
Use `--dry-run` to inspect the proposed changes.

## How we verify this workflow

The independent [consumer harness](../tests/verification/verify_consumer.py)
installs the package into separate project directories, verifies its payload and
local references, checks repeat installs and conflict handling, and can compile
the standalone starter from the installed package. The release report records
these results separately from actual Minecraft observations.

When evaluating an agent or extending the toolkit, also ask it to create a new
screen and add a second matching screen in a fresh session. Check discovery,
shared-code reuse, build success, and the intended in-game result. The skill is
guidance for the chosen coding agent; automatic compliance by every agent or
editor is not assumed. This consuming-project workflow is as important as the
toolkit's own showcase.
