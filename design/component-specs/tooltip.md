# Tooltip

A tooltip supplies optional context for a labeled control. Required instructions,
errors, and setting values stay visible elsewhere. Use an opaque `raised`
surface, `textPrimary`, `radii.control`, `spacing.sm` padding, and at most
`metrics.tooltipMaxWidth` width with wrapped text.

Show after `motion.tooltipDelayMs` of pointer hover. Keyboard focus may reveal
the same description without requiring pointer movement. Position within the
available viewport and shift to avoid the trigger and important actions. The
tooltip belongs above its own content but below an active unrelated modal.

The tooltip does not capture clicks or enter the tab order. Hide it when hover
and focus leave the trigger, the trigger is removed, scrolling moves the anchor,
or the owning screen closes. Escape dismisses a visible tooltip before an outer
dismissible layer where appropriate. Do not keep delayed callbacks from a closed
screen alive.

Use short opacity feedback rather than a bouncing or scaling text effect.
Reduced motion uses an immediate appearance. The trigger's accessible
description should carry the explanatory text even when the visual tooltip is
not shown.

Verification: all viewport corners, long translated text, keyboard focus,
scrolling anchors, delayed-show cancellation, and modal layering.
