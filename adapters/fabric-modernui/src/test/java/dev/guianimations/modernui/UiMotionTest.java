package dev.guianimations.modernui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;
import org.junit.jupiter.api.Test;

/** Exercises the adapter's lifecycle and scheduling contract without constructing native views. */
class UiMotionTest {
    @Test
    void initialEntryDrawsItsStartingStateAndReachesExactEndpoint() {
        Fixture fixture = new Fixture();
        fixture.motion.enter(220, 8, false);
        assertEquals(0, fixture.frames.alpha);
        assertEquals(8, fixture.frames.translationY);
        assertEquals(1, fixture.frames.pending.size());

        fixture.frameAt(110);
        assertEquals(0.875f, fixture.frames.alpha);
        assertEquals(1f, fixture.frames.translationY);
        fixture.frameAt(220);
        assertEquals(1f, fixture.frames.alpha);
        assertEquals(0f, fixture.frames.translationY);
        assertTrue(fixture.frames.pending.isEmpty());
    }

    @Test
    void reversingAnActiveTransitionPreservesBothVisualPropertiesEvenWithDifferentTravel() {
        Fixture fixture = new Fixture();
        fixture.motion.enter(220, 8, false);
        fixture.frameAt(110);
        float enteringAlpha = fixture.frames.alpha;
        float enteringY = fixture.frames.translationY;

        fixture.motion.exit(140, 16, false, null);
        assertEquals(enteringAlpha, fixture.frames.alpha);
        assertEquals(enteringY, fixture.frames.translationY);
        fixture.frameAt(180);
        float exitingAlpha = fixture.frames.alpha;
        float exitingY = fixture.frames.translationY;
        assertTrue(exitingAlpha < enteringAlpha);
        assertTrue(exitingY > enteringY);

        fixture.motion.enter(220, 40, false);
        assertEquals(exitingAlpha, fixture.frames.alpha, "Re-entering must not snap alpha to zero");
        assertEquals(exitingY, fixture.frames.translationY, "Changed travel must not jump translation");
        assertEquals(1, fixture.frames.pending.size(), "Retargeting replaces the previous queued frame");
        fixture.frameAt(400);
        assertEquals(1f, fixture.frames.alpha);
        assertEquals(0f, fixture.frames.translationY);
    }

    @Test
    void repeatingTheSameEntryPreservesItsDeadline() {
        Fixture fixture = new Fixture();
        fixture.motion.enter(220, 8, false);
        fixture.frameAt(110);
        fixture.motion.enter(220, 8, false);
        assertEquals(0.875f, fixture.frames.alpha);
        fixture.frameAt(220);
        assertEquals(1f, fixture.frames.alpha);
        assertEquals(0f, fixture.frames.translationY);
        assertTrue(fixture.frames.pending.isEmpty());
    }

    @Test
    void reversingExitDiscardsItsCompletionRatherThanClosingAReopenedView() {
        Fixture fixture = new Fixture();
        AtomicInteger obsoleteCompletion = new AtomicInteger();
        AtomicInteger currentCompletion = new AtomicInteger();
        fixture.motion.exit(140, 8, false, obsoleteCompletion::incrementAndGet);
        fixture.frameAt(70);
        fixture.motion.enter(220, 8, false);
        fixture.frameAt(290);
        assertEquals(0, obsoleteCompletion.get());

        fixture.motion.exit(140, 8, false, currentCompletion::incrementAndGet);
        fixture.frameAt(430);
        assertEquals(0, obsoleteCompletion.get());
        assertEquals(1, currentCompletion.get());
        fixture.motion.run();
        fixture.motion.finish();
        assertEquals(1, currentCompletion.get(), "Completion must be consumed once");
    }

    @Test
    void cancellationRemovesQueuedWorkAndIgnoresAnAlreadyCapturedFrame() {
        Fixture fixture = new Fixture();
        AtomicInteger completions = new AtomicInteger();
        fixture.motion.exit(140, 8, false, completions::incrementAndGet);
        Runnable staleFrame = fixture.frames.pending.peekFirst();
        fixture.motion.cancel();
        assertTrue(fixture.frames.pending.isEmpty());
        int writesAtDisposal = fixture.frames.writes;
        fixture.clock.millis(1_000);
        staleFrame.run();
        fixture.motion.finish();
        assertEquals(writesAtDisposal, fixture.frames.writes);
        assertEquals(0, completions.get());
        assertTrue(fixture.frames.pending.isEmpty());
    }

