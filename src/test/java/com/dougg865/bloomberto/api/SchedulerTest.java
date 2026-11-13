package com.dougg865.bloomberto.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dougg865.bloomberto.api.scheduler.Scheduler;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SchedulerTest {
    @Test
    void runsOnceAfterDelay() {
        Scheduler s = new Scheduler();
        AtomicInteger n = new AtomicInteger();
        s.runLater(2, n::incrementAndGet);
        s.tick();
        s.tick();
        assertEquals(0, n.get());
        s.tick();
        assertEquals(1, n.get());
        s.tick();
        assertEquals(1, n.get());
        assertEquals(0, s.size());
    }

    @Test
    void zeroDelayRunsOnNextTick() {
        Scheduler s = new Scheduler();
        AtomicInteger n = new AtomicInteger();
        s.runLater(0, n::incrementAndGet);
        assertEquals(0, n.get());
        s.tick();
        assertEquals(1, n.get());
    }

    @Test
    void repeatingRunsEveryInterval() {
        Scheduler s = new Scheduler();
        AtomicInteger n = new AtomicInteger();
        s.runRepeating(1, 3, n::incrementAndGet);
        for (int i = 0; i < 8; i++) {
            s.tick();
        }
        // ticks 1..8 (0-based: 0..7) -> runs at 1, 4, 7
        assertEquals(3, n.get());
    }

    @Test
    void cancelStopsTask() {
        Scheduler s = new Scheduler();
        AtomicInteger n = new AtomicInteger();
        Scheduler.Handle h = s.runRepeating(0, 1, n::incrementAndGet);
        s.tick();
        h.cancel();
        assertFalse(h.isActive());
        s.tick();
        s.tick();
        assertEquals(1, n.get());
        assertEquals(0, s.size());
    }

    @Test
    void taskScheduledWhileTickingWaitsForNextTick() {
        Scheduler s = new Scheduler();
        AtomicInteger n = new AtomicInteger();
        s.runLater(0, () -> s.runLater(0, n::incrementAndGet));
        s.tick();
        assertEquals(0, n.get());
        s.tick();
        assertEquals(1, n.get());
    }

    @Test
    void failingOneShotIsNotRepeated() {
        Scheduler s = new Scheduler();
        AtomicInteger n = new AtomicInteger();
        s.runLater(0, () -> {
            n.incrementAndGet();
            throw new IllegalStateException("boom");
        });
        assertThrows(IllegalStateException.class, s::tick);
        s.tick();
        assertEquals(1, n.get());
    }

    @Test
    void rejectsBadInterval() {
        assertThrows(IllegalArgumentException.class, () -> new Scheduler().runRepeating(0, 0, () -> { }));
    }
}
