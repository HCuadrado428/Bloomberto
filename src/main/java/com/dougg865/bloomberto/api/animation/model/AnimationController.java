package com.dougg865.bloomberto.api.animation.model;

import java.util.Map;

/**
 * Plays the animations of a model on an entity or block entity: one animation at a time, with smooth transitions between
 * them. Call {@link #tick()} once per game tick and {@link #pose(float)} when drawing.
 *
 * <pre>{@code
 * controller.play("animation.golem.walk").transition(5);                    // blend over 5 ticks
 * controller.play("animation.golem.attack").transition(2).then("animation.golem.idle");
 * }</pre>
 *
 * Playing the clip that is already playing does nothing (so you can call {@code play} every tick); use
 * {@link #restart()} to start it over.
 */
public final class AnimationController {
    private static final float SECONDS_PER_TICK = 0.05f;

    private final Map<String, AnimationClip> clips;
    private AnimationClip current;
    private float time;
    private float speed = 1f;
    private String next;

    private Pose blendFrom;
    private int blendTicks;
    private int blendElapsed;
    private boolean freshSwitch;

    public AnimationController(Map<String, AnimationClip> clips) {
        this.clips = Map.copyOf(clips);
    }

    /** Handle returned by {@link #play} to adjust the switch that was just made. */
    public final class Play {
        /** Blends from the previous animation over {@code ticks} ticks. Only works right after {@code play}. */
        public Play transition(int ticks) {
            if (freshSwitch && blendFrom != null) {
                blendTicks = Math.max(0, ticks);
                blendElapsed = 0;
            }
            return this;
        }

        /** Plays {@code clip} when the current one ends (for animations that do not loop). */
        public Play then(String clip) {
            if (!clips.containsKey(clip)) {
                throw new IllegalArgumentException("Unknown animation: " + clip);
            }
            next = clip;
            return this;
        }
    }

    /**
     * Starts an animation. If it is already the current one, nothing changes.
     *
     * @throws IllegalArgumentException if the model has no animation with that name
     */
    public Play play(String name) {
        AnimationClip clip = clips.get(name);
        if (clip == null) {
            throw new IllegalArgumentException("Unknown animation '" + name + "'; available: " + clips.keySet());
        }
        if (clip == current) {
            freshSwitch = false;
            return new Play();
        }
        switchTo(clip);
        return new Play();
    }

    private void switchTo(AnimationClip clip) {
        blendFrom = current == null ? null : pose(0f);
        blendTicks = 0;
        blendElapsed = 0;
        current = clip;
        time = 0f;
        next = null;
        freshSwitch = true;
    }

    /** Starts the current animation again from its first frame. */
    public void restart() {
        if (current != null) {
            AnimationClip clip = current;
            current = null;
            switchTo(clip);
        }
    }

    public void stop() {
        current = null;
        blendFrom = null;
        next = null;
    }

    /** Playback speed multiplier; 1 is normal. */
    public AnimationController speed(float speed) {
        this.speed = speed;
        return this;
    }

    public String currentName() {
        return current == null ? null : current.name();
    }

    public boolean isPlaying(String name) {
        return current != null && current.name().equals(name) && !isFinished();
    }

    /** {@code true} when the current animation does not loop and has reached its end. */
    public boolean isFinished() {
        return current != null && current.isFinished(time);
    }

    /** Advances time by one game tick. */
    public void tick() {
        freshSwitch = false;
        if (current == null) {
            return;
        }
        time += SECONDS_PER_TICK * speed;
        if (blendFrom != null) {
            blendElapsed++;
            if (blendElapsed >= blendTicks) {
                blendFrom = null;
            }
        }
        if (next != null && current.isFinished(time)) {
            String name = next;
            next = null;
            switchTo(clips.get(name));
            freshSwitch = false;
        }
    }

    /**
     * The pose to draw with.
     *
     * @param partialTick how far into the current tick (0-1), for smooth motion between ticks
     */
    public Pose pose(float partialTick) {
        if (current == null) {
            return new Pose();
        }
        float t = time + partialTick * SECONDS_PER_TICK * speed;
        Pose clipPose = current.sample(t);
        if (blendFrom != null && blendTicks > 0) {
            float progress = Math.min(1f, (blendElapsed + partialTick) / blendTicks);
            return Pose.blend(blendFrom, clipPose, progress);
        }
        return clipPose;
    }
}
