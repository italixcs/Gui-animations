# Toggle

A toggle represents one boolean setting, with a visible label, current state,
and accessible control name. Use `metrics.toggleWidth` × `toggleHeight`, a pill
radius, and a full-height labeled row as the activation target where practical.

| State | Track | Thumb |
| --- | --- | --- |
| Off | `border` | `textPrimary` |
| On | `accent` | `textOnAccent` |
| Disabled | `raised`, visible outline | `textDisabled` |

The label uses `textPrimary`; helper text uses `textSecondary`. Checked state
must be conveyed by thumb position and control semantics, not color alone.
Do not label a setting only with an unexplained icon.

Click or keyboard activation changes the value exactly once. A labeled row and
its inner control must share one action path to avoid toggling twice. Tab moves
to/from the toggle; Space uses the framework's checkable-control semantics.
Disabled state leaves the value unchanged.

Thumb movement uses `motion.release`; color follows `motion.hover`. Commit the
boolean immediately, animate its visual representation, and retarget rapid
changes without jumping. Reduced motion immediately places the thumb in its
new location. Persist only through the consuming project's settings API.

Verification: repeated activation ends in the expected boolean state, row/thumb
clicks dispatch once, keyboard semantics expose checked state, and restoring a
saved setting updates both the value and display.
