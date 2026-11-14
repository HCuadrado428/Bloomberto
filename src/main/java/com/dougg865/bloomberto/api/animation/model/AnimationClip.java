package com.dougg865.bloomberto.api.animation.model;

import com.dougg865.bloomberto.api.animation.molang.Molang;
import com.dougg865.bloomberto.api.animation.molang.MolangContext;
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
    public record Keyframe(float time, float[] pre, float[] post, Interp interp, Molang[] preExpr, Molang[] postExpr) {
        /** A keyframe with plain numbers. */
        public Keyframe(float time, float[] pre, float[] post, Interp interp) {
            this(time, pre, post, interp, null, null);
        }

        /** The value just before the key; Molang axes are evaluated with {@code ctx}. */
        float[] pre(MolangContext ctx) {
            return resolve(pre, preExpr, ctx);
        }

        /** The value just after the key; Molang axes are evaluated with {@code ctx}. */
        float[] post(MolangContext ctx) {
            return resolve(post, postExpr, ctx);
        }

        private static float[] resolve(float[] constants, Molang[] expr, MolangContext ctx) {
            if (expr == null || ctx == null) {
                return constants.clone();
            }
            float[] v = constants.clone();
            for (int i = 0; i < 3; i++) {
                if (expr[i] != null) {
                    v[i] = (float) expr[i].eval(ctx);
                }
            }
            return v;
        }
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
        return sample(seconds, null);
    }

    /**
     * Like {@link #sample(float)} but evaluates Molang values with {@code ctx}. {@code query.anim_time} (time inside the clip)
     * and {@code query.life_time} (the playback time, not wrapped) are set on it first.
     */
    public Pose sample(float seconds, MolangContext ctx) {
        float t = wrap(seconds);
        if (ctx != null) {
            ctx.set("query.anim_time", t).set("query.life_time", seconds);
        }
        Pose pose = new Pose();
        for (Map.Entry<String, BoneChannels> e : bones.entrySet()) {
            BoneTransform bt = pose.get(e.getKey());
            BoneChannels c = e.getValue();
            if (c.position() != null) {
                bt.position = value(c.position(), t, loop == Loop.LOOP, length, ctx);
            }
            if (c.rotation() != null) {
                bt.rotation = value(c.rotation(), t, loop == Loop.LOOP, length, ctx);
            }
            if (c.scale() != null) {
                bt.scale = value(c.scale(), t, loop == Loop.LOOP, length, ctx);
            }
        }
        return pose;
    }

    /** Interpolated value of a keyframe track at time {@code t}. */
    static float[] value(List<Keyframe> keys, float t, boolean looping, float length) {
        return value(keys, t, looping, length, null);
    }

    static float[] value(List<Keyframe> keys, float t, boolean looping, float length, MolangContext ctx) {
        if (keys.isEmpty()) {
            return new float[] {0, 0, 0};
        }
        if (t <= keys.get(0).time()) {
            return keys.get(0).post(ctx);
        }
        Keyframe last = keys.get(keys.size() - 1);
        if (t >= last.time()) {
            return last.post(ctx);
        }
        int i = 0;
        while (i + 1 < keys.size() && keys.get(i + 1).time() <= t) {
            i++;
        }
        Keyframe a = keys.get(i);
        Keyframe b = keys.get(i + 1);
        float[] aPost = a.post(ctx);
        float[] bPre = b.pre(ctx);
        float span = b.time() - a.time();
        float f = span <= 0f ? 1f : (t - a.time()) / span;
        if (b.interp() == Interp.STEP) {
            return aPost;
        }
        float[] result = new float[3];
        boolean spline = a.interp() == Interp.CATMULLROM || b.interp() == Interp.CATMULLROM;
        for (int axis = 0; axis < 3; axis++) {
            if (spline) {
                float p0 = i > 0 ? keys.get(i - 1).post(ctx)[axis] : aPost[axis];
                float p3 = i + 2 < keys.size() ? keys.get(i + 2).pre(ctx)[axis] : bPre[axis];
                result[axis] = Interpolation.catmullRom(p0, aPost[axis], bPre[axis], p3, f);
            } else {
                result[axis] = Interpolation.lerp(aPost[axis], bPre[axis], f);
            }
        }
        return result;
    }
}
