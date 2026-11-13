package com.dougg865.bloomberto.api.animation.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Reads Bedrock geometry files as exported by Blockbench: format 1.12.0 ({@code minecraft:geometry}) and the older 1.8.0. */
public final class GeoParser {
    private GeoParser() {
    }

    /**
     * @param geometryId which geometry to read when the file has several (e.g. {@code "geometry.golem"}); {@code null} = the first
     */
    public static GeoModel parse(String json, String geometryId) throws ParseException {
        JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            throw new ParseException("Not a valid JSON object: " + e.getMessage(), e);
        }
        if (root.has("minecraft:geometry")) {
            JsonArray array = root.getAsJsonArray("minecraft:geometry");
            for (JsonElement e : array) {
                JsonObject geometry = e.getAsJsonObject();
                JsonObject description = obj(geometry, "description");
                String id = description == null ? "" : str(description, "identifier", "");
                if (geometryId == null || geometryId.equals(id)) {
                    int w = description == null ? 64 : intOf(description, "texture_width", 64);
                    int h = description == null ? 64 : intOf(description, "texture_height", 64);
                    return build(id, w, h, geometry);
                }
            }
            throw new ParseException("Geometry '" + geometryId + "' not found in the file");
        }
        // 1.8.0: each "geometry.xxx" key is a geometry; texture size is texturewidth / textureheight.
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            if (entry.getKey().startsWith("geometry.") && entry.getValue().isJsonObject()) {
                if (geometryId == null || geometryId.equals(entry.getKey())) {
                    JsonObject g = entry.getValue().getAsJsonObject();
                    return build(entry.getKey(), intOf(g, "texturewidth", 64), intOf(g, "textureheight", 64), g);
                }
            }
        }
        throw new ParseException("No geometry found: expected 'minecraft:geometry' or a 'geometry.*' key");
    }

    public static GeoModel parse(String json) throws ParseException {
        return parse(json, null);
    }

    private static GeoModel build(String id, int textureWidth, int textureHeight, JsonObject geometry) throws ParseException {
        if (textureWidth <= 0 || textureHeight <= 0) {
            throw new ParseException("Invalid texture size " + textureWidth + "x" + textureHeight);
        }
        JsonArray boneArray = geometry.has("bones") ? geometry.getAsJsonArray("bones") : new JsonArray();
        List<GeoModel.Bone> bones = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (JsonElement e : boneArray) {
            JsonObject b = e.getAsJsonObject();
            String name = str(b, "name", null);
            if (name == null) {
                throw new ParseException("A bone has no name");
            }
            if (!names.add(name)) {
                throw new ParseException("Duplicate bone name: " + name);
            }
            List<GeoModel.Cube> cubes = new ArrayList<>();
            if (b.has("cubes")) {
                for (JsonElement c : b.getAsJsonArray("cubes")) {
                    cubes.add(cube(name, c.getAsJsonObject()));
                }
            }
            bones.add(new GeoModel.Bone(name, str(b, "parent", null), vec(b, "pivot", 0f, name), vec(b, "rotation", 0f, name),
                    bool(b, "mirror"), cubes));
        }
        for (GeoModel.Bone bone : bones) {
            if (bone.parent() != null && !names.contains(bone.parent())) {
                throw new ParseException("Bone '" + bone.name() + "' has an unknown parent '" + bone.parent() + "'");
            }
        }
        checkNoCycles(bones);
        return new GeoModel(id, textureWidth, textureHeight, bones);
    }

    private static void checkNoCycles(List<GeoModel.Bone> bones) throws ParseException {
        Map<String, String> parent = new HashMap<>();
        bones.forEach(b -> parent.put(b.name(), b.parent()));
        for (GeoModel.Bone b : bones) {
            Set<String> seen = new HashSet<>();
            String current = b.name();
            while (current != null) {
                if (!seen.add(current)) {
                    throw new ParseException("Bone hierarchy has a cycle at '" + current + "'");
                }
                current = parent.get(current);
            }
        }
    }

    private static GeoModel.Cube cube(String bone, JsonObject c) throws ParseException {
        float[] origin = vec(c, "origin", 0f, bone);
        float[] size = vec(c, "size", 0f, bone);
        float[] pivot = c.has("pivot") ? vec(c, "pivot", 0f, bone) : null;
        float[] rotation = vec(c, "rotation", 0f, bone);
        float inflate = c.has("inflate") ? c.get("inflate").getAsFloat() : 0f;
        boolean mirror = bool(c, "mirror");
        GeoModel.Uv uv;
        if (!c.has("uv")) {
            uv = new GeoModel.Uv(0, 0, Map.of());
        } else if (c.get("uv").isJsonArray()) {
            JsonArray a = c.getAsJsonArray("uv");
            if (a.size() < 2) {
                throw new ParseException("Cube uv in bone '" + bone + "' needs two numbers");
            }
            uv = new GeoModel.Uv(a.get(0).getAsFloat(), a.get(1).getAsFloat(), Map.of());
        } else {
            Map<String, GeoModel.UvRegion> faces = new HashMap<>();
            for (Map.Entry<String, JsonElement> face : c.getAsJsonObject("uv").entrySet()) {
                if (!face.getKey().matches("north|south|east|west|up|down")) {
                    continue;
                }
                JsonObject f = face.getValue().getAsJsonObject();
                JsonArray fuv = f.getAsJsonArray("uv");
                JsonArray fsize = f.has("uv_size") ? f.getAsJsonArray("uv_size") : null;
                float w = fsize != null ? fsize.get(0).getAsFloat() : 0f;
                float h = fsize != null ? fsize.get(1).getAsFloat() : 0f;
                faces.put(face.getKey(), new GeoModel.UvRegion(fuv.get(0).getAsFloat(), fuv.get(1).getAsFloat(), w, h));
            }
            uv = new GeoModel.Uv(0, 0, faces);
        }
        return new GeoModel.Cube(origin, size, pivot, rotation, inflate, mirror, uv);
    }

    private static float[] vec(JsonObject o, String key, float fallback, String where) throws ParseException {
        if (!o.has(key)) {
            return new float[] {fallback, fallback, fallback};
        }
        JsonElement e = o.get(key);
        if (!e.isJsonArray() || e.getAsJsonArray().size() != 3) {
            throw new ParseException("'" + key + "' in '" + where + "' must be three numbers");
        }
        JsonArray a = e.getAsJsonArray();
        try {
            return new float[] {a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
        } catch (NumberFormatException | UnsupportedOperationException | IllegalStateException ex) {
            throw new ParseException("'" + key + "' in '" + where + "' has a value that is not a number (Molang expressions are not supported)", ex);
        }
    }

    private static JsonObject obj(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonObject() ? o.getAsJsonObject(key) : null;
    }

    private static String str(JsonObject o, String key, String fallback) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : fallback;
    }

    private static int intOf(JsonObject o, String key, int fallback) {
        return o.has(key) ? o.get(key).getAsInt() : fallback;
    }

    private static boolean bool(JsonObject o, String key) {
        return o.has(key) && o.get(key).getAsBoolean();
    }
}
