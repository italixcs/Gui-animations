# Modal confirmation

A modal contains a clear title, a concise explanation, and explicit actions.
Use `panel` fill, `radii.popup`, `spacing.lg` padding, and a viewport-fitting
scrollable body. Draw `scrim` at `effects.scrimOpacity` behind it. Blur remains
optional and disabled by default.

On open, remember the previously focused control and move focus into the dialog.
For destructive confirmation, focus Cancel or another safe default rather than
the destructive action. Tab/Shift+Tab stay inside the modal. Background controls
receive neither pointer nor keyboard activation while the modal is active.

Escape cancels the top dismissible modal. A backdrop click may cancel a simple
confirmation if the behavior is deliberate; it must never confirm a destructive
action. Button actions dispatch once. After closing, restore focus to the prior
trigger if it still exists, otherwise to a meaningful current screen control.

Opening uses `motion.popupOpen`, closing uses `motion.popupClose`. Keep the modal
in the input layer throughout the closing transition, then remove it and restore
focus. Repeated open/close input retargets from the current displayed state.
Replacing the parent screen cancels modal animation and stale callbacks.
Reduced motion removes translation and immediately settles visibility.

Verification: keyboard trap and restoration, background input blocking,
Escape/backdrop cancel, rapid open-close-open, parent disposal, narrow viewport,
and the selected action firing exactly once.
