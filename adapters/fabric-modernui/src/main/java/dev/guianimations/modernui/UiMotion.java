package dev.guianimations.modernui;

import dev.guianimations.core.Easing;
import dev.guianimations.core.Tween;
import icyllis.modernui.view.View;

import java.time.Duration;
import java.util.function.LongSupplier;

/** One elapsed-time transition for a view. Cancellation removes scheduled UI work. */
public final class UiMotion implements Runnable {
    /** Small scheduling seam for deterministic motion tests without a GPU or UI thread. */
    public interface FrameTarget {
        void alpha(float alpha);
        void translationY(float translation);
        void postFrame(Runnable frame);
        void removeFrame(Runnable frame);
    }

    private final FrameTarget target;
    private final Tween progress;
    private final Tween translation;
    private Runnable onComplete;
    private boolean cancelled;
    private boolean exit;
    private float distance;
    private boolean started;

    public UiMotion(View view) {
        this(new FrameTarget() {
            @Override public void alpha(float alpha) { view.setAlpha(alpha); }
            @Override public void translationY(float offset) { view.setTranslationY(offset); }
            @Override public void postFrame(Runnable frame) { view.postOnAnimation(frame); }
            @Override public void removeFrame(Runnable frame) { view.removeCallbacks(frame); }
        }, System::nanoTime);
    }

    public UiMotion(FrameTarget target, LongSupplier clock) {
        this.target = target;
        progress = new Tween(1, clock);
        translation = new Tween(0, clock);
    }

    public void enter(long durationMs, float distance, boolean reducedMotion) {
        start(false, durationMs, distance, reducedMotion, null);
    }

    public void exit(long durationMs, float distance, boolean reducedMotion, Runnable onComplete) {
        start(true, durationMs, distance, reducedMotion, onComplete);
    }

    private void start(boolean exit, long durationMs, float distance, boolean reducedMotion, Runnable complete) {
        target.removeFrame(this);
        this.exit = exit;
        this.distance = distance;
        this.onComplete = complete;
        this.cancelled = false;
        if (!started && !exit) {
            progress.snapTo(0);
            translation.snapTo(distance);
        }
        started = true;
        progress.animateTo(exit ? 0 : 1, Duration.ofMillis(durationMs), exit ? Easing.CUBIC_IN : Easing.CUBIC_OUT, reducedMotion);
        translation.animateTo(exit ? distance : 0, Duration.ofMillis(durationMs), exit ? Easing.CUBIC_IN : Easing.CUBIC_OUT, reducedMotion);
        run();
    }

    @Override public void run() {
        if (cancelled) return;
        float value = (float) progress.value();
        target.alpha(value);
        target.translationY((float) translation.value());
        if (progress.isRunning() || translation.isRunning()) target.postFrame(this);
        else {
            Runnable done = onComplete;
            onComplete = null;
            if (done != null) done.run();
        }
    }

    public void finish() {
        progress.snapTo(exit ? 0 : 1);
        translation.snapTo(exit ? distance : 0);
        run();
    }

    public void cancel() {
        cancelled = true;
        progress.cancel();
        translation.cancel();
        onComplete = null;
        target.removeFrame(this);
    }
}
