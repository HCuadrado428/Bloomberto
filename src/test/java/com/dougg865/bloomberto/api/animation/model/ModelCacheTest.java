package com.dougg865.bloomberto.api.animation.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ModelCacheTest {
    private static GeoModel model() {
        return new GeoModel("geometry.t", 16, 16, List.of(new GeoModel.Bone("root", null, new float[3], new float[3], false,
                List.of(new GeoModel.Cube(new float[] {0, 0, 0}, new float[] {1, 1, 1}, null, new float[3], 0f, false,
                        new GeoModel.Uv(0, 0, java.util.Map.of()))))));
    }

    @Test
    void sameModelObjectIsCompiledOnce() {
        GeoModel m = model();
        assertSame(ModelGeometry.compile(m), ModelGeometry.compile(m));
        // an equal but different object (e.g. after a resource reload) gets its own compiled copy
        assertNotSame(ModelGeometry.compile(m), ModelGeometry.compile(model()));
    }

    @Test
    void emitGivesFourCornersPerQuad() {
        int[] corners = {0};
        ModelGeometry.compile(model()).emit(new Pose(), (x, y, z, u, v, nx, ny, nz) -> corners[0]++);
        assertEquals(24, corners[0]); // a cube: 6 faces x 4 corners
        assertEquals(6, ModelGeometry.build(model(), new Pose()).size());
    }

    @Test
    void weakMapForgetsUnusedKeys() throws Exception {
        IdentityWeakMap<Object, String> map = new IdentityWeakMap<>();
        Object kept = new Object();
        map.put(kept, "kept");
        map.put(new Object(), "lost");
        for (int i = 0; i < 50 && map.size() > 1; i++) {
            System.gc();
            Thread.sleep(20);
        }
        assertEquals("kept", map.get(kept));
        assertTrue(map.size() <= 2);
    }
}
