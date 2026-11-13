package com.dougg865.bloomberto.api.animation;

import com.dougg865.bloomberto.api.color.Color;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Interpolates between two values of type {@code T}; must return a new instance for mutable types. */
@FunctionalInterface
public interface Interpolator<T> {
    T interpolate(T from, T to, float t);

    Interpolator<Float> FLOAT = (a, b, t) -> a + (b - a) * t;
    Interpolator<Vector3f> VECTOR3F = (a, b, t) -> a.lerp(b, t, new Vector3f());
    /** Spherical interpolation, which keeps the shortest rotation. */
    Interpolator<Quaternionf> QUATERNIONF = (a, b, t) -> a.slerp(b, t, new Quaternionf());
    Interpolator<Color> COLOR = Color::lerp;
}
