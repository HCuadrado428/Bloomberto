package com.dougg865.bloomberto.api.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EasingTest {
    @Test
    void everyCurveStartsAtZeroAndEndsAtOne() {
        for (Easing e : Easing.values()) {
            assertEquals(0f, e.apply(0f), 1e-5f, e + " at 0");
            assertEquals(1f, e.apply(1f), 1e-5f, e + " at 1");
            assertEquals(0f, e.apply(-3f), 1e-5f, e + " below 0");
            assertEquals(1f, e.apply(3f), 1e-5f, e + " above 1");
        }
    }

    @Test
    void curvesAreContinuousAtTheirEnds() {
        for (Easing e : Easing.values()) {
            assertEquals(0f, e.apply(1e-4f), 0.05f, e + " near 0");
            assertEquals(1f, e.apply(1f - 1e-4f), 0.05f, e + " near 1");
        }
    }

    @Test
    void inOutCurvesAreSymmetricAtTheMidpoint() {
        for (Easing e : Easing.values()) {
            if (e.name().startsWith("IN_OUT_")) {
                assertEquals(0.5f, e.apply(0.5f), 1e-4f, e.name());
                assertEquals(1f, e.apply(0.3f) + e.apply(0.7f), 1e-4f, e.name() + " symmetry");
            }
        }
    }

    @Test
    void knownValues() {
        assertEquals(0.5f, Easing.LINEAR.apply(0.5f), 1e-6f);
        assertEquals(0.875f, Easing.OUT_CUBIC.apply(0.5f), 1e-6f);
        assertEquals(0.125f, Easing.IN_CUBIC.apply(0.5f), 1e-6f);
        assertEquals(0.25f, Easing.IN_QUAD.apply(0.5f), 1e-6f);
        assertEquals(0.75f, Easing.OUT_QUAD.apply(0.5f), 1e-6f);
    }

    @Test
    void inCurvesAreSlowerThanLinearAndOutCurvesFaster() {
        for (Easing e : new Easing[] {Easing.IN_QUAD, Easing.IN_CUBIC, Easing.IN_QUART, Easing.IN_QUINT, Easing.IN_EXPO,
                Easing.IN_CIRC, Easing.IN_SINE}) {
            assertTrue(e.apply(0.5f) < 0.5f, e.name());
        }
        for (Easing e : new Easing[] {Easing.OUT_QUAD, Easing.OUT_CUBIC, Easing.OUT_QUART, Easing.OUT_QUINT, Easing.OUT_EXPO,
                Easing.OUT_CIRC, Easing.OUT_SINE}) {
            assertTrue(e.apply(0.5f) > 0.5f, e.name());
        }
    }

    @Test
    void backAndElasticOvershoot() {
        assertTrue(Easing.OUT_BACK.apply(0.7f) > 1f);
        assertTrue(Easing.IN_BACK.apply(0.3f) < 0f);
        boolean over = false;
        for (int i = 1; i < 100; i++) {
            over |= Easing.OUT_ELASTIC.apply(i / 100f) > 1f;
        }
        assertTrue(over);
    }

    @Test
    void bounceStaysInRange() {
        for (int i = 0; i <= 100; i++) {
            float v = Easing.OUT_BOUNCE.apply(i / 100f);
            assertTrue(v >= 0f && v <= 1.0001f);
        }
    }
}
