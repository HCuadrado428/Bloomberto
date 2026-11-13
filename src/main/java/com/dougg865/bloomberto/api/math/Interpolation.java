package com.dougg865.bloomberto.api.math;

/** Scalar interpolation helpers. */
public final class Interpolation {
    private Interpolation() {
    }

    public static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /** Where {@code v} lies between {@code a} and {@code b} (0 at a, 1 at b); 0 if {@code a == b}. */
    public static float inverseLerp(float a, float b, float v) {
        return a == b ? 0f : (v - a) / (b - a);
    }

    /** Maps {@code v} from the range [inMin, inMax] to [outMin, outMax] without clamping. */
    public static float remap(float v, float inMin, float inMax, float outMin, float outMax) {
        return lerp(outMin, outMax, inverseLerp(inMin, inMax, v));
    }

    public static float smoothstep(float t) {
        float x = clamp(t, 0f, 1f);
        return x * x * (3f - 2f * x);
    }

    public static float smootherstep(float t) {
        float x = clamp(t, 0f, 1f);
        return x * x * x * (x * (x * 6f - 15f) + 10f);
    }

    /** Cubic Bezier with control values p0..p3 at parameter t in [0, 1]. */
    public static float cubicBezier(float p0, float p1, float p2, float p3, float t) {
        float u = 1f - t;
        return u * u * u * p0 + 3f * u * u * t * p1 + 3f * u * t * t * p2 + t * t * t * p3;
    }

    /** Catmull-Rom segment between p1 and p2 (p0 and p3 are the neighbours), t in [0, 1]. */
    public static float catmullRom(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        return 0.5f * ((2f * p1) + (-p0 + p2) * t + (2f * p0 - 5f * p1 + 4f * p2 - p3) * t2
                + (-p0 + 3f * p1 - 3f * p2 + p3) * t3);
    }
}
