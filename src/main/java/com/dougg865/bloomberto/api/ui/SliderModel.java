package com.dougg865.bloomberto.api.ui;

/** Maps a pointer position on a slider track to a value in a range, with optional stepping. Pure math. */
public final class SliderModel {
    private final double min;
    private final double max;
    private final double step;
    private double value;

    /**
     * @param step the value granularity (0 = continuous)
     */
    public SliderModel(double min, double max, double step, double initial) {
        if (max <= min) {
            throw new IllegalArgumentException("max must be greater than min");
        }
        this.min = min;
        this.max = max;
        this.step = Math.max(0, step);
        this.value = snap(initial);
    }

    private double snap(double v) {
        double clamped = Math.max(min, Math.min(max, v));
        if (step <= 0) {
            return clamped;
        }
        double snapped = min + Math.round((clamped - min) / step) * step;
        return Math.max(min, Math.min(max, snapped));
    }

    public double value() {
        return value;
    }

    public void setValue(double v) {
        this.value = snap(v);
    }

    /** The value as a fraction of the range, 0 to 1 (where the knob sits). */
    public double fraction() {
        return (value - min) / (max - min);
    }

    /**
     * Sets the value from a pointer position.
     *
     * @param localX the pointer's x inside the track
     * @param trackWidth the track width in pixels
     */
    public void setFromPointer(double localX, double trackWidth) {
        double f = trackWidth <= 0 ? 0 : Math.max(0, Math.min(1, localX / trackWidth));
        setValue(min + f * (max - min));
    }

    /** Moves by {@code steps} steps (or 1% of the range when continuous); for keyboard control. */
    public void nudge(int steps) {
        double delta = step > 0 ? step : (max - min) * 0.01;
        setValue(value + steps * delta);
    }
}
