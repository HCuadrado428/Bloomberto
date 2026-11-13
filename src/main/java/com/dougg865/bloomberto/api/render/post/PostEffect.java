package com.dougg865.bloomberto.api.render.post;

import com.dougg865.bloomberto.api.animation.Easing;
import com.dougg865.bloomberto.api.animation.Tween;
import com.dougg865.bloomberto.api.animation.TweenManager;

/**
 * A full-screen effect: a chain of passes ({@link #define(ChainSpec)}) plus an intensity from 0 to 1 that decides how strong
 * it is. Subclass it, or use one of the built-in effects ({@link BuiltInEffects}), and register it with
 * {@link PostEffects#register}.
 *
 * <p>An effect is drawn only while it is {@link #isEnabled() enabled} and its intensity is above 0.
 * The intensity can be animated with {@link #fadeTo(float, int, Easing)}.
 */
public abstract class PostEffect {
    private final TweenManager animations = new TweenManager();
    private volatile boolean enabled;
    private volatile float intensity = 1f;
    private Tween<Float> fade;

    /** Describes the passes of this effect's chain. Called once when the chain is built. */
    protected abstract void define(ChainSpec spec);

    /** Called every frame before the chain runs, to translate {@code intensity} into uniforms. */
    protected abstract void applyUniforms(UniformSetter uniforms, float intensity);

    /** Sets a uniform by name on every pass of the chain that declares it. */
    public interface UniformSetter {
        void set(String name, float... values);
    }

    /** Builds this effect's description. */
    public final ChainSpec buildSpec() {
        ChainSpec spec = new ChainSpec();
        define(spec);
        spec.validate();
        return spec;
    }

    /** Lets the renderer write this frame's uniforms. */
    public final void writeUniforms(UniformSetter setter) {
        applyUniforms(setter, intensity);
    }

    public PostEffect setIntensity(float intensity) {
        this.intensity = Math.max(0f, Math.min(1f, intensity));
        return this;
    }

    public float getIntensity() {
        return intensity;
    }

    public PostEffect enable() {
        this.enabled = true;
        return this;
    }

    public PostEffect disable() {
        this.enabled = false;
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** {@code true} when the effect should be drawn this frame. */
    public boolean isActive() {
        return enabled && intensity > 0.0001f;
    }

    /** Animates the intensity to {@code target} over {@code ticks} game ticks; replaces any running fade. */
    public PostEffect fadeTo(float target, int ticks, Easing easing) {
        animations.clear();
        fade = null;
        float to = Math.max(0f, Math.min(1f, target));
        if (ticks <= 0) {
            intensity = to;
            return this;
        }
        fade = Tween.of(intensity, to).duration(ticks).ease(easing).onUpdate(v -> intensity = Math.max(0f, Math.min(1f, v)));
        fade.start(animations);
        return this;
    }

    /** Enables the effect and fades it in from 0 to {@code target}. */
    public PostEffect fadeIn(float target, int ticks, Easing easing) {
        intensity = 0f;
        enabled = true;
        return fadeTo(target, ticks, easing);
    }

    /** Fades out to 0 and disables the effect when done. */
    public PostEffect fadeOut(int ticks, Easing easing) {
        fadeTo(0f, ticks, easing);
        if (fade != null) {
            fade.onComplete(this::disable);
        } else {
            disable();
        }
        return this;
    }

    /** Advances running fades by one tick; called by Bloomberto every client tick. */
    public void tick() {
        animations.tick();
    }
}
