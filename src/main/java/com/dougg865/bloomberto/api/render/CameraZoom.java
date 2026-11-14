package com.dougg865.bloomberto.api.render;

import com.dougg865.bloomberto.api.animation.Easing;

/**
 * Zooms the player's camera by changing the field of view (a real zoom, unlike the screen-space {@code Zoom} post effect),
 * so it works with shader packs too. A factor of 2 shows the world twice as large; 1 is no zoom.
 *
 * <pre>{@code
 * CameraZoom.shared().zoomTo(3f, 8, Easing.OUT_CUBIC);  // spyglass-like zoom in 8 ticks
 * CameraZoom.shared().reset(8, Easing.OUT_CUBIC);        // back to normal
 * }</pre>
 *
 * Client only.
 */
public final class CameraZoom {
    private static final CameraZoom SHARED = new CameraZoom();

    private float from = 1f;
    private float to = 1f;
    private int duration;
    private int age;
    private Easing easing = Easing.LINEAR;

    /** The instance Bloomberto reads every frame. */
    public static CameraZoom shared() {
        return SHARED;
    }

    /** Moves the zoom to {@code factor} (above 1 zooms in, below 1 zooms out) over {@code ticks} ticks; 0 ticks jumps. */
    public synchronized CameraZoom zoomTo(float factor, int ticks, Easing easing) {
        float target = Math.max(0.1f, Math.min(50f, factor));
        this.from = factor(0f);
        this.to = target;
        this.duration = Math.max(0, ticks);
        this.age = 0;
        this.easing = easing == null ? Easing.LINEAR : easing;
        if (duration == 0) {
            this.from = target;
        }
        return this;
    }

    /** Goes back to no zoom over {@code ticks} ticks. */
    public synchronized CameraZoom reset(int ticks, Easing easing) {
        return zoomTo(1f, ticks, easing);
    }

    /** Advances one game tick. Bloomberto calls this; you do not need to. */
    public synchronized void tick() {
        if (age < duration) {
            age++;
        }
    }

    /** The current zoom factor, 1 when not zooming. */
    public synchronized float factor(float partialTick) {
        if (duration == 0 || age >= duration) {
            return to;
        }
        float t = Math.min(1f, (age + partialTick) / duration);
        float eased = easing.apply(t);
        // Interpolate in log space so zooming in and out feels even.
        double logFrom = Math.log(from);
        double logTo = Math.log(to);
        return (float) Math.exp(logFrom + (logTo - logFrom) * eased);
    }

    /** {@code true} while the factor is not 1. */
    public synchronized boolean isZoomed() {
        return Math.abs(to - 1f) > 1e-4f || (age < duration && Math.abs(from - 1f) > 1e-4f);
    }

    /** The field of view (degrees) after the zoom: the picture is magnified by {@link #factor}. */
    public double apply(double fovDegrees, float partialTick) {
        float f = factor(partialTick);
        if (Math.abs(f - 1f) < 1e-4f) {
            return fovDegrees;
        }
        double half = Math.toRadians(fovDegrees) / 2.0;
        return Math.toDegrees(2.0 * Math.atan(Math.tan(half) / f));
    }
}
