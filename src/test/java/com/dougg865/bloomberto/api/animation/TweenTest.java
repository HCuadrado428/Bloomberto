package com.dougg865.bloomberto.api.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dougg865.bloomberto.api.color.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class TweenTest {
    private static final float EPS = 1e-5f;

    private static List<Float> run(Tween<Float> tween, int maxTicks) {
        List<Float> values = new ArrayList<>();
        tween.onUpdate(values::add);
        for (int i = 0; i < maxTicks && !tween.tick(); i++) {
            // keep ticking until finished
        }
        return values;
    }

    @Test
    void floatTweenReachesTargetAfterDuration() {
        AtomicInteger done = new AtomicInteger();
        Tween<Float> tween = Tween.of(0f, 10f).duration(4).onComplete(done::incrementAndGet);
        List<Float> values = new ArrayList<>();
        tween.onUpdate(values::add);
        for (int i = 0; i < 10; i++) {
            tween.tick();
        }
        assertEquals(List.of(2.5f, 5f, 7.5f, 10f), values);
        assertEquals(1, done.get());
        assertTrue(tween.isFinished());
    }

    @Test
    void easingIsApplied() {
        List<Float> values = run(Tween.of(0f, 1f).duration(2).ease(Easing.IN_QUAD), 5);
        assertEquals(0.25f, values.get(0), EPS);
        assertEquals(1f, values.get(1), EPS);
    }

    @Test
    void delayPostponesStart() {
        Tween<Float> tween = Tween.of(0f, 1f).duration(1).delay(2);
        List<Float> values = new ArrayList<>();
        tween.onUpdate(values::add);
        assertFalse(tween.tick());
        assertFalse(tween.tick());
        assertTrue(values.isEmpty());
        assertTrue(tween.tick());
        assertEquals(List.of(1f), values);
    }

    @Test
    void repeatAndYoyo() {
        List<Float> values = run(Tween.of(0f, 1f).duration(2).repeat(1).yoyo(true), 10);
        assertEquals(List.of(0.5f, 1f, 0.5f, 0f), values);
    }

    @Test
    void repeatForeverNeverCompletes() {
        AtomicInteger done = new AtomicInteger();
        Tween<Float> tween = Tween.of(0f, 1f).duration(1).repeat(-1).onComplete(done::incrementAndGet);
        for (int i = 0; i < 50; i++) {
            assertFalse(tween.tick());
        }
        assertEquals(0, done.get());
    }

    @Test
    void resetReplaysIncludingDelay() {
        Tween<Float> tween = Tween.of(0f, 1f).duration(1).delay(1);
        tween.tick();
        tween.tick();
        assertTrue(tween.isFinished());
        tween.reset();
        assertFalse(tween.isFinished());
        assertFalse(tween.tick());
        assertTrue(tween.tick());
    }

    @Test
    void vectorTweenInterpolatesAndDoesNotAliasInputs() {
        Vector3f from = new Vector3f(0, 0, 0);
        Vector3f to = new Vector3f(10, 20, 30);
        List<Vector3f> values = new ArrayList<>();
        Tween<Vector3f> tween = Tween.of(from, to).duration(2).onUpdate(values::add);
        from.set(99, 99, 99);
        tween.tick();
        tween.tick();
        assertEquals(new Vector3f(5, 10, 15), values.get(0));
        assertEquals(new Vector3f(10, 20, 30), values.get(1));
        assertTrue(values.get(0) != values.get(1));
    }

    @Test
    void quaternionTweenTakesTheShortestPath() {
        Quaternionf from = new Quaternionf();
        Quaternionf to = new Quaternionf().rotateY((float) Math.PI / 2f);
        List<Quaternionf> values = new ArrayList<>();
        Tween<Quaternionf> tween = Tween.of(from, to).duration(2).onUpdate(values::add);
        tween.tick();
        tween.tick();
        Quaternionf mid = values.get(0);
        assertEquals((float) Math.PI / 4f, 2f * (float) Math.asin(mid.y), 1e-4f);
        assertTrue(values.get(1).equals(to, 1e-5f));
    }

    @Test
    void colorTween() {
        List<Color> values = new ArrayList<>();
        Tween<Color> tween = Tween.of(Color.BLACK, Color.WHITE).duration(2).onUpdate(values::add);
        tween.tick();
        tween.tick();
        assertEquals(Color.ofRgb(128, 128, 128), values.get(0));
        assertEquals(Color.WHITE, values.get(1));
    }

    @Test
    void managerRunsAndDropsFinishedTweens() {
        TweenManager manager = new TweenManager();
        AtomicInteger done = new AtomicInteger();
        Tween.of(0f, 1f).duration(2).onComplete(done::incrementAndGet).start(manager);
        assertEquals(1, manager.size());
        manager.tick();
        assertEquals(1, manager.size());
        manager.tick();
        assertEquals(0, manager.size());
        assertEquals(1, done.get());
    }

    @Test
    void tweenStartedFromACallbackWaitsForNextManagerTick() {
        TweenManager manager = new TweenManager();
        AtomicInteger second = new AtomicInteger();
        Tween.of(0f, 1f).duration(1).onComplete(
                () -> Tween.of(0f, 1f).duration(1).onUpdate(v -> second.incrementAndGet()).start(manager)).start(manager);
        manager.tick();
        assertEquals(0, second.get());
        manager.tick();
        assertEquals(1, second.get());
    }
}
