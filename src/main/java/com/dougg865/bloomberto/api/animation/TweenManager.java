package com.dougg865.bloomberto.api.animation;

import java.util.ArrayList;
import java.util.List;

/** Ticks a set of animations and drops them when they finish. */
public final class TweenManager {
    private static final TweenManager SHARED = new TweenManager();

    private final List<Animation> active = new ArrayList<>();
    private final List<Animation> pending = new ArrayList<>();
    private boolean ticking;

    /** The manager used by {@link Tween#start()}; ticked by Bloomberto every client tick. */
    public static TweenManager shared() {
        return SHARED;
    }

    public void add(Animation animation) {
        (ticking ? pending : active).add(animation);
    }

    public void remove(Animation animation) {
        active.remove(animation);
        pending.remove(animation);
    }

    public void tick() {
        ticking = true;
        try {
            active.removeIf(animation -> {
                try {
                    return animation.tick();
                } catch (RuntimeException e) {
                    // drop the broken animation so it does not fail again every tick or hold up the others
                    com.dougg865.bloomberto.Bloomberto.LOGGER.error("Animation failed and was removed", e);
                    return true;
                }
            });
        } finally {
            ticking = false;
            active.addAll(pending);
            pending.clear();
        }
    }

    public int size() {
        return active.size() + pending.size();
    }

    public void clear() {
        active.clear();
        pending.clear();
    }
}
