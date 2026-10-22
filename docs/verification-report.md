# Verification report

Gui-animations 0.1.0 was built and checked on 2026-10-22. The pinned Minecraft
client rendered the showcase, accepted real keyboard/mouse input, persisted
settings, survived resize and resource reload, and completed the corrected
rendering pass without the earlier high-severity GL/uniform errors.

## Environment and scope

Minecraft 26.1.2, Fabric Loader 0.19.2, Fabric API 0.149.0+26.1.2, ModernUI Fabric
3.13.0.5/core 3.13.0, JDK 25, Gradle 9.4.0, and Loom 1.16.3. Runtime inspection
used a managed Linux workspace, Xvfb, and Mesa 25.0.7 llvmpipe OpenGL 4.5.
Keyboard and pointer events were sent to the actual Minecraft window; an
assistant inspected the original window captures.

## Completed checks

| Check | Result |
| --- | --- |
| Full Gradle build | Passed for core, ModernUI adapter, and showcase, including sources and distributable JARs. |
| Java tests | 34 passed: 20 motion-core, 10 adapter lifecycle/motion, and 4 settings-store tests. |
| Python tests | 43 passed: 29 installer/package and 14 design-validation tests. |
| Design assets | 13 contrast pairs, strict schema, geometry/timing constraints, and actual font/license hashes passed. |
| Self-contained package | 63 payload files plus manifest; deterministic freshness check passed. |
| External consumer checks | All 8 passed, including a standalone Gradle build from installed templates outside this checkout. |
| Agent-consumption exercise | A separate agent used only installed guidance/source to add and build a second screen reusing the shared theme, controls, and motion. |
| Actual Minecraft UI | Clean proportional Inter text, rounded shapes, visible slider thumb, navigation, toggles, popup, tooltip, and save feedback observed. |
| Keyboard and modal input | Tab/Shift+Tab focus, Enter activation, modal focus capture, cancellation, reset confirmation, focus restoration, and Escape order observed. |
| Settings | Pointer and keyboard slider changes, save to the local properties file, and close/F8 reopen persistence observed. |
| Layout | 1280×800 and 320×240 windows checked; compact navigation fits, content scrolls, footer actions and clamped modal remain accessible. |
| Lifecycle | Repeated close/reopen, live resize, and F3+T resource reload completed. |
| Corrected rendering | Zero `GL_INVALID_OPERATION` and zero `severity=HIGH` messages in the final runtime session after the host bridge fix. |

The Python and Java suites cover interruption continuity, changed travel
distance, reduced-motion settlement, cancellation and stale callbacks,
completion reentrancy, elapsed-time schedules at 30/60/144 FPS, malformed
settings, installer rollback, update conflicts, source provenance, path safety,
and token mutations. The FPS schedules are mathematical tests, not GPU
performance measurements.

The [consumer harness](../tests/verification/verify_consumer.py) checks both
Codex and Claude installations, payload hashes/modes and 49 local links per
installed skill, existing-rule/source preservation, idempotence, local-edit
conflicts, mixed agents, corrupted-source refusal, and destination symlinks.
See [final consumer results](evidence/consumer-checks.json),
[runtime action summary](evidence/runtime-checks.json), and the
[separate agent exercise](evidence/agent-consumer-evaluation.md).

## Runtime evidence

- [Main showcase](media/showcase.png)
- [Keyboard focus in the reset dialog](media/modal-keyboard.png)
- [Compact viewport with scrolled content](media/compact-scroll.png)
- [10-second actual Minecraft interaction recording](media/interactions.mp4)

The H.264 recording captures the real 1280×800 X11 window at 30 capture FPS.
It is not a browser mockup, generated animation, or frame-rate benchmark.

## Defects found and fixed

1. **Interrupted motion jumped.** Separate elapsed-time alpha and translation
   tracks now retarget from displayed values, including when travel distance
   changes. Regression tests cover reversal, cancellation, and callbacks.
2. **Slider thumb was invisible.** ModernUI's circle shape default uses its level
   as a pie angle; level zero produced no circle. The thumb explicitly disables
   level-dependent shape rendering.
3. **Keyboard focus failed.** The host's touch-mode root rejected controls that
   were only focusable. Controls now opt into touch-mode focus; pointer-down
   focus handling preserves first-click activation. Modal keyboard/reset paths
   were exercised in the actual client.
4. **Sampler uniforms targeted the wrong GL program.** A synchronous diagnostic
   callback isolated Minecraft's stale `GlCommandEncoder.lastProgram` cache
   after external ModernUI rendering. The exact-version host bridge invalidates
   cached program/pipeline references once after external work and before
   compositing; Minecraft performs its normal rebind. It neither suppresses
   errors nor destroys/recompiles the shader cache. Temporary diagnostics were
   removed, and the final session had no recurrence.
5. **Small layouts overflowed.** The modal width is clamped to available space;
   compact header/footer and the short Style tab preserve access at 320×240.
6. **Async work could outlive its view.** Generation guards, cancellation,
   callback disposal, and save-state checks prevent stale UI updates.
7. **Installer upgrade edge cases.** Reserved paths, removed-file mode edits,
   and independent source-import provenance gained preflight checks and tests.

During development, rebuilding a JAR while a running JVM held it open caused a
ZIP-header class-load failure. The test process was corrected to stop the game
before rebuilding; original runtime artifacts stayed frozen during inspection.
An external X11 window-destruction command also produced a native `BadDrawable`
teardown error. A fresh run was then closed through Minecraft's own Quit Game
control to verify normal application shutdown separately from that harness error.

## Remaining limits

This is Linux software-renderer coverage, not a physical GPU/high-DPI benchmark
or Windows/macOS/Vulkan certification. The title-screen entry path was exercised;
world creation, multiplayer/disconnect paths, and broad language/font fallback
coverage were not independently exercised. GUI-scale/device-density combinations
beyond the recorded viewport checks need additional hardware testing.

The headless environment logged a GLFW standard-cursor availability warning and
initial no-audio-device messages; those are distinct from the corrected shader
binding defect. No universal FPS guarantee or automatic compliance by every
coding agent/editor is claimed. The extra agent-generated screen compiled but
its own runtime appearance was not inspected.