    @Test
    void reducedMotionAtStartSettlesWithoutSchedulingFrames() {
        Fixture fixture = new Fixture();
        fixture.motion.enter(220, 8, true);
        assertEquals(1f, fixture.frames.alpha);
        assertEquals(0f, fixture.frames.translationY);
        assertTrue(fixture.frames.pending.isEmpty());

        AtomicInteger completions = new AtomicInteger();
        fixture.motion.exit(140, 8, true, completions::incrementAndGet);
        assertEquals(0f, fixture.frames.alpha);
        assertEquals(1, completions.get());
        assertTrue(fixture.frames.pending.isEmpty());
    }

    @Test
    void enablingReducedMotionCanFinishAnActiveTransitionImmediately() {
        Fixture fixture = new Fixture();
        fixture.motion.enter(220, 8, false);
        fixture.frameAt(55);
        fixture.motion.finish();
        assertEquals(1f, fixture.frames.alpha);
        assertEquals(0f, fixture.frames.translationY);

        AtomicInteger completions = new AtomicInteger();
        fixture.motion.exit(140, 8, false, completions::incrementAndGet);
        fixture.motion.finish();
        assertEquals(0f, fixture.frames.alpha);
        assertEquals(1, completions.get());
        fixture.motion.finish();
        fixture.clock.millis(1_000);
        fixture.frames.drain();
        assertEquals(1, completions.get());
    }

    @Test
    void completionCanStartAnotherTransitionWithoutDuplicateSchedulingOrStaleWork() {
        Fixture fixture = new Fixture();
        AtomicInteger completions = new AtomicInteger();
        fixture.motion.exit(140, 8, false, () -> {
            completions.incrementAndGet();
            fixture.motion.enter(220, 8, false);
        });
        fixture.frameAt(140);
        assertEquals(1, completions.get());
        assertEquals(0f, fixture.frames.alpha);
        assertEquals(1, fixture.frames.pending.size());
        fixture.frameAt(360);
        assertEquals(1f, fixture.frames.alpha);
        assertEquals(0f, fixture.frames.translationY);
        assertEquals(1, completions.get());
        assertTrue(fixture.frames.pending.isEmpty());
    }

    @Test
    void aCancelledMotionCanBeReusedFromItsCurrentDisplay() {
        Fixture fixture = new Fixture();
        fixture.motion.enter(220, 8, false);
        fixture.frameAt(110);
        fixture.motion.cancel();
        float previousAlpha = fixture.frames.alpha;
        float previousY = fixture.frames.translationY;
        fixture.motion.enter(220, 8, false);
        assertEquals(previousAlpha, fixture.frames.alpha);
        assertEquals(previousY, fixture.frames.translationY);
        fixture.frameAt(330);
        assertEquals(1f, fixture.frames.alpha);
        assertEquals(0f, fixture.frames.translationY);
    }

    @Test
    void completionWaitsForTranslationWhenARepeatedExitChangesItsDistance() {
        Fixture fixture = new Fixture();
        AtomicInteger obsolete = new AtomicInteger();
        AtomicInteger latest = new AtomicInteger();
        fixture.motion.exit(140, 8, false, obsolete::incrementAndGet);
        fixture.frameAt(70);
        float previousY = fixture.frames.translationY;
        fixture.motion.exit(140, 40, false, latest::incrementAndGet);
        assertEquals(previousY, fixture.frames.translationY);
        fixture.frameAt(140);
        assertEquals(0f, fixture.frames.alpha);
        assertEquals(0, latest.get(), "The remaining translation must finish before disposal");
        fixture.frameAt(210);
        assertEquals(40f, fixture.frames.translationY);
        assertEquals(0, obsolete.get());
        assertEquals(1, latest.get());
    }

    private static final class Fixture {
        final FakeClock clock = new FakeClock();
        final FakeFrames frames = new FakeFrames();
        final UiMotion motion = new UiMotion(frames, clock);

        void frameAt(long millis) {
            clock.millis(millis);
            assertFalse(frames.pending.isEmpty(), "A transition frame should be scheduled");
            frames.pending.removeFirst().run();
        }
    }

    private static final class FakeClock implements LongSupplier {
        long nanos;

        void millis(long millis) { nanos = millis * 1_000_000L; }

        @Override public long getAsLong() { return nanos; }
    }

    private static final class FakeFrames implements UiMotion.FrameTarget {
        final Deque<Runnable> pending = new ArrayDeque<>();
        float alpha = 1;
        float translationY;
        int writes;

        @Override public void alpha(float value) { alpha = value; writes++; }
        @Override public void translationY(float value) { translationY = value; writes++; }
        @Override public void postFrame(Runnable frame) { pending.addLast(frame); }
        @Override public void removeFrame(Runnable frame) { pending.removeIf(queued -> queued == frame); }

        void drain() {
            int frames = 0;
            while (!pending.isEmpty()) {
                assertTrue(++frames <= 100, "Completed transitions must not schedule an endless loop");
                pending.removeFirst().run();
            }
        }
    }
}
