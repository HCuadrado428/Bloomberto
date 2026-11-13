package com.dougg865.bloomberto.api.render.post;

import com.dougg865.bloomberto.api.color.Color;

/** Effects that ship with Bloomberto. Register what you need with {@link PostEffects#register}. */
public final class BuiltInEffects {
    private BuiltInEffects() {
    }

    private static void copyBack(ChainSpec spec, String from) {
        spec.pass("blit", from, ChainSpec.MAIN);
    }

    /** Blurs the screen; intensity 1 is a radius of {@link #maxRadius}. */
    public static final class Blur extends PostEffect {
        private float maxRadius = 16f;

        public Blur maxRadius(float radius) {
            this.maxRadius = radius;
            return this;
        }

        @Override
        protected void define(ChainSpec spec) {
            spec.target("swap");
            spec.pass("box_blur", ChainSpec.MAIN, "swap").linearFilter().uniform("BlurDir", 1f, 0f);
            spec.pass("box_blur", "swap", ChainSpec.MAIN).linearFilter().uniform("BlurDir", 0f, 1f);
        }

        @Override
        protected void applyUniforms(UniformSetter u, float intensity) {
            u.set("Radius", maxRadius * intensity);
        }
    }

    /** Makes bright areas glow. */
    public static final class Bloom extends PostEffect {
        private float threshold = 0.7f;
        private float strength = 1.2f;
        private float blurRadius = 8f;

        /** Brightness (0-1) above which pixels glow. */
        public Bloom threshold(float threshold) {
            this.threshold = threshold;
            return this;
        }

        /** How much glow is added at intensity 1. */
        public Bloom strength(float strength) {
            this.strength = strength;
            return this;
        }

        public Bloom blurRadius(float radius) {
            this.blurRadius = radius;
            return this;
        }

        @Override
        protected void define(ChainSpec spec) {
            spec.target("swap").target("bright").target("blur");
            spec.pass("bloomberto:bloom_bright", ChainSpec.MAIN, "bright");
            spec.pass("box_blur", "bright", "blur").linearFilter().uniform("BlurDir", 1f, 0f);
            spec.pass("box_blur", "blur", "bright").linearFilter().uniform("BlurDir", 0f, 1f);
            spec.pass("bloomberto:bloom_combine", ChainSpec.MAIN, "swap").aux("BloomSampler", "bright");
            copyBack(spec, "swap");
        }

        @Override
        protected void applyUniforms(UniformSetter u, float intensity) {
            u.set("Threshold", threshold);
            u.set("Radius", blurRadius);
            u.set("BloomIntensity", strength * intensity);
        }
    }

    /** Darkens (or tints) the edges of the screen. */
    public static final class Vignette extends PostEffect {
        private Color color = Color.BLACK;
        private float radius = 0.9f;
        private float softness = 0.5f;

        public Vignette color(Color color) {
            this.color = color;
            return this;
        }

        /** Distance from the center (0-1, corners are 1) where the effect reaches full strength. */
        public Vignette radius(float radius) {
            this.radius = radius;
            return this;
        }

        /** Width of the soft transition. */
        public Vignette softness(float softness) {
            this.softness = softness;
            return this;
        }

        @Override
        protected void define(ChainSpec spec) {
            spec.target("swap");
            spec.pass("bloomberto:vignette", ChainSpec.MAIN, "swap");
            copyBack(spec, "swap");
        }

        @Override
        protected void applyUniforms(UniformSetter u, float intensity) {
            u.set("Intensity", intensity);
            u.set("Radius", radius);
            u.set("Softness", softness);
            u.set("VignetteColor", color.red() / 255f, color.green() / 255f, color.blue() / 255f);
        }
    }

    /** Fades the picture to grayscale. */
    public static final class Desaturate extends PostEffect {
        @Override
        protected void define(ChainSpec spec) {
            spec.target("swap");
            spec.pass("bloomberto:desaturate", ChainSpec.MAIN, "swap");
            copyBack(spec, "swap");
        }

        @Override
        protected void applyUniforms(UniformSetter u, float intensity) {
            u.set("Amount", intensity);
        }
    }

    /** Turns the picture into big pixels; intensity 1 is {@link #maxPixelSize}-pixel blocks. */
    public static final class Pixelate extends PostEffect {
        private float maxPixelSize = 16f;

        public Pixelate maxPixelSize(float size) {
            this.maxPixelSize = size;
            return this;
        }

