package com.dougg865.bloomberto.api.animation.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A model exported from Blockbench as a Bedrock geometry ({@code .geo.json}): a tree of bones, each with cubes.
 * Values are as written in the file (Bedrock coordinates, 1/16 block units, degrees); {@link Conventions} converts them.
 */
public record GeoModel(String identifier, int textureWidth, int textureHeight, List<Bone> bones) {
    /** A texture region in pixels. */
    public record UvRegion(float u, float v, float width, float height) {
    }

    /** Where a cube's faces sample the texture: one box layout, or an explicit region for each face. */
    public record Uv(float boxU, float boxV, Map<String, UvRegion> faces) {
        public boolean isPerFace() {
            return !faces.isEmpty();
        }
    }

    /**
     * @param origin   the minimum corner
     * @param pivot    the point the cube rotates around, or {@code null} if it does not rotate on its own
     * @param rotation the cube's own rotation in degrees
     */
    public record Cube(float[] origin, float[] size, float[] pivot, float[] rotation, float inflate, boolean mirror, Uv uv) {
    }

    /** @param parent the parent bone's name, or {@code null} for a root bone */
    public record Bone(String name, String parent, float[] pivot, float[] rotation, boolean mirror, List<Cube> cubes) {
    }

    public GeoModel {
        bones = List.copyOf(bones);
    }

    public Bone bone(String name) {
        for (Bone b : bones) {
            if (b.name().equals(name)) {
                return b;
            }
        }
        return null;
    }

    /** The bones whose parent is {@code parentName} ({@code null} = the roots), in file order. */
    public List<Bone> childrenOf(String parentName) {
        List<Bone> result = new ArrayList<>();
        for (Bone b : bones) {
            if (java.util.Objects.equals(b.parent(), parentName)) {
                result.add(b);
            }
        }
        return result;
    }

    /** Names of all bones, in file order. */
    public Map<String, Bone> bonesByName() {
        Map<String, Bone> map = new LinkedHashMap<>();
        bones.forEach(b -> map.put(b.name(), b));
        return map;
    }
}
