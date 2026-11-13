package com.dougg865.bloomberto.api.math;

/** Catmull-Rom spline through a list of values; passes exactly through every point. */
public final class Spline {
    private final float[] points;

    private Spline(float[] points) {
        this.points = points;
    }

    /** @throws IllegalArgumentException with fewer than 2 points */
    public static Spline catmullRom(float... points) {
        if (points.length < 2) {
            throw new IllegalArgumentException("A spline needs at least 2 points");
        }
        return new Spline(points.clone());
    }

    /** Evaluates the spline; {@code t} in [0, 1] covers the whole curve (clamped). */
    public float sample(float t) {
        int segments = points.length - 1;
        float scaled = Interpolation.clamp(t, 0f, 1f) * segments;
        int i = Math.min((int) scaled, segments - 1);
        float local = scaled - i;
        float p0 = points[Math.max(i - 1, 0)];
        float p1 = points[i];
        float p2 = points[i + 1];
        float p3 = points[Math.min(i + 2, points.length - 1)];
        return Interpolation.catmullRom(p0, p1, p2, p3, local);
    }
}