        @Override
        protected void define(ChainSpec spec) {
            spec.target("swap");
            spec.pass("bloomberto:pixelate", ChainSpec.MAIN, "swap");
            copyBack(spec, "swap");
        }

        @Override
        protected void applyUniforms(UniformSetter u, float intensity) {
            u.set("PixelSize", 1f + (maxPixelSize - 1f) * intensity);
        }
    }

    /** Color correction: brightness, contrast, saturation and a tint, blended in by the intensity. */
    public static final class ColorGrade extends PostEffect {
        private float brightness;
        private float contrast = 1f;
        private float saturation = 1f;
        private Color tint = Color.WHITE;

        /** Added to every channel, around -0.5 to 0.5. */
        public ColorGrade brightness(float v) {
            this.brightness = v;
            return this;
        }

        /** 1 = unchanged. */
        public ColorGrade contrast(float v) {
            this.contrast = v;
            return this;
        }

        /** 1 = unchanged, 0 = grayscale. */
        public ColorGrade saturation(float v) {
            this.saturation = v;
            return this;
        }

        /** Multiplied with the picture; white = unchanged. */
        public ColorGrade tint(Color color) {
            this.tint = color;
            return this;
        }

        @Override
        protected void define(ChainSpec spec) {
            spec.target("swap");
            spec.pass("bloomberto:color_grade", ChainSpec.MAIN, "swap");
            copyBack(spec, "swap");
        }

        @Override
        protected void applyUniforms(UniformSetter u, float intensity) {
            u.set("GradeAmount", intensity);
            u.set("Brightness", brightness);
            u.set("Contrast", contrast);
            u.set("Saturation", saturation);
            u.set("Tint", tint.red() / 255f, tint.green() / 255f, tint.blue() / 255f);
        }
    }

    /**
     * Shakes the picture. Use {@link #trigger} for an impact that dies away by itself, or {@code enable()} with an
     * intensity for a continuous rumble. The intensity scales the movement.
     *
     * <pre>{@code
     * ((BuiltInEffects.Shake) PostEffects.get(Bloomberto.id("shake"))).trigger(1f, 20);
     * }</pre>
     */
    public static final class Shake extends PostEffect {
        private final ShakeModel model = new ShakeModel(7L, 22f);
        private final long start = System.nanoTime();
        private float maxOffset = 0.015f;

        /** The largest offset as a fraction of the screen at intensity 1. Default 1.5%. */
        public Shake maxOffset(float fraction) {
            this.maxOffset = fraction;
            return this;
        }

        /** Starts a shake of the given strength (0-1) that fades out over {@code ticks} ticks. */
        public Shake trigger(float strength, int ticks) {
            setIntensity(strength);
            enable();
            fadeOut(ticks, com.dougg865.bloomberto.api.animation.Easing.OUT_QUAD);
            return this;
        }

        @Override
        protected void define(ChainSpec spec) {
            spec.target("swap");
            spec.pass("bloomberto:shake", ChainSpec.MAIN, "swap");
            copyBack(spec, "swap");
        }

        @Override
        protected void applyUniforms(UniformSetter u, float intensity) {
            float seconds = (System.nanoTime() - start) / 1_000_000_000f;
            u.set("Offset", model.offset(seconds, intensity, maxOffset));
        }
    }

    /**
     * Zooms into the center of the screen. This is a screen-space zoom (the picture is scaled), not a change of the
     * field of view; intensity 1 is {@link #maxZoom}.
     */
    public static final class Zoom extends PostEffect {
        private float maxZoom = 1.5f;
        private float centerX = 0.5f;
        private float centerY = 0.5f;

        /** The scale factor at intensity 1; default 1.5. */
        public Zoom maxZoom(float factor) {
            this.maxZoom = factor;
            return this;
        }

        /** The zoom origin as screen fractions; default the middle. */
        public Zoom center(float x, float y) {
            this.centerX = x;
            this.centerY = y;
            return this;
        }

        @Override
        protected void define(ChainSpec spec) {
            spec.target("swap");
            spec.pass("bloomberto:zoom", ChainSpec.MAIN, "swap");
            copyBack(spec, "swap");
        }

        @Override
        protected void applyUniforms(UniformSetter u, float intensity) {
            u.set("Zoom", 1f + (maxZoom - 1f) * intensity);
            u.set("Center", centerX, centerY);
        }
    }
}
