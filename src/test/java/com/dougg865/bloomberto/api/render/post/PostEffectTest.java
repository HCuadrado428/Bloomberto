package com.dougg865.bloomberto.api.render.post;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dougg865.bloomberto.api.animation.Easing;
import com.dougg865.bloomberto.api.color.Color;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PostEffectTest {
    private static Map<String, float[]> uniforms(PostEffect effect) {
        Map<String, float[]> map = new LinkedHashMap<>();
        effect.writeUniforms(map::put);
        return map;
    }

    @Test
    void everyBuiltInBuildsAValidChainEndingOnTheMainTarget() {
        PostEffect[] effects = {
            new BuiltInEffects.Blur(), new BuiltInEffects.Bloom(), new BuiltInEffects.Vignette(),
            new BuiltInEffects.Desaturate(), new BuiltInEffects.Pixelate(), new BuiltInEffects.ColorGrade()
        };
        for (PostEffect effect : effects) {
            ChainSpec spec = effect.buildSpec();
            assertFalse(spec.passes().isEmpty(), effect.getClass().getSimpleName());
            assertEquals(ChainSpec.MAIN, spec.passes().get(spec.passes().size() - 1).out(),
                    effect.getClass().getSimpleName() + " must write its result back to the main target");
            for (ChainSpec.Pass pass : spec.passes()) {
                assertTrue(pass.in().equals(ChainSpec.MAIN) || spec.targets().contains(pass.in()));
                assertTrue(pass.out().equals(ChainSpec.MAIN) || spec.targets().contains(pass.out()));
            }
        }
    }

    @Test
    void bloomUsesTheBlurredTargetAsAuxInput() {
        ChainSpec spec = new BuiltInEffects.Bloom().buildSpec();
        ChainSpec.Pass combine = spec.passes().stream().filter(p -> p.program().equals("bloomberto:bloom_combine")).findFirst().orElseThrow();
        assertEquals("bright", combine.auxTargets().get("BloomSampler"));
        assertEquals(5, spec.passes().size());
    }

    @Test
    void blurHasHorizontalAndVerticalPasses() {
        ChainSpec spec = new BuiltInEffects.Blur().buildSpec();
        assertArrayEquals(new float[] {1f, 0f}, spec.passes().get(0).constants().get("BlurDir"));
        assertArrayEquals(new float[] {0f, 1f}, spec.passes().get(1).constants().get("BlurDir"));
        assertTrue(spec.passes().get(0).isLinearFilter());
    }

    @Test
    void intensityDrivesUniforms() {
        assertEquals(8f, uniforms(new BuiltInEffects.Blur().setIntensity(0.5f)).get("Radius")[0], 1e-6f);
        assertEquals(0.3f, uniforms(new BuiltInEffects.Desaturate().setIntensity(0.3f)).get("Amount")[0], 1e-6f);
        assertEquals(1f, uniforms(new BuiltInEffects.Pixelate().setIntensity(0f)).get("PixelSize")[0], 1e-6f);
        assertEquals(16f, uniforms(new BuiltInEffects.Pixelate().setIntensity(1f)).get("PixelSize")[0], 1e-6f);
        Map<String, float[]> v = uniforms(new BuiltInEffects.Vignette().color(Color.ofRgb(255, 0, 0)).setIntensity(0.6f));
        assertEquals(0.6f, v.get("Intensity")[0], 1e-6f);
        assertArrayEquals(new float[] {1f, 0f, 0f}, v.get("VignetteColor"), 1e-6f);
        assertEquals(0.6f * 1.2f, uniforms(new BuiltInEffects.Bloom().setIntensity(0.6f)).get("BloomIntensity")[0], 1e-6f);
        Map<String, float[]> g = uniforms(new BuiltInEffects.ColorGrade().contrast(1.5f).saturation(0.5f).setIntensity(1f));
        assertEquals(1.5f, g.get("Contrast")[0], 1e-6f);
        assertEquals(0.5f, g.get("Saturation")[0], 1e-6f);
        assertEquals(1f, g.get("GradeAmount")[0], 1e-6f);
    }

    @Test
    void stateAndClamping() {
        PostEffect e = new BuiltInEffects.Desaturate();
        assertFalse(e.isEnabled());
        assertFalse(e.isActive());
        e.enable();
        assertTrue(e.isActive());
        e.setIntensity(3f);
        assertEquals(1f, e.getIntensity());
        e.setIntensity(-1f);
        assertEquals(0f, e.getIntensity());
        assertFalse(e.isActive(), "enabled but intensity 0 draws nothing");
        e.disable();
        assertFalse(e.isEnabled());
    }

    @Test
    void fadeToAnimatesIntensityOverTicks() {
        PostEffect e = new BuiltInEffects.Desaturate().setIntensity(0f);
        e.fadeTo(1f, 4, Easing.LINEAR);
        e.tick();
        assertEquals(0.25f, e.getIntensity(), 1e-6f);
        e.tick();
        e.tick();
        e.tick();
        assertEquals(1f, e.getIntensity(), 1e-6f);
        e.tick();
        assertEquals(1f, e.getIntensity(), 1e-6f);
        e.fadeTo(0.2f, 0, Easing.LINEAR);
        assertEquals(0.2f, e.getIntensity(), 1e-6f);
    }

    @Test
    void fadeInEnablesAndFadeOutDisables() {
        PostEffect e = new BuiltInEffects.Vignette();
        e.fadeIn(0.8f, 2, Easing.LINEAR);
        assertTrue(e.isEnabled());
        assertEquals(0f, e.getIntensity());
        e.tick();
        e.tick();
        assertEquals(0.8f, e.getIntensity(), 1e-6f);
        e.fadeOut(2, Easing.LINEAR);
        assertTrue(e.isEnabled());
        e.tick();
        e.tick();
        assertEquals(0f, e.getIntensity(), 1e-6f);
        assertFalse(e.isEnabled());
    }

    @Test
    void aNewFadeReplacesTheRunningOne() {
        PostEffect e = new BuiltInEffects.Desaturate().setIntensity(0f);
        e.fadeTo(1f, 10, Easing.LINEAR);
        e.tick();
        e.fadeTo(0f, 2, Easing.LINEAR);
        e.tick();
        e.tick();
        assertEquals(0f, e.getIntensity(), 1e-6f);
        e.tick();
        assertEquals(0f, e.getIntensity(), 1e-6f, "the first fade must not keep running");
    }

    @Test
    void chainSpecRejectsMistakes() {
        ChainSpec spec = new ChainSpec();
        assertThrows(IllegalArgumentException.class, () -> spec.target(ChainSpec.MAIN));
        assertThrows(IllegalArgumentException.class, () -> spec.pass("p", ChainSpec.MAIN, "missing"));
        spec.target("a");
        assertThrows(IllegalArgumentException.class, () -> spec.target("a"));
        assertThrows(IllegalStateException.class, spec::validate, "no passes");
        spec.pass("p", ChainSpec.MAIN, "a").aux("S", "ghost");
        assertThrows(IllegalArgumentException.class, spec::validate, "unknown aux target");
    }
}
