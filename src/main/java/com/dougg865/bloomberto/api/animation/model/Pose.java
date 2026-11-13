package com.dougg865.bloomberto.api.animation.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** The animated transforms of a model's bones at one moment. Bones that are not in the pose are not animated. */
public final class Pose {
    private final Map<String, BoneTransform> bones = new HashMap<>();

    /** The transform of a bone, created (as "no change") if it is not there yet. */
    public BoneTransform get(String bone) {
        return bones.computeIfAbsent(bone, k -> new BoneTransform());
    }

    public boolean has(String bone) {
        return bones.containsKey(bone);
    }

    public Set<String> boneNames() {
        return bones.keySet();
    }

    /**
     * Blends two poses: bones in only one of them blend with "no change". {@code t = 0} gives {@code from}, 1 gives {@code to}.
     */
    public static Pose blend(Pose from, Pose to, float t) {
        Pose result = new Pose();
        Set<String> names = new java.util.HashSet<>(from.bones.keySet());
        names.addAll(to.bones.keySet());
        for (String name : names) {
            BoneTransform a = from.bones.getOrDefault(name, new BoneTransform());
            BoneTransform b = to.bones.getOrDefault(name, new BoneTransform());
            result.bones.put(name, a.lerp(b, t));
        }
        return result;
    }
}
