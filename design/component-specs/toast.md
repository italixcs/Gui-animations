# Toast

A toast confirms a completed action without stealing focus. Use `raised` with
`textPrimary`, or `successSurface` with `success` for a success message. Wrap at
`metrics.toastMaxWidth` and clamp to the viewport's safe insets. A clear textual
message is required; color or an icon alone is insufficient.

Show for `motion.toastVisibleMs`, measured with monotonic elapsed time. Entrance
uses `motion.toastOpen` and exit uses `motion.toastClose`; the timer begins when
the toast is visible. Reduced motion removes spatial movement and settles the
same state immediately. Limit the visible queue and combine repeated identical
messages to avoid hiding controls behind an unbounded stack.

The toast does not capture pointer input or enter the tab order. Announce its
message through the host's noninterrupting accessibility/narration mechanism
where supported. Do not put a required or destructive action only in a timed
toast; show a persistent alternative. Important errors remain in the related
screen rather than disappearing automatically.

Closing the owning screen cancels pending toast callbacks unless the application
explicitly owns a cross-screen toast system. Opening a modal must not put a
toast above the modal's required controls.

Verification: repeated notifications, queue bounds, pause/focus-loss timing,
screen replacement, translated wrapping, and absence of focus/input theft.
