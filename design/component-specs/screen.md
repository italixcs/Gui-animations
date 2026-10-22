# Responsive settings screen

The showcase starts with a navigation region and one scrollable settings area.
Use `background`, a `panel` container, `spacing.lg` safe insets, and a centered
maximum width of `metrics.maxContentWidth`. All lengths use the adapter's
logical coordinate contract rather than raw framebuffer pixels.

At widths at or above `metrics.sidebarBreakpoint`, allocate
`metrics.sidebarWidth` to side navigation and place content beside it with
`spacing.md` or `spacing.lg` gap. Below the breakpoint, use a compact navigation
selector or horizontal category control above content. Keep both navigation and
the active category reachable by keyboard. Layout must continue working at
`metrics.minViewportWidth` × `metrics.minViewportHeight` through scrolling.

Each setting has a label, the appropriate shared control, and helper/error text
only where needed. Labels wrap rather than overlapping controls. Preserve the
focused setting and deliberate scroll position when changing categories.
Search results explain empty state and provide a way to clear the query.

Use `motion.screenOpen` for the screen container and `motion.screenClose` when
returning to its parent. Prefer opacity/translation of the whole content over
independent effects on every label. Preserve display-to-input transform
agreement throughout motion. Reduced motion immediately settles the screen.

Escape first dismisses a tooltip or modal where applicable, then returns to the
parent through the normal client screen lifecycle. Screen replacement, reload,
resize, disconnect, and re-open must cancel/rebuild renderer resources through
their supported lifecycle and leave no orphaned input or callbacks.

Verification: narrow/large viewport, multiple GUI scales/high DPI, long and
non-Latin labels, screen open-close-reopen, Tab order, category changes during
motion, pointer clicks on animated controls, and unchanged unrelated game settings.
