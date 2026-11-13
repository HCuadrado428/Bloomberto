package com.dougg865.bloomberto.api.animation.model;

import com.dougg865.bloomberto.api.math.Interpolation;
import java.util.List;
import java.util.Map;

/**
 * One animation of a Blockbench {@code .animation.json}: keyframes per bone for position, rotation and scale.
 * Times are in seconds.
 */
public record AnimationClip(String name, float length, Loop loop, Map<String, BoneChannels> bones) {
    /** What happens when the end is reached. */
    public enum Loop { LOOP, ONCE, HOLD }

    public enum Interp { LINEAR, CATMULLROM, STEP }

    /**
     * A keyframe. {@code pre} is the value just before the key and {@code post} just after it (they differ only for a jump).
     */
    public record Keyframe(float time, float[] pre, float[] post, Interp interp) {
    }

    /** The keyframes of one bone, or {@code null} for a channel the animation does not touch. */
    public record BoneChannels(List<Keyframe> position, List<Keyframe> rotation, List<Keyframe> scale) {
    }

    public AnimationClip {
        bones = Map.copyOf(bones);
    }

    /**
     * Maps a playback time to a time inside the clip: loops wrap, one-shot clips stop at the end, hold clips stay on the last
     * frame (all three read the same value at the end).
     */
    public float wrap(float seconds) {
        if (length <= 0f) {
            return 0f;
        }
        return switch (loop) {
            case LOOP -> {
                float t = seconds % length;
                yield t < 0 ? t + length : t;
            }
            case ONCE, HOLD -> Math.max(0f, Math.min(length, seconds));
        };
    }

    /** {@code true} once a non-looping clip has played to its end. */
    public boolean isFinished(float seconds) {
        return loop != Loop.LOOP && seconds >= length;
    }

    /** The pose at {@code seconds} (after applying {@link #wrap}). Bones the clip does not animate are absent. */
    public Pose sample(float seconds) {
        float t = wrap(seconds);
        Pose pose = new Pose();
        for (Map.Entry<String, BoneChannels> e : bones.entrySet()) {
            BoneTransform bt = pose.get(e.getKey());
            BoneChannels c = e.getValue();
            if (c.position() != null) {
                bt.position = value(c.position(), t, loop == Loop.LOOP, length);
            }
            if (c.rotation() != null) {
                bt.rotation = value(c.rotation(), t, loop == Loop.LOOP, length);
            }
            if (c.scale() != null) {
                bt.scale = value(c.scale(), t, loop == Loop.LOOP, length);
            }
        }
        return pose;
    }

    /** Interpolated value of a keyframe track at time {@code t}. */
    static float[] value(List<Keyframe> keys, float t, boolean looping, float length) {
        if (keys.isEmpty()) {
            return new float[] {0, 0, 0};
        }
        if (t <= keys.get(0).time()) {
            return keys.get(0).post().clone();
        }
        Keyframe last = keys.get(keys.size() - 1);
        if (t >= last.time()) {
            return last.post().clone();
        }
        int i = 0;
        while (i + 1 < keys.size() && keys.get(i + 1).time() <= t) {
            i++;
        }
        Keyframe a = keys.get(i);
        Keyframe b = keys.get(i + 1);
        float span = b.time() - a.time();
        float f = span <= 0f ? 1f : (t - a.time()) / span;
        if (b.interp() == Interp.STEP) {
            return a.post().clone();
        }
        float[] result = new float[3];
        boolean spline = a.interp() == Interp.CATMULLROM || b.interp() == Interp.CATMULLROM;
        for (int axis = 0; axis < 3; axis++) {
            if (spline) {
                float p0 = i > 0 ? keys.get(i - 1).post()[axis] : a.post()[axis];
                float p3 = i + 2 < keys.size() ? keys.get(i + 2).pre()[axis] : b.pre()[axis];
                result[axis] = Interpolation.catmullRom(p0, a.post()[axis], b.pre()[axis], p3, f);
            } else {
                result[axis] = Interpolation.lerp(a.post()[axis], b.pre()[axis], f);
            }
        }
        return result;
    }
}
