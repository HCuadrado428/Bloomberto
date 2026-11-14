package com.dougg865.bloomberto.impl.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dougg865.bloomberto.platform.Services;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RenderCompatTest {
    @AfterEach
    void cleanUp() {
        net.optifine.Config.shaders = false;
        RenderCompat.resetForTests();
    }

    @Test
    void optifineShadersAreDetectedThroughReflection() {
        net.optifine.Config.shaders = false;
        RenderCompat.resetForTests();
        assertFalse(RenderCompat.shaderPackActive());
        assertTrue(RenderCompat.shaderModPresent());
        net.optifine.Config.shaders = true;
        assertTrue(RenderCompat.shaderPackActive());
        net.optifine.Config.shaders = false;
        assertFalse(RenderCompat.shaderPackActive());
    }
}
