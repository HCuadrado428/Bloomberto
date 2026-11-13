package com.dougg865.bloomberto.api.animation;

import java.util.ArrayList;
import java.util.List;

/**
 * Combines animations: {@link #sequence} plays them one after another, {@link #parallel} plays
 * them at the same time and finishes when the last one does.
 */
public final class Timeline implements Animation {
    private final List<Animation> children;
    private final boolean parallel;
    private int current;
    private boolean finished;
    private Runnable onComplete = () -> { };

    private Timeline(List<Animation> children, boolean parallel) {
        this.children = children;
        this.parallel = parallel;
        this.finished = children.isEmpty();
    }

    public static Timeline sequence(Animation... animations) {
        return new Timeline(new ArrayList<>(List.of(animations)), false);
    }

    public static Timeline parallel(Animation... animations) {
        return new Timeline(new ArrayList<>(List.of(animations)), true);
    }

    /** An animation that does nothing for {@code ticks} ticks; useful as a gap in a sequence. */
    public static Animation pause(int ticks) {
        return Tween.of(0f, 1f).duration(ticks);
    }

    public Timeline onComplete(Runnable onComplete) {
        this.onComplete = onComplete;
        return this;
    }

    /** Registers this timeline with the shared manager. */
    public Timeline start() {
        return start(TweenManager.shared());
    }

    public Timeline start(TweenManager manager) {
        reset();
        manager.add(this);
        return this;
    }

    @Override
    public boolean tick() {
        if (finished) {
            return true;
        }
        if (parallel) {
            boolean allDone = true;
            for (Animation a : children) {
                if (!a.isFinished()) {
                    allDone &= a.tick();
                }
            }
            finished = allDone;
        } else {
            if (children.get(current).tick()) {
                current++;
                finished = current >= children.size();
            }
        }
        if (finished) {
            onComplete.run();
        }
        return finished;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public void reset() {
        for (Animation a : children) {
            a.reset();
        }
        current = 0;
        finished = children.isEmpty();
    }
}
