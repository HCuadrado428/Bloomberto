package com.dougg865.bloomberto.api.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A float value animated through keyframes placed at tick positions. The easing given with a
 * keyframe shapes the segment that ends at that keyframe.
 *
 * <pre>{@code
 * KeyframeTrack.create().at(0, 0f).at(10, 1f, Easing.OUT_BACK).at(30, 0f).onUpdate(v -> scale = v).start();
 * }</pre>
 */
public final class KeyframeTrack implements Animation {
    private record Key(int tick, float value, Easing easing) { }

    private final List<Key> keys = new ArrayList<>();
    private Consumer<Float> onUpdate = v -> { };
    private Runnable onComplete = () -> { };
    private int time;
    private boolean finished;

    private KeyframeTrack() {
    }

    public static KeyframeTrack create() {
        return new KeyframeTrack();
    }

    public KeyframeTrack at(int tick, float value) {
        return at(tick, value, Easing.LINEAR);
    }

    /** Keyframes must be added in increasing tick order. */
    public KeyframeTrack at(int tick, float value, Easing easing) {
        if (!keys.isEmpty() && tick <= keys.get(keys.size() - 1).tick()) {
            throw new IllegalArgumentException("Keyframes must be in increasing tick order");
        }
        keys.add(new Key(tick, value, easing));
        return this;
    }

    public KeyframeTrack onUpdate(Consumer<Float> onUpdate) {
        this.onUpdate = onUpdate;
        return this;
    }

    public KeyframeTrack onComplete(Runnable onComplete) {
        this.onComplete = onComplete;
        return this;
    }

    public KeyframeTrack start() {
        return start(TweenManager.shared());
    }

    public KeyframeTrack start(TweenManager manager) {
        reset();
        manager.add(this);
        return this;
    }

    /** The track's value at a tick; before the first keyframe and after the last it holds the end values. */
    public float sample(float tick) {
        if (keys.isEmpty()) {
            throw new IllegalStateException("No keyframes");
        }
        Key first = keys.get(0);
        if (tick <= first.tick()) {
            return first.value();
        }
        for (int i = 1; i < keys.size(); i++) {
            Key a = keys.get(i - 1);
            Key b = keys.get(i);
            if (tick <= b.tick()) {
                float t = (tick - a.tick()) / (float) (b.tick() - a.tick());
                return a.value() + (b.value() - a.value()) * b.easing().apply(t);
            }
        }
        return keys.get(keys.size() - 1).value();
    }

    /** Length in ticks (the position of the last keyframe). */
    public int length() {
        return keys.isEmpty() ? 0 : keys.get(keys.size() - 1).tick();
    }

    @Override
    public boolean tick() {
        if (finished) {
            return true;
        }
        if (keys.isEmpty()) {
            finished = true;
            return true;
        }
        time++;
        onUpdate.accept(sample(time));
        if (time >= length()) {
            finished = true;
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
        time = 0;
        finished = false;
    }
}
