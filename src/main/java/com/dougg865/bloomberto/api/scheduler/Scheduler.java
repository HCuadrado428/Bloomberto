package com.dougg865.bloomberto.api.scheduler;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs tasks after a number of ticks. The owner calls {@link #tick()} once per game tick
 * (Bloomberto's client and server schedulers will be driven by the loader tick events).
 *
 * <p>Not thread-safe: use it from the thread that ticks it.
 */
public final class Scheduler {
    /** Handle to cancel a scheduled task. */
    public interface Handle {
        void cancel();

        boolean isActive();
    }

    private final class Task implements Handle {
        final Runnable action;
        final int interval; // 0 = one shot
        int remaining;
        boolean active = true;

        Task(Runnable action, int delay, int interval) {
            this.action = action;
            this.remaining = delay;
            this.interval = interval;
        }

        @Override
        public void cancel() {
            active = false;
        }

        @Override
        public boolean isActive() {
            return active;
        }
    }

    private final List<Task> tasks = new ArrayList<>();
    private final List<Task> pending = new ArrayList<>();
    private boolean ticking;

    /** Runs {@code action} once after {@code delayTicks} ticks (0 = on the next tick). */
    public Handle runLater(int delayTicks, Runnable action) {
        return add(new Task(action, Math.max(0, delayTicks), 0));
    }

    /** Runs {@code action} every {@code intervalTicks} ticks, first after {@code delayTicks}. */
    public Handle runRepeating(int delayTicks, int intervalTicks, Runnable action) {
        if (intervalTicks < 1) {
            throw new IllegalArgumentException("intervalTicks must be >= 1");
        }
        return add(new Task(action, Math.max(0, delayTicks), intervalTicks));
    }

    private Handle add(Task task) {
        (ticking ? pending : tasks).add(task);
        return task;
    }

    /** Advances time by one tick and runs every task that is due. */
    public void tick() {
        ticking = true;
        try {
            for (Task task : tasks) {
                if (!task.active) {
                    continue;
                }
                if (task.remaining-- > 0) {
                    continue;
                }
                // Update the state before running so a throwing task is not run twice.
                if (task.interval > 0) {
                    task.remaining = task.interval - 1;
                } else {
                    task.active = false;
                }
                task.action.run();
            }
        } finally {
            ticking = false;
            tasks.removeIf(t -> !t.active);
            tasks.addAll(pending);
            pending.clear();
        }
    }

    public int size() {
        return tasks.size() + pending.size();
    }
}
