package com.dougg865.bloomberto.api.color;

import java.util.Locale;

/**
 * Immutable color with 8-bit RGBA channels, convertible to and from HSV and hex strings.
 */
public final class Color {
    public static final Color WHITE = ofRgb(255, 255, 255);
    public static final Color BLACK = ofRgb(0, 0, 0);
    public static final Color TRANSPARENT = ofRgba(0, 0, 0, 0);

    private final int argb;

    private Color(int argb) {
        this.argb = argb;
    }

    /** Creates a color from a packed {@code 0xAARRGGBB} value. */
    public static Color ofArgb(int argb) {
        return new Color(argb);
    }

    /** Creates an opaque color from channels in the range 0-255 (values are clamped). */
    public static Color ofRgb(int r, int g, int b) {
        return ofRgba(r, g, b, 255);
    }

    public static Color ofRgba(int r, int g, int b, int a) {
        return new Color(clamp8(a) << 24 | clamp8(r) << 16 | clamp8(g) << 8 | clamp8(b));
    }

    /** Creates a color from float channels in the range 0-1 (values are clamped). */
    public static Color ofFloats(float r, float g, float b, float a) {
        return ofRgba(Math.round(r * 255f), Math.round(g * 255f), Math.round(b * 255f), Math.round(a * 255f));
    }

    /**
     * Parses {@code #RGB}, {@code #RRGGBB} or {@code #AARRGGBB} (the leading {@code #} is optional).
     *
     * @throws IllegalArgumentException if the text is not a valid hex color
     */
    public static Color fromHex(String hex) {
        String s = hex.startsWith("#") ? hex.substring(1) : hex;
        try {
            switch (s.length()) {
                case 3: {
                    int r = Integer.parseInt(s.substring(0, 1), 16);
                    int g = Integer.parseInt(s.substring(1, 2), 16);
                    int b = Integer.parseInt(s.substring(2, 3), 16);
                    return ofRgb(r * 17, g * 17, b * 17);
                }
                case 6:
                    return new Color(0xFF000000 | Integer.parseInt(s, 16));
                case 8:
                    return new Color((int) Long.parseLong(s, 16));
                default:
                    break;
            }
        } catch (NumberFormatException ignored) {
            // fall through to the exception below
        }
        throw new IllegalArgumentException("Invalid hex color: " + hex);
    }

    /**
     * Creates an opaque color from HSV.
     *
     * @param hue        in degrees; any value is wrapped into [0, 360)
     * @param saturation 0-1
     * @param value      0-1
     */
    public static Color fromHsv(float hue, float saturation, float value) {
        float h = ((hue % 360f) + 360f) % 360f / 60f;
        float s = clamp01(saturation);
        float v = clamp01(value);
        int sector = (int) h;
        float f = h - sector;
        float p = v * (1f - s);
        float q = v * (1f - s * f);
        float t = v * (1f - s * (1f - f));
        switch (sector) {
            case 0: return ofFloats(v, t, p, 1f);
            case 1: return ofFloats(q, v, p, 1f);
            case 2: return ofFloats(p, v, t, 1f);
            case 3: return ofFloats(p, q, v, 1f);
            case 4: return ofFloats(t, p, v, 1f);
            default: return ofFloats(v, p, q, 1f);
        }
    }

    public int argb() {
        return argb;
    }

    public int red() {
        return argb >> 16 & 0xFF;
    }

    public int green() {
        return argb >> 8 & 0xFF;
    }

    public int blue() {
        return argb & 0xFF;
    }

    public int alpha() {
        return argb >>> 24;
    }

    /** Returns {@code {hue (0-360), saturation (0-1), value (0-1)}}. */
    public float[] toHsv() {
        float r = red() / 255f;
        float g = green() / 255f;
        float b = blue() / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float h;
        if (delta == 0f) {
            h = 0f;
        } else if (max == r) {
            h = 60f * (((g - b) / delta) % 6f);
        } else if (max == g) {
            h = 60f * ((b - r) / delta + 2f);
        } else {
            h = 60f * ((r - g) / delta + 4f);
        }
        if (h < 0f) {
            h += 360f;
        }
        return new float[] {h, max == 0f ? 0f : delta / max, max};
    }

    public Color withAlpha(int alpha) {
        return ofRgba(red(), green(), blue(), alpha);
    }

    /** Linear interpolation per channel; {@code t} is clamped to 0-1. */
    public Color lerp(Color other, float t) {
        float k = clamp01(t);
        return ofRgba(
                Math.round(red() + (other.red() - red()) * k),
                Math.round(green() + (other.green() - green()) * k),
                Math.round(blue() + (other.blue() - blue()) * k),
                Math.round(alpha() + (other.alpha() - alpha()) * k));
    }

    /** {@code #RRGGBB} if opaque, otherwise {@code #AARRGGBB}; uppercase. */
    public String toHex() {
        return alpha() == 255
                ? String.format(Locale.ROOT, "#%06X", argb & 0xFFFFFF)
                : String.format(Locale.ROOT, "#%08X", argb);
    }

    private static int clamp8(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Color c && c.argb == argb;
    }

    @Override
    public int hashCode() {
        return argb;
    }

    @Override
    public String toString() {
        return "Color" + toHex();
    }
}
