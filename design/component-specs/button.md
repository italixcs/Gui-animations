# Button

Height is `metrics.controlHeight` (or `compactControlHeight` for a documented
compact layout), radius is `radii.control`, horizontal padding is `spacing.md`.
The hit target occupies the complete control bounds.

| State | Primary button | Neutral button |
| --- | --- | --- |
| Rest | `accent` fill, `textOnAccent` label | `panel` fill, `border` outline, `textPrimary` label |
| Hover | `accentHover` fill | `raised` fill |
| Pressed | `accentPressed` fill | `raised` fill plus immediate pressed feedback |
| Focused | Outer `focus` ring | Outer `focus` ring |
| Disabled | `raised` fill, `textDisabled` label | Same; no hover/activation |

Use `metrics.focusWidth` for the focus ring and leave a two-unit dark surface
gap between the ring and a bright control. That keeps the ring's adjacent colors
within the validated contrast contract. Keep the outline inside layout-safe
insets so clipping does not remove it.

Mouse activation requires primary-button press and release inside the intended
target. Releasing outside cancels the action. Enter/Space activate a focused
button once using the framework's standard semantics; do not also invoke the
same action from an independent key listener. Disabled buttons receive no action.
Preserve focus after the action unless a new screen/dialog deliberately takes it.

Animate color with `motion.hover` and press feedback with `motion.press` /
`motion.release`. Scale to 0.98 is optional: use it only when hit testing and text
sharpness remain correct. The action executes on activation rather than waiting
for a visual animation to finish. Retarget an interrupted effect from the current
displayed value. Reduced motion removes scale and applies immediate state feedback.

Verification: pointer drag-out cancellation, repeated clicks, held Space,
disabled activation, focus visibility over all fills, and correct action count.
