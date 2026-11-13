package com.dougg865.bloomberto.api.animation.model;

/** The animated offset of one bone: position (1/16 units), rotation (degrees) and scale. Mutable. */
public final class BoneTransform {
    public float[] position = {0, 0, 0};
    public float[] rotation = {0, 0, 0};
    public float[] scale = {1, 1, 1};

    public BoneTransform copy() {
        BoneTransform c = new BoneTransform();
        c.position = position.clone();
        c.rotation = rotation.clone();
        c.scale = scale.clone();
        return c;
    }

    /** {@code this * (1 - t) + other * t} per component. */
    public BoneTransform lerp(BoneTransform other, float t) {
        BoneTransform r = new BoneTransform();
        for (int i = 0; i < 3; i++) {
            r.position[i] = position[i] + (other.position[i] - position[i]) * t;
            r.rotation[i] = rotation[i] + (other.rotation[i] - rotation[i]) * t;
            r.scale[i] = scale[i] + (other.scale[i] - scale[i]) * t;
        }
        return r;
    }
}
