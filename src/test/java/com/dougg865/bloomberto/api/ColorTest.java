package com.dougg865.bloomberto.api;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dougg865.bloomberto.api.color.Color;
import org.junit.jupiter.api.Test;

class ColorTest {
    @Test
    void parsesHexForms() {
        assertEquals(Color.ofRgb(255, 0, 128), Color.fromHex("#FF0080"));
        assertEquals(Color.ofRgb(255, 0, 128), Color.fromHex("ff0080"));
        assertEquals(Color.ofRgb(170, 187, 204), Color.fromHex("#abc"));
        assertEquals(Color.ofRgba(1, 2, 3, 128), Color.fromHex("#80010203"));
    }

    @Test
    void rejectsInvalidHex() {
        assertThrows(IllegalArgumentException.class, () -> Color.fromHex("#12"));
        assertThrows(IllegalArgumentException.class, () -> Color.fromHex("#GGGGGG"));
    }

    @Test
    void hexRoundTrip() {
        assertEquals("#FF0080", Color.fromHex("#ff0080").toHex());
        assertEquals("#80010203", Color.ofRgba(1, 2, 3, 128).toHex());
    }

    @Test
    void channelsAreClamped() {
        Color c = Color.ofRgba(300, -5, 10, 999);
        assertEquals(255, c.red());
        assertEquals(0, c.green());
        assertEquals(255, c.alpha());
    }

    @Test
    void hsvPrimaries() {
        assertEquals(Color.ofRgb(255, 0, 0), Color.fromHsv(0f, 1f, 1f));
        assertEquals(Color.ofRgb(0, 255, 0), Color.fromHsv(120f, 1f, 1f));
        assertEquals(Color.ofRgb(0, 0, 255), Color.fromHsv(240f, 1f, 1f));
        assertEquals(Color.ofRgb(255, 0, 0), Color.fromHsv(360f, 1f, 1f));
        assertEquals(Color.ofRgb(0, 0, 255), Color.fromHsv(-120f, 1f, 1f));
    }

    @Test
    void hsvRoundTrip() {
        float[] hsv = Color.ofRgb(0, 255, 0).toHsv();
        assertArrayEquals(new float[] {120f, 1f, 1f}, hsv, 1e-4f);
        assertArrayEquals(new float[] {0f, 0f, 0f}, Color.BLACK.toHsv(), 1e-6f);
        Color c = Color.ofRgb(200, 100, 50);
        float[] h = c.toHsv();
        assertEquals(c, Color.fromHsv(h[0], h[1], h[2]));
    }

    @Test
    void lerpMixesChannels() {
        Color mid = Color.BLACK.lerp(Color.WHITE, 0.5f);
        assertEquals(Color.ofRgb(128, 128, 128), mid);
        assertEquals(Color.BLACK, Color.BLACK.lerp(Color.WHITE, -3f));
        assertEquals(Color.WHITE, Color.BLACK.lerp(Color.WHITE, 3f));
    }
}
