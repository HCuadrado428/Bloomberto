package com.dougg865.bloomberto.api.ui;

import com.dougg865.bloomberto.api.animation.Easing;
import com.dougg865.bloomberto.api.animation.Tween;
import com.dougg865.bloomberto.api.animation.TweenManager;

/**
 * Animates how a piece of GUI appears or disappears: it fades, slides in from an offset and scales up, all driven by one
 * progress value that moves from 0 (hidden) to 1 (shown). Read {@link #alpha()}, {@link #offsetX()}, {@link #offsetY()}
 * and {@link #scale()} each frame and apply them (see {@code UiGraphics.transition}).
 *
 * <pre>{@code
 * Transition t = Transition.create().fade().slide(0, 12).scale(0.9f).duration(10);
 * t.show();                         // later: t.hide();
 * }</pre>
 */
public final class Transition {
    private final TweenManager animations;
    private boolean fade;
    private float slideX;
    private float slideY;
    private float fromScale = 1f;
    private int duration = 10;
    private Easing easing = Easing.OUT_CUBIC;
    private float progress;
    private Tween<Float> running;

    private Transition(TweenManager animations) {
        this.animations = animations;
    }

    /** A transition ticked by Bloomberto's shared client manager. */
    public static Transition create() {
        return new Transition(TweenManager.shared());
    }

    /** A transition ticked by the manager you provide (useful in tests or on the server). */
    public static Transition create(TweenManager manager) {
        return new Transition(manager);
    }

    /** Fade in and out. */
    public Transition fade() {
        this.fade = true;
        return this;
    }

    /** Start {@code dx}/{@code dy} pixels away from the final position and move in. */
    public Transition slide(float dx, float dy) {
        this.slideX = dx;
        this.slideY = dy;
        return this;
    }

    /** Start at this scale (e.g. 0.9) and grow to 1. */
    public Transition scale(float from) {
        this.fromScale = from;
        return this;
    }

    public Transition duration(int ticks) {
        this.duration = Math.max(1, ticks);
        return this;
    }

    public Transition ease(Easing easing) {
        this.easing = easing;
        return this;
    }

    public Transition show() {
        return moveTo(1f);
    }

    public Transition hide() {
        return moveTo(0f);
    }

    /** Jumps to the shown state without animating. */
    public Transition showNow() {
        stop();
        progress = 1f;
        return this;
    }

    private void stop() {
        if (running != null) {
            animations.remove(running);
            running = null;
        }
    }

    private Transition moveTo(float target) {
        stop();
        float from = progress;
        if (from == target) {
            return this;
        }
        // The distance to travel decides the time, so reversing halfway takes proportionally less.
        int ticks = Math.max(1, Math.round(duration * Math.abs(target - from)));
        running = Tween.of(from, target).duration(ticks).ease(easing).onUpdate(v -> progress = v);
        running.start(animations);
        return this;
    }

    /** 0 = hidden, 1 = fully shown. */
    public float progress() {
        return progress;
    }

    public boolean isVisible() {
        return progress > 0f;
    }

    public boolean isAnimating() {
        return running != null && !running.isFinished();
    }

    public float alpha() {
        return fade ? progress : 1f;
    }

    public float offsetX() {
        return slideX * (1f - progress);
    }

    public float offsetY() {
        return slideY * (1f - progress);
    }

    public float scale() {
        return fromScale + (1f - fromScale) * progress;
    }
}
