package com.dougg865.bloomberto.api.render;

import com.dougg865.bloomberto.api.animation.Easing;
import com.dougg865.bloomberto.api.render.post.ShakeModel;

/**
 * Shakes the player's camera (a few degrees of yaw and pitch). Unlike the post-processing {@code Shake} effect this
 * moves the actual view, so it also works while a shader pack (Iris, Oculus, OptiFine) is active. It does not change where
 * the player is aiming: only how the world is drawn.
 *
 * <pre>{@code
 * CameraShake.shared().shake(1f, 30);          // strong shake that fades out over 30 ticks
 * CameraShake.shared().shake(0.4f, 10, 1.5f);  // light shake, at most 1.5 degrees
 * }</pre>
 *
 * A stronger shake replaces a weaker one that is still running; a weaker one is ignored. Client only.
 */
public final class CameraShake {
    private static final CameraShake SHARED = new CameraShake();
    private static final float DEFAULT_MAX_DEGREES = 2.5f;

    private final ShakeModel model = new ShakeModel(11L, 24f);
    private float strength;
    private float maxDegrees = DEFAULT_MAX_DEGREES;
    private int duration;
    private int age;

    /** The instance Bloomberto reads every frame. */
    public static CameraShake shared() {
        return SHARED;
    }

    /** Starts a shake of {@code strength} (0-1) that fades out over {@code ticks} ticks, at most 2.5 degrees. */
    public synchronized CameraShake shake(float strength, int ticks) {
        return shake(strength, ticks, DEFAULT_MAX_DEGREES);
    }

    /** Like {@link #shake(float, int)} with the largest rotation (at strength 1) set to {@code maxDegrees}. */
    public synchronized CameraShake shake(float strength, int ticks, float maxDegrees) {
        float s = Math.max(0f, Math.min(1f, strength));
        if (ticks <= 0 || s <= 0f || s * maxDegrees < currentDegrees(0f)) {
            return this;
        }
        this.strength = s;
        this.maxDegrees = maxDegrees;
        this.duration = ticks;
        this.age = 0;
        return this;
    }

    /** Stops any shake at once. */
    public synchronized void stop() {
        duration = 0;
        age = 0;
        strength = 0f;
    }

    public synchronized boolean isShaking() {
        return duration > 0;
    }

    /** Advances one game tick. Bloomberto calls this; you do not need to. */
    public synchronized void tick() {
        if (duration > 0 && ++age >= duration) {
            stop();
        }
    }

    private float currentDegrees(float partialTick) {
        if (duration <= 0) {
            return 0f;
        }
        float progress = Math.min(1f, (age + partialTick) / duration);
        return strength * maxDegrees * (1f - Easing.OUT_QUAD.apply(progress));
    }

    /** {@code {yawDegrees, pitchDegrees}} to add to the camera now; both 0 when nothing is shaking. */
    public synchronized float[] offset(float partialTick) {
        float amplitude = currentDegrees(partialTick);
        if (amplitude <= 0f) {
            return new float[] {0f, 0f};
        }
        float seconds = (age + partialTick) / 20f;
        return model.offset(seconds, 1f, amplitude);
    }
}
