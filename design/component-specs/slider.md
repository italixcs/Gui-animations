# Slider

A slider exposes a labeled bounded numeric setting, including the current value
and unit. Its interactive row is at least `metrics.compactControlHeight` high;
the visible track uses `metrics.sliderTrackHeight`. Provide enough thumb room at
both ends so the minimum and maximum do not clip.

Use `border` for the unfilled track and `accent` for the selected range. The thumb
needs a dark `background` outline when resting on a lighter track, so it remains
distinct. Keyboard focus uses an outer `focus` ring with dark spacing.

Define minimum, maximum, and step explicitly. Clamp pointer and keyboard values
to those bounds. Left/Right changes by one step, Home/End selects endpoints, and
the framework should expose the numeric value to accessibility. Support modifier
increments only if documented to the user. A precise numeric entry can accompany
a setting where a slider alone is insufficient.

During drag, acquire the framework's pointer capture and release it on pointer
up, cancellation, screen disposal, or loss of the interaction. Do not require
the pointer to remain over the thumb. Update the actual value directly under the
pointer; do not add easing that makes the thumb lag behind input. Use short color
feedback for hover/focus and animation only for externally changed values when
it helps orientation. Reduced motion snaps external changes.

Verification: bounds, exact step rounding, dragging beyond either end, release
outside the panel, keyboard-only edits, persisted value restoration, and layout
changes while dragging. The hit test must use the same transformed bounds as
the visible slider.
