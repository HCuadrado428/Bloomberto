package com.dougg865.bloomberto.api.ui;

/** An axis-aligned rectangle in GUI pixels. */
public record Rect(int x, int y, int width, int height) {
    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }

    public boolean contains(double px, double py) {
        return px >= x && px < x + width && py >= y && py < y + height;
    }

    /** The rectangle shrunk by {@code amount} on every side (never smaller than 0). */
    public Rect inset(int amount) {
        int w = Math.max(0, width - 2 * amount);
        int h = Math.max(0, height - 2 * amount);
        return new Rect(x + Math.min(amount, width / 2), y + Math.min(amount, height / 2), w, h);
    }

    /** The overlap with another rectangle, or an empty rectangle at the origin of the overlap. */
    public Rect intersect(Rect other) {
        int x1 = Math.max(x, other.x);
        int y1 = Math.max(y, other.y);
        int x2 = Math.min(right(), other.right());
        int y2 = Math.min(bottom(), other.bottom());
        return new Rect(x1, y1, Math.max(0, x2 - x1), Math.max(0, y2 - y1));
    }
}
