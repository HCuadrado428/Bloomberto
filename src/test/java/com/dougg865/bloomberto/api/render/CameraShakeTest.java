package com.dougg865.bloomberto.api.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CameraShakeTest {
    private static float max(float[] o) {
        return Math.max(Math.abs(o[0]), Math.abs(o[1]));
    }

    @Test
    void idleShakeIsZero() {
        CameraShake s = new CameraShake();
        assertFalse(s.isShaking());
        assertEquals(0f, max(s.offset(0.5f)));
    }

    @Test
    void shakeStaysWithinMaxAndEnds() {
        CameraShake s = new CameraShake();
        s.shake(1f, 10, 2f);
        assertTrue(s.isShaking());
        for (int tick = 0; tick < 10; tick++) {
            assertTrue(max(s.offset(0.5f)) <= 2f + 1e-4f);
            s.tick();
        }
        assertFalse(s.isShaking());
        assertEquals(0f, max(s.offset(0f)));
    }

    @Test
    void shakeFadesOut() {
        CameraShake s = new CameraShake();
        s.shake(1f, 20, 4f);
        float early = 0;
        float late = 0;
        for (int tick = 0; tick < 20; tick++) {
            float m = max(s.offset(0f));
            if (tick < 5) early = Math.max(early, m);
            if (tick >= 15) late = Math.max(late, m);
            s.tick();
        }
        assertTrue(late < early);
    }

    @Test
    void weakerShakeDoesNotReplaceStrongerOne() {
        CameraShake s = new CameraShake();
        s.shake(1f, 40, 4f);
        s.shake(0.1f, 5, 4f);
        for (int i = 0; i < 6; i++) {
            s.tick();
        }
        assertTrue(s.isShaking());
    }

    @Test
    void stopClears() {
        CameraShake s = new CameraShake();
        s.shake(1f, 40);
        s.stop();
        assertFalse(s.isShaking());
    }
}
