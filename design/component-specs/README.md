# Component catalog

All values refer to the canonical [tokens](../tokens.json), and all components
use the [coordinate contract](../README.md#coordinate-contract). Reuse the
adapter's shared controls instead of implementing a different state machine for
each screen.

| Component | Purpose |
| --- | --- |
| [Panel](panel.md) | Group content on a clear, rounded surface. |
| [Button](button.md) | Trigger one action with visible mouse/keyboard feedback. |
| [Toggle](toggle.md) | Change a boolean setting with an explicit label and state. |
| [Slider](slider.md) | Adjust a bounded value precisely using mouse or keyboard. |
| [Tooltip](tooltip.md) | Explain a control without hiding required instructions. |
| [Modal](modal.md) | Request focused confirmation and restore prior focus. |
| [Toast](toast.md) | Confirm completed work without interrupting input. |
| [Screen layout](screen.md) | Adapt navigation and content to small viewports. |

Every interactive component must handle focus, disabled state, rapid repeated
input, and lifecycle disposal. Reduced motion applies throughout; state changes
still happen immediately. Screen replacement cancels pending callbacks and
prevents hidden controls from receiving input.
