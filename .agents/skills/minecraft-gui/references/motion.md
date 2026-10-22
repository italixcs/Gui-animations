# Motion workflow

Use the project's motion tokens and the selected renderer's supported animation
primitives. Inspect the packaged [starter](source-map.md) for working lifecycle
and transition wiring before adding another animation system.

## Define the transition

For each interaction, specify its trigger, current state, destination, duration,
easing, cancellation/retargeting behavior, and reduced-motion outcome. Immediate
input feedback should occur at once; do not postpone a setting change or action
until a decorative transition completes.

Initial token treatments are restrained: hover 100 ms, press 70 ms, release
110 ms, screen opening 220 ms, closing 140 ms, popup opening/closing 160/120 ms,
and toast opening/closing 220/160 ms. Read
[the actual token file](../templates/starter/design/tokens.json) for authoritative values and avoid
duplicating numbers in each component.

Prefer ease-out for arrivals and short ease-in for departures. Translation and
opacity preserve text clarity better than large scale changes. If a subtle
press scale is used, verify glyph sharpness and transformed hit testing. Do not
introduce springs or overshoot without an interaction that benefits from them.

## Handle time and interruption

- Drive progression with monotonic elapsed time, independently of the game tick,
  frame count, and pause state. Do not block or sleep on a UI/render thread.
- A repeated or reversed trigger starts from the displayed value. Ignore stale
  completion callbacks after cancellation, screen replacement, or disposal.
- Decide what a long frame or loss of window focus should do. Avoid accumulating
  an unbounded animation backlog on refocus.
- Keep input transforms, rendering transforms, and clipping aligned throughout
  the transition. Check popup anchors, nested clips, and tooltip placement.
- Reduced motion removes spatial movement and springs, applying state changes
  immediately. Preserve focus and clear state feedback.

## Verify observable behavior

Use injected/manual clocks for focused automated tests where possible. Check
start, midpoint, endpoint, reversal, rapid repeated events, zero duration,
long frame gaps, reduced motion, and disposal before completion. Check that one
intended activation dispatches its action once.

In Minecraft, observe the same transition under 30, 60, and 144 FPS caps and
several GUI scales. Record actual results; mathematical timing tests do not
establish perceived smoothness, glyph sharpness, or GPU cost.
