# Panel

Use `colors.panel`, `radii.panel`, and `spacing.lg` padding. Separate groups with
`spacing.md` or `spacing.lg`; use `colors.raised` for an interactive or nested
surface only when hierarchy needs it. A heading uses `typography.sizes.section`
and body text uses `typography.sizes.body`.

Draw anti-aliased rounded geometry through the renderer. Keep edge clipping and
child layout consistent; do not leave a rectangular child background extending
beyond rounded corners. Child focus outlines need enough inset to remain visible.

A panel is a grouping surface, not an implicit button. It should not enter the
keyboard tab order unless it has its own meaningful action. Use a scrollable
content container where children exceed available height. Panel open/close motion
belongs to its screen or modal; avoid separately animating every text label.

Verification: long translated headings wrap, content scrolls inside its clip,
focus rings remain visible, and the smallest supported viewport has no offscreen
required actions.
