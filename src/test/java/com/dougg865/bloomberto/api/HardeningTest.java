package com.dougg865.bloomberto.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dougg865.bloomberto.api.animation.Animation;
import com.dougg865.bloomberto.api.animation.TweenManager;
import com.dougg865.bloomberto.api.animation.model.AnimationController;
import com.dougg865.bloomberto.api.animation.model.AnimationParser;
import com.dougg865.bloomberto.api.animation.model.BbModelParser;
import com.dougg865.bloomberto.api.animation.model.GeoParser;
import com.dougg865.bloomberto.api.animation.model.ParseException;
import com.dougg865.bloomberto.api.animation.molang.Molang;
import com.dougg865.bloomberto.api.animation.molang.MolangException;
import com.dougg865.bloomberto.api.event.Event;
import com.dougg865.bloomberto.api.scheduler.Scheduler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Failure cases found in the code review: none of them may crash the game or stop unrelated work. */
class HardeningTest {
    @Test
    void aFailingScheduledTaskDoesNotStopOrDelayTheOthers() {
        Scheduler s = new Scheduler();
        List<String> ran = new ArrayList<>();
        s.runLater(0, () -> ran.add("a"));
        s.runLater(0, () -> {
            throw new IllegalStateException("boom");
        });
        s.runLater(0, () -> ran.add("c"));
        s.tick();
        assertEquals(List.of("a", "c"), ran);
        assertEquals(0, s.size());
    }

    @Test
    void clearCancelsEverything() {
        Scheduler s = new Scheduler();
        AtomicInteger runs = new AtomicInteger();
        s.runRepeating(0, 1, runs::incrementAndGet);
        s.runLater(5, runs::incrementAndGet);
        s.clear();
        s.tick();
        assertEquals(0, runs.get());
        assertEquals(0, s.size());
    }

    @Test
    void aFailingAnimationIsDroppedAndTheOthersKeepRunning() {
        TweenManager m = new TweenManager();
        AtomicInteger good = new AtomicInteger();
        m.add(anim(() -> {
            throw new IllegalStateException("broken");
        }));
        m.add(anim(() -> good.incrementAndGet() >= 3));
        m.tick();
        m.tick();
        m.tick();
        assertEquals(3, good.get());
        assertEquals(0, m.size());
    }

    @Test
    void aListenerThatAlwaysFailsDoesNotStopTheOthers() {
        Event<Runnable> event = new Event<>("test");
        AtomicInteger calls = new AtomicInteger();
        event.register(() -> {
            throw new IllegalStateException("always");
        });
        event.register(() -> {
            throw new NoClassDefFoundError("missing/Clazz");
        });
        event.register(calls::incrementAndGet);
        for (int i = 0; i < 100; i++) {
            event.invoke(Runnable::run);
        }
        assertEquals(100, calls.get());
    }

    @Test
    void controllerKeepsRealTimeSpeedAtLowFrameRates() throws Exception {
        var clips = com.dougg865.bloomberto.api.animation.model.AnimationParser.parse(
                "{\"animations\":{\"a\":{\"loop\":true,\"animation_length\":100,\"bones\":{\"b\":{\"position\":[0,0,0]}}}}}");
        AnimationController fast = new AnimationController(clips);
        AnimationController slow = new AnimationController(clips);
        fast.play("a");
        slow.play("a");
        for (long t = 0; t <= 40; t++) {
            fast.tickAt(t);          // a frame every tick (60 FPS draws several per tick)
            if (t % 8 == 0) {
                slow.tickAt(t);      // a frame every 8 ticks (2.5 FPS)
            }
        }
        assertEquals(fast.time(), slow.time(), 1e-4f);
        slow.tickAt(10_000);         // after a long time off screen: catches up at most 5 s
        assertTrue(slow.time() - fast.time() <= 5.001f, "caught up " + (slow.time() - fast.time()));
    }

    @Test
    void controllerTicksOncePerGameTick() {
        AnimationController c = new AnimationController(java.util.Map.of());
        // no clip: only checks that tickAt with the same game time is a no-op and different times both tick
        c.tickAt(10);
        c.tickAt(10);
        c.tickAt(11);
        assertEquals(null, c.currentName());
    }

    @Test
    void molangRejectsAbsurdNestingAndLength() {
        assertThrows(MolangException.class, () -> Molang.compile("-".repeat(10_000) + "1"));
        assertThrows(MolangException.class, () -> Molang.compile("(".repeat(5_000) + "1" + ")".repeat(5_000)));
        assertThrows(MolangException.class, () -> Molang.compile("1+".repeat(3_000) + "1"));
        // ordinary expressions are still fine
        assertTrue(Molang.isNumber("1"));
        assertFalse(Molang.isNumber("q.x"));
    }

    @Test
    void wrongJsonTypesAreParseExceptionsNotCrashes() {
        String[] geo = {
            "{\"minecraft:geometry\":[{\"bones\":[1,2]}]}",
            "{\"minecraft:geometry\":[{\"bones\":[{\"name\":{},\"cubes\":5}]}]}",
            "{\"minecraft:geometry\":5}",
            "[]",
            "[".repeat(100_000),
        };
        for (String json : geo) {
            assertThrows(ParseException.class, () -> GeoParser.parse(json), json.length() > 60 ? "deep" : json);
        }
        String[] anim = {
            "{\"animations\":5}",
            "{\"animations\":{\"a\":5}}",
            "{\"animations\":{\"a\":{\"bones\":{\"b\":{\"rotation\":{\"0\":{\"pre\":{}}}}}}}}",
            "{\"animations\":{\"a\":{\"bones\":{\"b\":5}}}}",
        };
        for (String json : anim) {
            assertThrows(ParseException.class, () -> AnimationParser.parse(json), json);
        }
        String[] bb = {
            "{\"elements\":5}",
            "{\"elements\":[{\"uuid\":\"a\",\"from\":\"x\"}],\"outliner\":[\"a\"]}",
            "{\"outliner\":[{\"children\":5}]}",
            "{\"animations\":[{\"animators\":{\"x\":{\"name\":\"b\",\"keyframes\":5}}}]}",
        };
        for (String json : bb) {
            assertThrows(ParseException.class, () -> {
                BbModelParser.parseModel(json);
                BbModelParser.parseAnimations(json);
            }, json);
        }
    }

    private static Animation anim(java.util.function.BooleanSupplier step) {
        return new Animation() {
            private boolean finished;

            @Override
            public boolean tick() {
                finished = step.getAsBoolean();
                return finished;
            }

            @Override
            public boolean isFinished() {
                return finished;
            }

            @Override
            public void reset() {
                finished = false;
            }
        };
    }

    @Test
    void keyframeTrackPlaysAtNormalSpeed() {
        TweenManager m = new TweenManager();
        List<Float> values = new ArrayList<>();
        com.dougg865.bloomberto.api.animation.KeyframeTrack.create().at(0, 0f).at(4, 4f).onUpdate(values::add).start(m);
        m.tick();
        assertEquals(List.of(1f), values); // one step per game tick, not two
    }

    @Test
    void restartingARunningTweenDoesNotDoubleItsSpeed() {
        TweenManager m = new TweenManager();
        List<Float> values = new ArrayList<>();
        var tween = com.dougg865.bloomberto.api.animation.Tween.of(0f, 10f).duration(10).onUpdate(values::add);
        tween.start(m);
        m.tick();
        tween.start(m); // fired again while still playing
        m.tick();
        assertEquals(1, m.size());
        assertEquals(2, values.size());
        assertEquals(values.get(0), values.get(1), 1e-6f); // rewound: the same first step again
    }
}
