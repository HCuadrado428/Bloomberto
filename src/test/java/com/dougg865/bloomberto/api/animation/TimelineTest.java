package com.dougg865.bloomberto.api.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class TimelineTest {
    @Test
    void sequencePlaysInOrder() {
        List<String> log = new ArrayList<>();
        Tween<Float> a = Tween.of(0f, 1f).duration(2).onUpdate(v -> log.add("a"));
        Tween<Float> b = Tween.of(0f, 1f).duration(2).onUpdate(v -> log.add("b"));
        Timeline t = Timeline.sequence(a, b);
        int ticks = 0;
        while (!t.tick()) {
            ticks++;
        }
        assertEquals(List.of("a", "a", "b", "b"), log);
        assertEquals(3, ticks); // 4 ticks total, the 4th returns true
        assertTrue(t.isFinished());
    }

    @Test
    void parallelFinishesWithTheLongest() {
        AtomicInteger a = new AtomicInteger();
        AtomicInteger b = new AtomicInteger();
        Timeline t = Timeline.parallel(
                Tween.of(0f, 1f).duration(2).onUpdate(v -> a.incrementAndGet()),
                Tween.of(0f, 1f).duration(5).onUpdate(v -> b.incrementAndGet()));
        int ticks = 0;
        while (!t.tick()) {
            ticks++;
        }
        assertEquals(4, ticks);
        assertEquals(2, a.get());
        assertEquals(5, b.get());
    }

    @Test
    void onCompleteRunsOnce() {
        AtomicInteger done = new AtomicInteger();
        Timeline t = Timeline.sequence(Tween.of(0f, 1f).duration(1)).onComplete(done::incrementAndGet);
        t.tick();
        t.tick();
        t.tick();
        assertEquals(1, done.get());
    }

    @Test
    void pauseDelaysTheNextAnimation() {
        List<Integer> log = new ArrayList<>();
        int[] tick = {0};
        Timeline t = Timeline.sequence(Timeline.pause(3), Tween.of(0f, 1f).duration(1).onUpdate(v -> log.add(tick[0])));
        while (!t.isFinished()) {
            tick[0]++;
            t.tick();
        }
        assertEquals(List.of(4), log);
    }

    @Test
    void emptyTimelineIsFinished() {
        assertTrue(Timeline.sequence().isFinished());
        assertTrue(Timeline.parallel().tick());
    }

    @Test
    void resetReplaysTheWholeTimeline() {
        AtomicInteger n = new AtomicInteger();
        Timeline t = Timeline.sequence(Tween.of(0f, 1f).duration(2).onUpdate(v -> n.incrementAndGet()));
        while (!t.tick()) {
            // run to the end
        }
        t.reset();
        assertFalse(t.isFinished());
        while (!t.tick()) {
            // run again
        }
        assertEquals(4, n.get());
    }

    @Test
    void keyframesInterpolateBetweenPoints() {
        KeyframeTrack track = KeyframeTrack.create().at(0, 0f).at(10, 10f).at(20, 0f);
        assertEquals(0f, track.sample(0f), 1e-6f);
        assertEquals(5f, track.sample(5f), 1e-6f);
        assertEquals(10f, track.sample(10f), 1e-6f);
        assertEquals(5f, track.sample(15f), 1e-6f);
        assertEquals(0f, track.sample(99f), 1e-6f);
        assertEquals(0f, track.sample(-4f), 1e-6f);
        assertEquals(20, track.length());
    }

    @Test
    void keyframeEasingShapesTheSegmentEndingAtTheKeyframe() {
        KeyframeTrack track = KeyframeTrack.create().at(0, 0f).at(2, 1f, Easing.IN_QUAD);
        assertEquals(0.25f, track.sample(1f), 1e-6f);
    }

    @Test
    void keyframesMustBeOrdered() {
        assertThrows(IllegalArgumentException.class, () -> KeyframeTrack.create().at(5, 0f).at(5, 1f));
    }

    @Test
    void keyframeTrackPlaysAsAnimation() {
        List<Float> values = new ArrayList<>();
        AtomicInteger done = new AtomicInteger();
        KeyframeTrack track = KeyframeTrack.create().at(0, 0f).at(4, 4f).onUpdate(values::add).onComplete(done::incrementAndGet);
        while (!track.tick()) {
            // run to the end
        }
        assertEquals(List.of(1f, 2f, 3f, 4f), values);
        assertEquals(1, done.get());
    }
}
