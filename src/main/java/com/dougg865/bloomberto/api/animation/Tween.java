package com.dougg865.bloomberto.api.animation;

import com.dougg865.bloomberto.api.color.Color;
import java.util.function.Consumer;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Animates a value from one state to another over a number of ticks.
 *
 * <pre>{@code
 * Tween.of(0f, 1f).duration(20).ease(Easing.OUT_CUBIC)
 *      .onUpdate(v -> alpha = v).onComplete(this::hide).start();
 * }</pre>
 *
 * <p>Tweens are driven by calling {@link #tick()}; {@link #start()} registers the tween with the
 * shared {@link TweenManager}, which Bloomberto ticks every client tick.
 */
public final class Tween<T> implements Animation {
    private final T from;
    private final T to;
    private final Interpolator<T> interpolator;

    private int duration = 20;
    private int delay;
    private int delayLeft;
    private int repeats; // extra plays; -1 = forever
    private boolean yoyo;
    private Easing easing = Easing.LINEAR;
    private Consumer<T> onUpdate = v -> { };
    private Runnable onComplete = () -> { };

    private int elapsed;
    private int plays;
    private boolean finished;

    private Tween(T from, T to, Interpolator<T> interpolator) {
        this.from = from;
        this.to = to;
        this.interpolator = interpolator;
    }

    public static Tween<Float> of(float from, float to) {
        return new Tween<>(from, to, Interpolator.FLOAT);
    }

    public static Tween<Vector3f> of(Vector3f from, Vector3f to) {
        return new Tween<>(new Vector3f(from), new Vector3f(to), Interpolator.VECTOR3F);
    }

    public static Tween<Quaternionf> of(Quaternionf from, Quaternionf to) {
        return new Tween<>(new Quaternionf(from), new Quaternionf(to), Interpolator.QUATERNIONF);
    }

    public static Tween<Color> of(Color from, Color to) {
        return new Tween<>(from, to, Interpolator.COLOR);
    }

    /** Tween for any custom type. */
    public static <T> Tween<T> of(T from, T to, Interpolator<T> interpolator) {
        return new Tween<>(from, to, interpolator);
    }

    /** Length of one play, in ticks (at least 1). */
    public Tween<T> duration(int ticks) {
        this.duration = Math.max(1, ticks);
        return this;
    }

    /** Ticks to wait before starting. */
    public Tween<T> delay(int ticks) {
        this.delay = Math.max(0, ticks);
        this.delayLeft = this.delay;
        return this;
    }

    public Tween<T> ease(Easing easing) {
        this.easing = easing;
        return this;
    }

    /** Plays {@code extraPlays} more times after the first; {@code -1} loops forever. */
    public Tween<T> repeat(int extraPlays) {
        this.repeats = Math.max(-1, extraPlays);
        return this;
    }

    /** Alternate plays run backwards. */
    public Tween<T> yoyo(boolean yoyo) {
        this.yoyo = yoyo;
        return this;
    }

    /** Called every tick with the current value (also on the first tick after the delay). */
    public Tween<T> onUpdate(Consumer<T> onUpdate) {
        this.onUpdate = onUpdate;
        return this;
    }

    /** Called once when the tween finishes (never for tweens that repeat forever). */
    public Tween<T> onComplete(Runnable onComplete) {
        this.onComplete = onComplete;
        return this;
    }

    /** Registers this tween with the shared manager. */
    public Tween<T> start() {
        return start(TweenManager.shared());
    }

    public Tween<T> start(TweenManager manager) {
        reset();
        manager.add(this);
        return this;
    }

    /** The value at {@code progress} (0-1) along the current play, with easing applied. */
    public T valueAt(float progress) {
        return interpolator.interpolate(from, to, easing.apply(progress));
    }

    @Override
    public boolean tick() {
        if (finished) {
            return true;
        }
        if (delayLeft > 0) {
            delayLeft--;
            return false;
        }
        elapsed++;
        boolean backwards = yoyo && (plays & 1) == 1;
        float progress = (float) elapsed / duration;
        onUpdate.accept(valueAt(backwards ? 1f - progress : progress));
        if (elapsed >= duration) {
            if (repeats == -1 || plays < repeats) {
                plays++;
                elapsed = 0;
            } else {
                finished = true;
                onComplete.run();
            }
        }
        return finished;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public void reset() {
        elapsed = 0;
        plays = 0;
        delayLeft = delay;
        finished = false;
    }
}
