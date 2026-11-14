package com.dougg865.bloomberto.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BloombertoApiTest {
    @Test
    void levelChecks() {
        int level = BloombertoApi.level();
        assertTrue(level >= 2);
        assertTrue(BloombertoApi.has(level));
        assertTrue(BloombertoApi.has(1));
        assertFalse(BloombertoApi.has(level + 1));
        BloombertoApi.require("test", level);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> BloombertoApi.require("MyMod", level + 1));
        assertTrue(e.getMessage().contains("MyMod"));
        assertTrue(e.getMessage().contains(String.valueOf(level + 1)));
        assertEquals(level, BloombertoApi.level());
    }
}
