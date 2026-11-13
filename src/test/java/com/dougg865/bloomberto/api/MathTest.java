package com.dougg865.bloomberto.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dougg865.bloomberto.api.math.Interpolation;
import com.dougg865.bloomberto.api.math.Noise;
import com.dougg865.bloomberto.api.math.Spline;
import org.junit.jupiter.api.Test;

class MathTest {
    private static final float EPS = 1e-5f;

    @Test
    void lerpAndInverse() {
        assertEquals(15f, Interpolation.lerp(10f, 20f, 0.5f), EPS);
        assertEquals(0.25f, Interpolation.inverseLerp(0f, 8f, 2f), EPS);
        assertEquals(0f, Interpolation.inverseLerp(3f, 3f, 9f), EPS);
        assertEquals(50f, Interpolation.remap(5f, 0f, 10f, 0f, 100f), EPS);
    }

    @Test
    void smoothstepEndpoints() {
        assertEquals(0f, Interpolation.smoothstep(-1f), EPS);
        assertEquals(0.5f, Interpolation.smoothstep(0.5f), EPS);
        assertEquals(1f, Interpolation.smootherstep(2f), EPS);
    }

    @Test
    void bezierEndpoints() {
        assertEquals(1f, Interpolation.cubicBezier(1f, 5f, 7f, 9f, 0f), EPS);
        assertEquals(9f, Interpolation.cubicBezier(1f, 5f, 7f, 9f, 1f), EPS);
    }

    @Test
    void splinePassesThroughPoints() {
        Spline s = Spline.catmullRom(0f, 10f, 4f, 20f);
        assertEquals(0f, s.sample(0f), EPS);
        assertEquals(10f, s.sample(1f / 3f), 1e-4f);
        assertEquals(4f, s.sample(2f / 3f), 1e-4f);
        assertEquals(20f, s.sample(1f), EPS);
        assertEquals(20f, s.sample(5f), EPS);
    }

    @Test
    void splineNeedsTwoPoints() {
        assertThrows(IllegalArgumentException.class, () -> Spline.catmullRom(1f));
    }

    @Test
    void noiseIsDeterministicAndBounded() {
        Noise a = new Noise(42);
        Noise b = new Noise(42);
        boolean varies = false;
        float first = a.sample(0.3f, 0.7f);
        for (int i = 0; i < 200; i++) {
            float x = i * 0.37f - 20f;
            float y = i * 0.11f + 3f;
            float v = a.sample(x, y);
            assertEquals(v, b.sample(x, y), 0f);
            assertTrue(v >= -1f && v <= 1f);
            varies |= v != first;
        }
        assertTrue(varies);
        assertEquals(0f, a.sample(3f, 5f), EPS);
        assertNotEquals(new Noise(1).sample(0.5f, 0.5f), new Noise(2).sample(0.5f, 0.5f));
    }
}
