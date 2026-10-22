# Optional motion core

`dev.guianimations.core` contains a small elapsed-time tween for screen-level
effects and custom adapters. Use your renderer's established animation facilities
for ordinary component animations when they already meet the requirements.
This module has no runtime dependencies and produces Java 21-compatible classes.
Its build uses the repository's JDK 25 toolchain with `--release 21`.

```java
import dev.guianimations.core.Easing;
import dev.guianimations.core.Tween;
import java.time.Duration;

// In the screen lifecycle, once when opening:
Tween opacity = new Tween(0.0);
opacity.animateTo(1.0, Duration.ofMillis(220), Easing.CUBIC_OUT, reducedMotion);

// In each UI frame, sample once and pass this value to the renderer:
double alpha = opacity.value();

// Retarget from the currently displayed state, for example when closing:
opacity.animateTo(0.0, Duration.ofMillis(140), Easing.CUBIC_IN, reducedMotion);

// When the screen is replaced/disposed:
opacity.cancel();
```

These statements illustrate separate lifecycle events, not a single block to
run every frame. Each animated property needs its own `Tween` instance. Reuse the
sampled value for drawing and any corresponding input transform in that frame.
`animateTo` with the same active target preserves its original deadline. Switching
reduced motion on can instantly settle an active tween by retargeting with the
same destination and `reducedMotion = true`. `snapTo` also settles instantly.

The default clock is `System.nanoTime`; tests can inject a `LongSupplier` clock.
The clock must be monotonic. A backwards sample throws; signed `nanoTime`
rollover works as long as consecutive samples are less than 2^63 nanoseconds
apart. Long frame gaps catch up to real elapsed time, so a transition may finish
while the game is paused or unfocused. No sleeps, background work, callbacks, or
game-tick dependency are involved. The class is intentionally single-threaded.

Run the focused tests from the repository root:

```sh
./gradlew :reference:core:test
```

The tests cover 30/60/144 FPS schedules, interruption continuity, repeated
targets, cancellation, reduced motion, long gaps, invalid inputs, and signed
clock rollover. They verify elapsed-time math, not GPU frame pacing or perceived
smoothness; in-game rendering and transformed hitboxes must still be checked
through the selected adapter.
