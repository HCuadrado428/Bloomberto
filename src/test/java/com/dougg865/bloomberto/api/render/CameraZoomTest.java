package com.dougg865.bloomberto.api.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dougg865.bloomberto.api.animation.Easing;
import org.junit.jupiter.api.Test;

class CameraZoomTest {
    @Test
    void noZoomLeavesFovAlone() {
        CameraZoom z = new CameraZoom();
        assertFalse(z.isZoomed());
        assertEquals(70.0, z.apply(70.0, 0.3f), 1e-9);
    }

    @Test
    void jumpZoomAppliesImmediately() {
        CameraZoom z = new CameraZoom();
        z.zoomTo(2f, 0, Easing.LINEAR);
        assertTrue(z.isZoomed());
        assertEquals(2f, z.factor(0f), 1e-5f);
        // twice as large: tan(fov/2) halves
        double fov = z.apply(90.0, 0f);
        assertEquals(0.5, Math.tan(Math.toRadians(fov / 2)), 1e-6);
    }

    @Test
    void zoomEasesOverTicksAndResets() {
        CameraZoom z = new CameraZoom();
        z.zoomTo(4f, 10, Easing.LINEAR);
        float last = 1f;
        for (int i = 0; i < 10; i++) {
            float f = z.factor(0f);
            assertTrue(f >= last - 1e-6f);
            last = f;
            z.tick();
        }
        assertEquals(4f, z.factor(0f), 1e-4f);
        z.reset(5, Easing.LINEAR);
        for (int i = 0; i < 5; i++) {
            z.tick();
        }
        assertEquals(1f, z.factor(0f), 1e-4f);
        assertFalse(z.isZoomed());
    }

    @Test
    void midwayIsBetweenStartAndTarget() {
        CameraZoom z = new CameraZoom();
        z.zoomTo(4f, 10, Easing.LINEAR);
        for (int i = 0; i < 5; i++) {
            z.tick();
        }
        assertEquals(2f, z.factor(0f), 1e-3f);
    }
}
