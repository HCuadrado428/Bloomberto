package com.dougg865.bloomberto.api.animation;

/**
 * The standard easing curves (see easings.net). Every curve maps 0 to 0 and 1 to 1;
 * inputs outside [0, 1] are clamped.
 */
public enum Easing {
    LINEAR(t -> t),

    IN_SINE(t -> 1f - (float) Math.cos(t * Math.PI / 2.0)),
    OUT_SINE(t -> (float) Math.sin(t * Math.PI / 2.0)),
    IN_OUT_SINE(t -> -((float) Math.cos(Math.PI * t) - 1f) / 2f),

    IN_QUAD(t -> t * t),
    OUT_QUAD(t -> 1f - (1f - t) * (1f - t)),
    IN_OUT_QUAD(t -> t < 0.5f ? 2f * t * t : 1f - pow(-2f * t + 2f, 2) / 2f),

    IN_CUBIC(t -> t * t * t),
    OUT_CUBIC(t -> 1f - pow(1f - t, 3)),
    IN_OUT_CUBIC(t -> t < 0.5f ? 4f * t * t * t : 1f - pow(-2f * t + 2f, 3) / 2f),

    IN_QUART(t -> pow(t, 4)),
    OUT_QUART(t -> 1f - pow(1f - t, 4)),
    IN_OUT_QUART(t -> t < 0.5f ? 8f * pow(t, 4) : 1f - pow(-2f * t + 2f, 4) / 2f),

    IN_QUINT(t -> pow(t, 5)),
    OUT_QUINT(t -> 1f - pow(1f - t, 5)),
    IN_OUT_QUINT(t -> t < 0.5f ? 16f * pow(t, 5) : 1f - pow(-2f * t + 2f, 5) / 2f),

    IN_EXPO(t -> t == 0f ? 0f : (float) Math.pow(2.0, 10.0 * t - 10.0)),
    OUT_EXPO(t -> t == 1f ? 1f : 1f - (float) Math.pow(2.0, -10.0 * t)),
    IN_OUT_EXPO(t -> t == 0f ? 0f : t == 1f ? 1f : t < 0.5f
            ? (float) Math.pow(2.0, 20.0 * t - 10.0) / 2f
            : (2f - (float) Math.pow(2.0, -20.0 * t + 10.0)) / 2f),

    IN_CIRC(t -> 1f - (float) Math.sqrt(1f - t * t)),
    OUT_CIRC(t -> (float) Math.sqrt(1f - pow(t - 1f, 2))),
    IN_OUT_CIRC(t -> t < 0.5f
            ? (1f - (float) Math.sqrt(1f - pow(2f * t, 2))) / 2f
            : ((float) Math.sqrt(1f - pow(-2f * t + 2f, 2)) + 1f) / 2f),

    IN_BACK(t -> K.C3 * t * t * t - K.C1 * t * t),
    OUT_BACK(t -> 1f + K.C3 * pow(t - 1f, 3) + K.C1 * pow(t - 1f, 2)),
    IN_OUT_BACK(t -> t < 0.5f
            ? pow(2f * t, 2) * ((K.C2 + 1f) * 2f * t - K.C2) / 2f
            : (pow(2f * t - 2f, 2) * ((K.C2 + 1f) * (t * 2f - 2f) + K.C2) + 2f) / 2f),

    IN_ELASTIC(t -> t == 0f ? 0f : t == 1f ? 1f
            : -(float) Math.pow(2.0, 10.0 * t - 10.0) * (float) Math.sin((t * 10.0 - 10.75) * K.C4)),
    OUT_ELASTIC(t -> t == 0f ? 0f : t == 1f ? 1f
            : (float) Math.pow(2.0, -10.0 * t) * (float) Math.sin((t * 10.0 - 0.75) * K.C4) + 1f),
    IN_OUT_ELASTIC(t -> t == 0f ? 0f : t == 1f ? 1f : t < 0.5f
            ? -((float) Math.pow(2.0, 20.0 * t - 10.0) * (float) Math.sin((20.0 * t - 11.125) * K.C5)) / 2f
            : ((float) Math.pow(2.0, -20.0 * t + 10.0) * (float) Math.sin((20.0 * t - 11.125) * K.C5)) / 2f + 1f),

    IN_BOUNCE(t -> 1f - bounceOut(1f - t)),
    OUT_BOUNCE(Easing::bounceOut),
    IN_OUT_BOUNCE(t -> t < 0.5f
            ? (1f - bounceOut(1f - 2f * t)) / 2f
            : (1f + bounceOut(2f * t - 1f)) / 2f);

    /** Curve constants; kept in a holder class because enum constants cannot forward-reference fields. */
    private static final class K {
        static final float C1 = 1.70158f;
        static final float C2 = C1 * 1.525f;
        static final float C3 = C1 + 1f;
        static final double C4 = 2.0 * Math.PI / 3.0;
        static final double C5 = 2.0 * Math.PI / 4.5;
    }

    @FunctionalInterface
    private interface Curve {
        float apply(float t);
    }

    private final Curve function;

    Easing(Curve function) {
        this.function = function;
    }

    /** Applies the curve. {@code t} is clamped to [0, 1]; the result may overshoot for back/elastic curves. */
    public float apply(float t) {
        if (t <= 0f) {
            return 0f;
        }
        if (t >= 1f) {
            return 1f;
        }
        return function.apply(t);
    }

    private static float pow(float base, int exp) {
        float r = 1f;
        for (int i = 0; i < exp; i++) {
            r *= base;
        }
        return r;
    }

    private static float bounceOut(float t) {
        final float n1 = 7.5625f;
        final float d1 = 2.75f;
        if (t < 1f / d1) {
            return n1 * t * t;
        } else if (t < 2f / d1) {
            float x = t - 1.5f / d1;
            return n1 * x * x + 0.75f;
        } else if (t < 2.5f / d1) {
            float x = t - 2.25f / d1;
            return n1 * x * x + 0.9375f;
        }
        float x = t - 2.625f / d1;
        return n1 * x * x + 0.984375f;
    }
}
