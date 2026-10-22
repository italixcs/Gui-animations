# Review and diagnosis workflow

Inspect the requested behavior, changed code, shared components, build pins, and
actual client evidence. Review against the screen contract, the project's theme,
and [component specifications](../templates/starter/design/component-specs/README.md).

## Check the whole path

1. **Discoverability and build:** installed skill references resolve, the agent
   selected the correct stack, source imports retain notices, and the consuming
   project's documented build/tests pass.
2. **Real behavior:** controls bind to real settings/actions, persistence has an
   observable result, and failed saves show accurate feedback.
3. **Appearance:** proportional text is sharp at tested scales; fallback and
   long labels work; rounded edges are anti-aliased; density, hierarchy, clipping,
   and contrast remain usable at the smallest supported viewport.
4. **Input:** mouse coordinates match the visual layout; keyboard focus and
   activation work; disabled controls ignore activation; modals capture focus
   and block background input; Escape dismisses the correct layer.
5. **Motion:** timing is independent of FPS; rapid interaction, reversal,
   screen replacement, and reduced motion behave correctly without stale actions.
6. **Lifecycle/performance:** resize, reload, reopen, disconnect, and disposal do
   not leak resources or callbacks. Measure added frame time on named hardware
   before making performance claims; compare blur enabled and disabled if used.

## Investigate failures by cause

Reproduce the smallest failing sequence and identify the owning layer: project
binding, shared state, renderer, host adapter, font/resource lookup, coordinates,
dependency mismatch, or lifecycle. Fix that cause, add a focused regression test
where useful, and rerun the failed check plus affected neighboring behavior.
Do not suppress an error or weaken an assertion to make a build green.

Useful input sequences include opening/closing repeatedly, clicking during
arrival/departure, dragging beyond the slider bounds, keyboard-only operation,
opening a dialog over a focused control, resizing mid-transition, changing GUI
scale, and replacing the screen before a delayed callback completes.

## Report evidence and limits

State the exact tested stack, commands and results, in-game scenarios observed,
screenshots/video locations when captured, and remaining checks. Keep automated
state/timing tests separate from runtime appearance and performance observations.
Unobserved coverage remains unverified. Report each issue with its trigger,
impact, root cause when established, and fix rather than a vague quality score.
