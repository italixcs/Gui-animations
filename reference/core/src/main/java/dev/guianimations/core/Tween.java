package dev.guianimations.core;

import java.time.Duration;
import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * A small elapsed-time transition for one UI property, such as opacity or translation.
 *
 * <p>This is an optional recipe for screen-level effects or adapters that need one;
 * it does not replace a renderer's component animation system. Use one instance per
 * property, from a single UI thread. Read {@link #value()} once per frame and reuse
 * that value for drawing and input transforms.</p>
 *
 * <p>The default clock is {@link System#nanoTime()}. An injected clock must be
 * monotonic, with consecutive readings less than 2^63 nanoseconds apart. Signed
 * nanoTime rollover is supported. A clock moving backwards throws before changing
 * the transition. Long frame gaps catch up to elapsed real time, including while
 * the game is paused. No tasks or completion callbacks are scheduled.</p>
 */
public final class Tween {
    private final LongSupplier clock;
    private double start;
    private double target;
    private double displayed;
    private Easing easing = Easing.LINEAR;
    private long advancedNanos;
    private long elapsedNanos;
    private long durationNanos;
    private long lastReadNanos;
    private boolean clockRead;
    private boolean running;

    public Tween(double initialValue) {
        this(initialValue, System::nanoTime);
    }

    public Tween(double initialValue, LongSupplier monotonicNanos) {
        requireFinite(initialValue, "Initial value");
        clock = Objects.requireNonNull(monotonicNanos, "monotonicNanos");
        settle(initialValue);
    }

    public Tween animateTo(double nextTarget, Duration duration, Easing nextEasing) {
        return animateTo(nextTarget, duration, nextEasing, false);
    }

    /**
     * Retargets from the value displayed at the current time. Calling this with
     * the existing target does not restart an active transition. The supplied
     * duration is the full duration from the current value to the new target.
     * Reduced motion or a zero duration settles immediately.
     */
    public Tween animateTo(
            double nextTarget, Duration duration, Easing nextEasing, boolean reducedMotion) {
        requireFinite(nextTarget, "Target value");
        Objects.requireNonNull(duration, "duration");
        Objects.requireNonNull(nextEasing, "easing");
        if (duration.isNegative()) {
            throw new IllegalArgumentException("Duration must not be negative");
        }
        final long nextDuration;
        try {
            nextDuration = duration.toNanos();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Duration is too long to represent in nanoseconds", exception);
        }

        long now = readClock();
        advance(now);
        if (reducedMotion || nextDuration == 0 || displayed == nextTarget) {
            settle(nextTarget);
        } else if (!running || target != nextTarget) {
            start = displayed;
            target = nextTarget;
            advancedNanos = now;
            elapsedNanos = 0;
            durationNanos = nextDuration;
            easing = nextEasing;
            running = true;
        }
        return this;
    }

    /** Returns the current value, advancing only according to elapsed clock time. */
    public double value() {
        advance(readClock());
        return displayed;
    }

    /** Returns the current destination without reading the clock. */
    public double target() {
        return target;
    }

    /** Advances to the current time before reporting whether this tween is active. */
    public boolean isRunning() {
        advance(readClock());
        return running;
    }

    /** Stops at the current displayed value, for example when a screen is disposed. */
    public void cancel() {
        settle(value());
    }

    /** Immediately replaces the displayed value and destination; no old work remains pending. */
    public void snapTo(double value) {
        requireFinite(value, "Value");
        settle(value);
    }

    private long readClock() {
        long now = clock.getAsLong();
        if (clockRead && now - lastReadNanos < 0) {
            throw new IllegalArgumentException("Clock must be monotonic");
        }
        lastReadNanos = now;
        clockRead = true;
        return now;
    }

    private void advance(long now) {
        if (!running) {
            return;
        }
        long delta = now - advancedNanos;
        advancedNanos = now;
        // Compare before adding so even a near-Long.MAX_VALUE duration cannot overflow.
        if (delta >= durationNanos - elapsedNanos) {
            settle(target);
            return;
        }
        elapsedNanos += delta;
        double progress = easing.apply((double) elapsedNanos / durationNanos);
        // Weighted endpoints avoid overflow from (target - start) for finite extremes.
        displayed = start * (1.0 - progress) + target * progress;
    }

    private void settle(double value) {
        start = value;
        target = value;
        displayed = value;
        durationNanos = 0;
        elapsedNanos = 0;
        running = false;
    }

    private static void requireFinite(double value, String label) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(label + " must be finite");
        }
    }
}
