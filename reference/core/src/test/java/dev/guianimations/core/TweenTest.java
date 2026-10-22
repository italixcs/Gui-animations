package dev.guianimations.core;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.function.LongSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TweenTest {
    private static final Duration OPEN = Duration.ofMillis(220);

    @ParameterizedTest
    @ValueSource(ints = {30, 60, 144})
    void elapsedTimeIsIndependentOfFrameSchedule(int framesPerSecond) {
        FakeClock clock = new FakeClock();
        Tween tween = new Tween(0.0, clock).animateTo(1.0, OPEN, Easing.CUBIC_OUT);
        long lastFrameBeforeCheckpoint = (long) Math.floor(0.11 * framesPerSecond);
        for (int frame = 1; frame <= lastFrameBeforeCheckpoint; frame++) {
            clock.nanos = Math.round(frame * 1_000_000_000.0 / framesPerSecond);
            double observed = tween.value();
            double expected = Easing.CUBIC_OUT.apply((double) clock.nanos / OPEN.toNanos());
            assertEquals(expected, observed, 1e-12);
        }
        // A shared wall-time checkpoint must agree even when the preceding frame counts differ.
        clock.nanos = 110_000_000L;
        assertEquals(0.875, tween.value(), 1e-12);
        clock.nanos = 220_000_000L;
        assertEquals(1.0, tween.value());
        assertFalse(tween.isRunning());
    }

    @Test
    void retargetStartsFromCurrentlyDisplayedValue() {
        FakeClock clock = new FakeClock();
        Tween tween = new Tween(0.0, clock).animateTo(1.0, OPEN, Easing.LINEAR);
        clock.nanos = 55_000_000L;
        assertEquals(0.25, tween.value());
        tween.animateTo(-1.0, Duration.ofMillis(100), Easing.LINEAR);
        assertEquals(0.25, tween.value(), "Retarget must not jump");
        clock.nanos = 105_000_000L;
        assertEquals(-0.375, tween.value());
        clock.nanos = 155_000_000L;
        assertEquals(-1.0, tween.value());
    }

    @Test
    void repeatedTargetDoesNotExtendDeadline() {
        FakeClock clock = new FakeClock();
        Tween tween = new Tween(0.0, clock).animateTo(1.0, OPEN, Easing.LINEAR);
        clock.nanos = 100_000_000L;
        tween.animateTo(1.0, OPEN, Easing.LINEAR);
        clock.nanos = 220_000_000L;
        assertEquals(1.0, tween.value());
        assertFalse(tween.isRunning());
    }

    @Test
    void retargetSamplesCurrentTimeEvenWithoutAnIntermediateFrame() {
        FakeClock clock = new FakeClock();
        Tween tween = new Tween(0.0, clock).animateTo(1.0, OPEN, Easing.LINEAR);
        clock.nanos = 110_000_000L;
        tween.animateTo(0.0, OPEN, Easing.LINEAR);
        assertEquals(0.5, tween.value());
    }

    @Test
    void zeroDurationAndReducedMotionSettleImmediately() {
        FakeClock clock = new FakeClock();
        Tween tween = new Tween(0.0, clock).animateTo(1.0, Duration.ZERO, Easing.CUBIC_OUT);
        assertEquals(1.0, tween.value());
        assertFalse(tween.isRunning());
        tween.animateTo(0.0, OPEN, Easing.CUBIC_OUT);
        clock.nanos = 10_000_000L;
        tween.animateTo(0.0, OPEN, Easing.CUBIC_OUT, true);
        assertEquals(0.0, tween.value());
        assertFalse(tween.isRunning());
    }

    @Test
    void longFrameGapCatchesUpExactly() {
        FakeClock clock = new FakeClock();
        Tween tween = new Tween(-8.0, clock).animateTo(0.0, OPEN, Easing.CUBIC_OUT);
        clock.nanos = Duration.ofHours(1).toNanos();
        assertEquals(0.0, tween.value());
        assertEquals(0.0, tween.target());
        assertFalse(tween.isRunning());
    }

    @Test
    void cancellationHoldsCurrentValueAndCannotCompleteLater() {
        FakeClock clock = new FakeClock();
        Tween tween = new Tween(0.0, clock).animateTo(1.0, OPEN, Easing.LINEAR);
        clock.nanos = 110_000_000L;
        tween.cancel();
        assertEquals(0.5, tween.value());
        assertEquals(0.5, tween.target());
        clock.nanos = Duration.ofHours(1).toNanos();
        assertEquals(0.5, tween.value());
        assertFalse(tween.isRunning());
        tween.animateTo(0.0, OPEN, Easing.LINEAR);
        clock.nanos += OPEN.toNanos();
        assertEquals(0.0, tween.value(), "A cancelled tween remains reusable");
    }

    @Test
    void snapReplacesTheOldTransition() {
        FakeClock clock = new FakeClock();
        Tween tween = new Tween(0.0, clock).animateTo(1.0, OPEN, Easing.LINEAR);
        tween.snapTo(0.3);
        clock.nanos = OPEN.toNanos() * 2;
        assertEquals(0.3, tween.value());
        assertFalse(tween.isRunning());
    }

    @Test
    void backwardsClockIsRejectedWithoutChangingTransitionState() {
        FakeClock clock = new FakeClock();
        clock.nanos = 5_000_000L;
        Tween tween = new Tween(0.0, clock).animateTo(1.0, OPEN, Easing.LINEAR);
        clock.nanos = 115_000_000L;
        assertEquals(0.5, tween.value());
        clock.nanos--;
        assertThrows(IllegalArgumentException.class, tween::value);
        clock.nanos = 115_000_000L;
        assertEquals(0.5, tween.value());
    }

    @Test
    void signedNanoTimeRolloverIsSupported() {
        FakeClock clock = new FakeClock();
        clock.nanos = Long.MAX_VALUE - 100_000_000L;
        Tween tween = new Tween(0.0, clock).animateTo(1.0, OPEN, Easing.LINEAR);
        clock.nanos += 110_000_000L;
        assertTrue(clock.nanos < 0, "This sample crossed signed long rollover");
        assertEquals(0.5, tween.value());
        clock.nanos += 110_000_000L;
        assertEquals(1.0, tween.value());
    }

    @Test
    void accumulatedElapsedTimeCannotOverflowForVeryLongTransitions() {
        FakeClock clock = new FakeClock();
        Tween tween = new Tween(0.0, clock).animateTo(
                1.0, Duration.ofNanos(Long.MAX_VALUE - 1), Easing.LINEAR);
        clock.nanos = Long.MAX_VALUE / 2;
        assertEquals(0.5, tween.value(), 1e-12);
        // Each reading is a valid monotonic delta, but total time crosses signed long range.
        clock.nanos += Long.MAX_VALUE / 2 + 10;
        assertEquals(1.0, tween.value());
        assertFalse(tween.isRunning());
    }

    @Test
    void invalidArgumentsLeaveAnExistingTransitionIntact() {
        FakeClock clock = new FakeClock();
        Tween tween = new Tween(0.0, clock).animateTo(1.0, OPEN, Easing.LINEAR);
        assertThrows(IllegalArgumentException.class,
                () -> tween.animateTo(Double.NaN, OPEN, Easing.LINEAR));
        assertThrows(IllegalArgumentException.class,
                () -> tween.animateTo(0.0, Duration.ofNanos(-1), Easing.LINEAR));
        assertThrows(IllegalArgumentException.class,
                () -> tween.animateTo(0.0, Duration.ofSeconds(Long.MAX_VALUE), Easing.LINEAR));
        assertThrows(NullPointerException.class, () -> tween.animateTo(0.0, null, Easing.LINEAR));
        assertThrows(NullPointerException.class, () -> tween.animateTo(0.0, OPEN, null));
        assertThrows(IllegalArgumentException.class, () -> tween.snapTo(Double.POSITIVE_INFINITY));
        clock.nanos = 110_000_000L;
        assertEquals(0.5, tween.value());
        assertEquals(1.0, tween.target());
    }

    @Test
    void invalidInitialValuesAndClockAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Tween(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new Tween(Double.NEGATIVE_INFINITY));
        assertThrows(NullPointerException.class, () -> new Tween(0.0, null));
    }

    @Test
    void largeFiniteEndpointsDoNotOverflowDuringInterpolation() {
        FakeClock clock = new FakeClock();
        Tween tween = new Tween(-Double.MAX_VALUE, clock)
                .animateTo(Double.MAX_VALUE, Duration.ofSeconds(1), Easing.LINEAR);
        clock.nanos = 500_000_000L;
        assertEquals(0.0, tween.value());
        clock.nanos = 1_000_000_000L;
        assertEquals(Double.MAX_VALUE, tween.value());
    }

    private static final class FakeClock implements LongSupplier {
        long nanos;

        @Override
        public long getAsLong() {
            return nanos;
        }
    }
}
