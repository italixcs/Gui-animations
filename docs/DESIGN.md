# Design direction

This is the visual and interaction contract for the first release. The
[canonical tokens and component specifications](../design/README.md) are the
machine-readable source of truth. The ModernUI adapter maps lengths to dp and
type sizes to sp; see [compatibility](compatibility.md).

## Visual character

A restrained desktop-app interface inside Minecraft: calm dark surfaces, readable
sans-serif typography, a clear accent color, soft corners, and quick motion.
The game world may remain visible behind the UI. Use an optional dim backdrop;
add blur only after profiling it, with a plain dim fallback.

Keep density practical for Minecraft's small logical GUI viewports. Establish a
clear hierarchy with spacing and typography before adding decoration. Choose one
accent and avoid constant animated gradients or idle bouncing.

## Typography

Use a properly licensed sans-serif font, with Inter as a candidate and Noto Sans
as a possible fallback. Ship chosen font assets with their notices rather than
depending on whichever fonts happen to be installed on the user's machine.

The renderer must support anti-aliased text and appropriate font fallback.
Adding a TTF file alone does not prove text will look smooth at every GUI scale.
Measure with the same font and shaping system used to draw; test kerning, long
labels, non-Latin text, and high-DPI output.

Start with four type roles: caption, body / control label, section heading, and
screen title. Avoid unnecessary weights and tiny helper text. Prefer opacity
and translation for text-bearing animations; if scale is used, verify text stays
sharp throughout the animation.

## Token defaults

All lengths are logical design units. During the compatibility spike, define
their conversion to the selected renderer's coordinates and physical pixels.
Do not treat browser CSS pixels or framebuffer pixels as Minecraft GUI units.

| Role | Starting value |
| --- | --- |
| Background | `#101419` |
| Panel | `#191F27` |
| Raised / hovered surface | `#232C37` |
| Primary text | `#F3F5F7` |
| Secondary text | `#AAB6C5` |
| Accent | `#8BB6FF` |
| Text on accent | `#12284A` |
| Spacing scale | 4, 8, 12, 16, 24 |
| Control / panel radius | 6 / 10 |
| Control height | 28–32, subject to in-game density checks |
| Caption / body / section / title | 10 / 12 / 16 / 22, subject to rasterization checks |

Check actual composited colors for contrast, especially on translucent surfaces.
Aim for WCAG AA contrast: 4.5:1 for normal text and 3:1 for large text and relevant
non-text UI indicators. A border or focus indicator must remain visible against
its adjacent surfaces. Validate at the smallest supported viewport, with a
scrollable layout and compact navigation when needed.

Rounded corners require appropriate anti-aliasing. Use the selected renderer's
supported path, shape, or shader facilities; a low-resolution rounded PNG stretched
to fit is unlikely to satisfy this project's goal.

## Motion language

Use motion to show cause and state changes. Input should receive immediate
feedback; a transition must not delay the action until its visual animation ends.

| Interaction | Initial duration | Treatment |
| --- | --- | --- |
| Hover / focus change | 100 ms | Color / border transition; focus stays obvious. |
| Press feedback | 70 ms | Immediate color response; optional restrained scale to 0.98. |
| Release | 110 ms | Return from the current displayed state. |
| Screen opening | 220 ms | Fade and translate up to 8 units into place; ease out. |
| Screen closing | 140 ms | Short fade / translation; release the screen at the right lifecycle point. |
| Popup opening / closing | 160 / 120 ms | Fade and translate up to 4 units; anchored to its trigger. |
| Toast entrance / exit | 220 / 160 ms | Short slide and fade; no required action available only briefly. |
| Tab / section change | 140 ms | Restrained content transition; preserve focus and scroll deliberately. |

Use gentle ease-out for arrivals and short ease-in for departures. Start with
tweens; add springs only for an interaction that benefits from them and only
after verifying stable timing. Avoid overshoot on text-heavy screens.

## Behavior rules

- Sample monotonic elapsed time in the UI lifecycle; do not advance animations
  by fixed amounts per frame or rely on a 20 Hz game tick. Decide explicitly
  how focus loss or long frame gaps affect each transition.
- An interrupted transition starts from the current displayed value. Repeated
  hover, click, open, and close events must not cause jumps or stale callbacks.
- Visual transforms and input transforms must agree. Check scissoring, nested
  clips, GUI scale, hit testing, tooltips, and popup anchoring together.
- Modal opening moves focus into the dialog. Tab stays within it. Closing
  restores focus to the trigger when that trigger still exists.
- Escape dismisses the top dismissible layer first. Define behavior for the
  parent screen, outside clicks, and destructive confirmation explicitly.
- A button triggers once for a complete intended activation. Disabled controls
  ignore activation and communicate their state without depending only on color.
- Screen replacement cancels outstanding UI transitions and callbacks. Dispose
  renderer resources through its supported lifecycle, including reload / resize.
- Reduced motion removes spatial motion and springs. Apply state changes
  immediately, optionally retaining a brief opacity transition of at most 80 ms.
  Keep focus, state, and interaction feedback clear.

## Verification

Evaluate the actual screen in Minecraft. Confirm text sharpness, shape edges,
scale behavior, keyboard navigation, transformed hitboxes, rapid interactions,
and modal layering. Observe motion at several FPS caps and record a video.

Measure the UI's added CPU / GPU frame time on named reference hardware instead
of promising a universal FPS figure. Compare blur on/off and screen closed/open,
look for allocations or resource growth over repeated cycles, and document the
method and limits with the results.
