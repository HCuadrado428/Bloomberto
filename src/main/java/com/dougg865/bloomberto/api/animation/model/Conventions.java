package com.dougg865.bloomberto.api.animation.model;

/**
 * Converts Blockbench / Bedrock values to Minecraft's render space the way GeckoLib does, so models and animations made for
 * GeckoLib look the same here: the X axis is mirrored, and rotations use the signs {@code (-x, -y, +z)} applied in the order
 * Z, Y, X. All lengths are in 1/16 block units until the renderer scales them.
 */
public final class Conventions {
    private Conventions() {
    }

    /** A pivot or position from the file to render space. */
    public static float[] position(float[] p) {
        return new float[] {-p[0], p[1], p[2]};
    }

    /** A rotation in degrees from the file to radians in render space, as {@code {x, y, z}}. */
    public static float[] rotation(float[] degrees) {
        return new float[] {
            (float) Math.toRadians(-degrees[0]),
            (float) Math.toRadians(-degrees[1]),
            (float) Math.toRadians(degrees[2])
        };
    }
}
