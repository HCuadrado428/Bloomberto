package com.dougg865.bloomberto.api.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Stretches a texture to any size without distorting its corners (9-slice / 9-patch): the four corners keep their size, the
 * four edges stretch along one axis and the center stretches along both.
 */
public final class NineSlice {
    /** One piece: where it comes from in the texture and where it goes on screen. */
    public record Patch(int srcX, int srcY, int srcW, int srcH, int dstX, int dstY, int dstW, int dstH) {
    }

    private final int textureWidth;
    private final int textureHeight;
    private final int left;
    private final int top;
    private final int right;
    private final int bottom;

    /**
     * @param textureWidth  width of the whole texture file in pixels
     * @param textureHeight height of the whole texture file in pixels
     * @param left          border widths in the source texture
     */
    public NineSlice(int textureWidth, int textureHeight, int left, int top, int right, int bottom) {
        if (left + right >= textureWidth || top + bottom >= textureHeight || left < 0 || top < 0 || right < 0 || bottom < 0) {
            throw new IllegalArgumentException("Borders must leave a stretchable center");
        }
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    /** Same border on every side. */
    public NineSlice(int textureWidth, int textureHeight, int border) {
        this(textureWidth, textureHeight, border, border, border, border);
    }

    public int textureWidth() {
        return textureWidth;
    }

    public int textureHeight() {
        return textureHeight;
    }

    /**
     * The patches that fill {@code target}. When the target is smaller than the borders together, the borders are scaled down
     * evenly so the pieces still fit exactly. Empty pieces are left out.
     */
    public List<Patch> patches(Rect target) {
        int w = target.width();
        int h = target.height();
        if (w <= 0 || h <= 0) {
            return List.of();
        }
        float scaleX = left + right > w ? (float) w / (left + right) : 1f;
        float scaleY = top + bottom > h ? (float) h / (top + bottom) : 1f;
        int dl = Math.round(left * scaleX);
        int dr = left + right > w ? w - dl : Math.round(right * scaleX);
        int dt = Math.round(top * scaleY);
        int db = top + bottom > h ? h - dt : Math.round(bottom * scaleY);
        int[] srcX = {0, left, textureWidth - right};
        int[] srcW = {left, textureWidth - left - right, right};
        int[] dstX = {target.x(), target.x() + dl, target.x() + w - dr};
        int[] dstW = {dl, w - dl - dr, dr};
        int[] srcY = {0, top, textureHeight - bottom};
        int[] srcH = {top, textureHeight - top - bottom, bottom};
        int[] dstY = {target.y(), target.y() + dt, target.y() + h - db};
        int[] dstH = {dt, h - dt - db, db};
        List<Patch> result = new ArrayList<>(9);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (dstW[col] > 0 && dstH[row] > 0 && srcW[col] > 0 && srcH[row] > 0) {
                    result.add(new Patch(srcX[col], srcY[row], srcW[col], srcH[row], dstX[col], dstY[row], dstW[col], dstH[row]));
                }
            }
        }
        return result;
    }
}
