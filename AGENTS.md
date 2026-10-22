# Agent instructions

## Project purpose

Help AI coding agents build clean, smooth Minecraft Java client interfaces using
documented design rules, reusable skills, working examples, and verified code.

## Branch workflow

Use `main` as the working and delivery branch, as requested by the user. Commit
and push completed, verified changes directly to `main` unless the user requests
a different workflow.

## Current phase

This repository currently contains the researched blueprint. Read
`docs/PLAN.md`, `docs/USAGE.md`, `docs/DESIGN.md`, and `docs/RESEARCH.md` before implementation.
The future file tree in the plan is a proposal, not a list of existing modules.
Keep README status and milestone checkboxes accurate as implementation lands.

## Work sequence

1. Identify the consuming project's Minecraft version, loader, mappings, Java
   version, and existing renderer. Use that project's established stack.
2. For this repository's first adapter, complete the compatibility spike in the
   plan and record exact dependency versions before implementing components.
3. Define the screen's task, hierarchy, component states, and layout at the
   smallest supported logical viewport.
4. Use shared tokens and established components. Keep interaction and motion
   logic separate from Minecraft and renderer adapters where practical.
5. Verify behavior in the actual Minecraft client. A screenshot or desktop
   preview alone does not establish in-game compatibility.
6. Report what changed, checks performed, evidence, and remaining limitations.

## Design and implementation rules

- Use readable sans-serif fonts with appropriate anti-aliasing. Measure text
  with the renderer that draws it. Verify glyph fallback and GUI scale changes.
- Follow the visual direction and motion table in `docs/DESIGN.md`.
- Use monotonic elapsed time for UI animation; keep behavior independent of FPS,
  game ticks, and pause state. Do not sleep or block the render thread.
- Retarget interrupted motion from the current displayed value. Keep visual
  transforms, clipping, and input coordinates consistent.
- Implement keyboard focus, Escape behavior, disabled states, modal input
  handling, and reduced motion alongside mouse interactions.
- Keep GPU resource creation out of the per-frame path. Follow the selected
  renderer's threading, state, and resource lifecycle requirements.
- Read version-matched documentation or source before using an API. Never mix
  mappings or render APIs from different Minecraft versions.
- Keep third-party license notices when distributing dependencies or assets.
  Do not copy restricted reference implementations or present their benchmarks
  as measurements of this project.

## Evidence

Once runnable code exists, add focused tests for timing, interruption, input,
and resource lifecycle where appropriate. Validate the selected adapter in game
at multiple GUI scales and frame rates. Document reference hardware and build
versions with any performance claims. Do not claim untested loaders or versions.

For documentation-only changes, check links, consistency, and scope; a game
build is unnecessary.
