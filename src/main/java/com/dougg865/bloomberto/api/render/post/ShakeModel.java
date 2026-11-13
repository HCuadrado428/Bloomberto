package com.dougg865.bloomberto.api.render.post;

import com.dougg865.bloomberto.api.math.Noise;

/**
 * Computes the screen offset of a camera shake: smooth random movement scaled by the current strength.
 * Pure math, so it can be tested without a game.
 */
public final class ShakeModel {
    private final Noise noise;
    private final float frequency;

    /**
     * @param seed      different seeds give different shake patterns
     * @param frequency how fast the shake moves, in noise units per second (10-30 feels like an impact)
     */
    public ShakeModel(long seed, float frequency) {
        this.noise = new Noise(seed);
        this.frequency = frequency;
    }

    /**
     * @param seconds  time since the shake started (or any increasing clock)
     * @param strength 0-1
     * @param maxOffset the largest offset, as a fraction of the screen (0.02 = 2%)
     * @return {@code {x, y}} offsets in screen fractions, each within {@code +-strength * maxOffset}
     */
    public float[] offset(float seconds, float strength, float maxOffset) {
        float t = seconds * frequency;
        float amplitude = Math.max(0f, Math.min(1f, strength)) * maxOffset;
        return new float[] {noise.sample(t, 0.5f) * amplitude, noise.sample(0.5f, t + 31.7f) * amplitude};
    }
}
