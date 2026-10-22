# Design workflow

## Establish the screen contract

Read the request and inspect nearby screens. Identify the primary task, important
actions, content groups, settings bindings, smallest logical viewport, long
translated labels, and keyboard path. Use the project's existing theme when it
has one; otherwise start from the packaged [tokens](../templates/starter/design/tokens.json).

Write a concise design note in the consuming project containing:

- The screen's purpose and hierarchy, with one clear primary action.
- A responsive layout for the smallest viewport and the usual viewport.
- The shared components and real settings/actions each control uses.
- Relevant hover, focus, pressed, disabled, error, and loading states.
- Opening/closing, popup, and click treatments plus reduced motion.

Prefer a compact, readable interface to a large decorative dashboard. Minecraft
logical viewports can be much smaller than the desktop window. Collapse the
sidebar or make content scroll when space requires it; never hide the only way
to close a dialog below the screen.

## Apply the theme consistently

Use [component specifications](../templates/starter/design/component-specs/README.md) for state and behavior
requirements. Use the token hierarchy for colors, spacing, typography, radii,
and motion. Keep local theme overrides in the consuming project's owned source
or configuration, not in this installed package.

The reference adapter maps design lengths to ModernUI dp and type sizes to sp.
Let host density perform the framebuffer conversion; do not apply Minecraft GUI
scale a second time. Confirm the consuming adapter uses the same coordinate
contract before porting dimensions to another renderer.

Use the bundled Inter font and its OFL notice when appropriate. Keep the
renderer's Unicode fallback enabled. Test a long label and a non-Latin example;
the presence of a TTF does not establish fallback coverage or rendering quality.

## Make hierarchy and state visible

Use spacing and typography before adding effects. Keep text readable on actual
composited surfaces: target 4.5:1 for normal text and 3:1 for large text and
relevant non-text indicators. Focus must remain visible against its adjacent
surfaces. A state must not rely only on hue or animation.

Default to the restrained dark theme and one accent. Use an optional dimmed
backdrop; blur and shadows require measured justification. Avoid idle movement,
animated backgrounds, and aggressive overshoot on text-bearing screens.

Before implementation, read [the implementation workflow](implementation.md)
and locate the matching source templates from [the source map](source-map.md).
