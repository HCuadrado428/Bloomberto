package com.dougg865.bloomberto.api.animation.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads Blockbench project files ({@code .bbmodel}) directly, so a model does not have to be exported first. The project is
 * turned into the same {@link GeoModel} and {@link AnimationClip}s as the exported Bedrock files, using the conversions
 * Blockbench applies when it exports Bedrock geometry (x axis mirrored, x and y rotations negated).
 *
 * <p>Supported: cubes (box UV and per-face UV), groups as bones (nested), cube and group rotations, inflate, mirrored UV,
 * bone animations (rotation, position, scale) with linear, Catmull-Rom and step interpolation, loop modes and Molang values.
 * Cubes outside any group go into a bone called {@code bb_main}. Meshes, locators, bezier handles and effect animators are
 * ignored. Not checked against files from every Blockbench version: if a model looks mirrored, export it as
 * {@code .geo.json} and compare.
 */
public final class BbModelParser {
    private BbModelParser() {
    }

    private static JsonObject root(String json) throws ParseException {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (!root.has("elements") && !root.has("outliner") && !root.has("animations")) {
                throw new ParseException("Not a Blockbench project: no 'elements', 'outliner' or 'animations'");
            }
            return root;
        } catch (JsonParseException | IllegalStateException e) {
            throw new ParseException("Not a valid JSON object: " + e.getMessage(), e);
        }
    }

    /** The model of a project. The identifier is {@code geometry.<name>} from the project name. */
    public static GeoModel parseModel(String json) throws ParseException {
        JsonObject root = root(json);
        int width = 64;
        int height = 64;
        if (root.has("resolution") && root.get("resolution").isJsonObject()) {
            JsonObject r = root.getAsJsonObject("resolution");
            width = r.has("width") ? r.get("width").getAsInt() : 64;
            height = r.has("height") ? r.get("height").getAsInt() : 64;
        }
        if (width <= 0 || height <= 0) {
            throw new ParseException("Invalid texture size " + width + "x" + height);
        }
        Map<String, JsonObject> elements = new HashMap<>();
        if (root.has("elements")) {
            for (JsonElement e : root.getAsJsonArray("elements")) {
                JsonObject o = e.getAsJsonObject();
                if (o.has("uuid") && (!o.has("type") || "cube".equals(o.get("type").getAsString()))) {
                    elements.put(o.get("uuid").getAsString(), o);
                }
            }
        }
        List<GeoModel.Bone> bones = new ArrayList<>();
        List<GeoModel.Cube> loose = new ArrayList<>();
        if (root.has("outliner")) {
            for (JsonElement item : root.getAsJsonArray("outliner")) {
                if (item.isJsonPrimitive()) {
                    JsonObject cube = elements.get(item.getAsString());
                    if (cube != null) {
                        loose.add(cube(cube));
                    }
                } else {
                    group(item.getAsJsonObject(), null, elements, bones);
                }
            }
        } else {
            for (JsonObject cube : elements.values()) {
                loose.add(cube(cube));
            }
        }
        if (!loose.isEmpty()) {
            bones.add(new GeoModel.Bone("bb_main", null, new float[3], new float[3], false, loose));
        }
        String name = root.has("name") ? root.get("name").getAsString() : "model";
        return new GeoModel("geometry." + name, width, height, bones);
    }

    private static void group(JsonObject g, String parent, Map<String, JsonObject> elements, List<GeoModel.Bone> out)
            throws ParseException {
        String name = g.has("name") ? g.get("name").getAsString() : "group";
        float[] origin = vec(g, "origin");
        float[] rotation = vec(g, "rotation");
        List<GeoModel.Cube> cubes = new ArrayList<>();
        List<JsonObject> childGroups = new ArrayList<>();
        if (g.has("children")) {
            for (JsonElement c : g.getAsJsonArray("children")) {
                if (c.isJsonPrimitive()) {
                    JsonObject cube = elements.get(c.getAsString());
                    if (cube != null) {
                        cubes.add(cube(cube));
                    }
                } else {
                    childGroups.add(c.getAsJsonObject());
                }
            }
        }
        out.add(new GeoModel.Bone(name, parent, new float[] {neg(origin[0]), origin[1], origin[2]},
                new float[] {neg(rotation[0]), neg(rotation[1]), rotation[2]}, false, cubes));
        for (JsonObject child : childGroups) {
            group(child, name, elements, out);
        }
    }

    private static GeoModel.Cube cube(JsonObject c) throws ParseException {
        float[] from = vec(c, "from");
        float[] to = vec(c, "to");
        float[] size = {to[0] - from[0], to[1] - from[1], to[2] - from[2]};
        float[] originPoint = {neg(from[0] + size[0]), from[1], from[2]};
        float[] rotation = vec(c, "rotation");
        boolean rotated = rotation[0] != 0 || rotation[1] != 0 || rotation[2] != 0;
        float[] pivot = null;
        float[] cubeRotation = new float[3];
        if (rotated) {
            float[] o = c.has("origin") ? vec(c, "origin") : new float[3];
            pivot = new float[] {neg(o[0]), o[1], o[2]};
            cubeRotation = new float[] {neg(rotation[0]), neg(rotation[1]), rotation[2]};
        }
        float inflate = c.has("inflate") ? c.get("inflate").getAsFloat() : 0f;
        boolean mirror = c.has("mirror_uv") && c.get("mirror_uv").getAsBoolean();
        GeoModel.Uv uv;
        boolean box = c.has("box_uv") ? c.get("box_uv").getAsBoolean() : !c.has("faces");
        if (box) {
            float[] off = c.has("uv_offset") ? uvPair(c.getAsJsonArray("uv_offset")) : new float[2];
            uv = new GeoModel.Uv(off[0], off[1], Map.of());
        } else {
            Map<String, GeoModel.UvRegion> faces = new HashMap<>();
            JsonObject f = c.getAsJsonObject("faces");
            for (Map.Entry<String, JsonElement> e : f.entrySet()) {
                JsonObject face = e.getValue().getAsJsonObject();
                if (!face.has("uv") || face.get("uv").isJsonNull()) {
                    continue;
                }
                JsonArray a = face.getAsJsonArray("uv");
                if (a.size() != 4) {
                    throw new ParseException("A face uv of cube '" + name(c) + "' needs four numbers");
                }
                float u1 = a.get(0).getAsFloat();
                float v1 = a.get(1).getAsFloat();
                faces.put(e.getKey(), new GeoModel.UvRegion(u1, v1, a.get(2).getAsFloat() - u1, a.get(3).getAsFloat() - v1));
            }
            uv = new GeoModel.Uv(0, 0, faces);
        }
        return new GeoModel.Cube(originPoint, size, pivot, cubeRotation, inflate, mirror, uv);
    }

    /** Negation that never produces -0. */
    private static float neg(float v) {
        return v == 0f ? 0f : -v;
    }

    private static String name(JsonObject o) {
        return o.has("name") ? o.get("name").getAsString() : "cube";
    }

    private static float[] uvPair(JsonArray a) throws ParseException {
        if (a.size() != 2) {
            throw new ParseException("uv_offset needs two numbers");
        }
        return new float[] {a.get(0).getAsFloat(), a.get(1).getAsFloat()};
    }

    private static float[] vec(JsonObject o, String key) throws ParseException {
        if (!o.has(key) || o.get(key).isJsonNull()) {
            return new float[3];
        }
        JsonElement e = o.get(key);
        if (!e.isJsonArray() || e.getAsJsonArray().size() != 3) {
            throw new ParseException("'" + key + "' must be three numbers");
        }
        JsonArray a = e.getAsJsonArray();
        try {
            return new float[] {a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
        } catch (NumberFormatException | UnsupportedOperationException ex) {
            throw new ParseException("'" + key + "' has a value that is not a number", ex);
        }
    }

    /** The animations of a project, by name, in file order. */
    public static Map<String, AnimationClip> parseAnimations(String json) throws ParseException {
        JsonObject root = root(json);
        Map<String, AnimationClip> clips = new LinkedHashMap<>();
        if (!root.has("animations")) {
            return clips;
        }
        for (JsonElement e : root.getAsJsonArray("animations")) {
            JsonObject a = e.getAsJsonObject();
            String name = a.has("name") ? a.get("name").getAsString() : "animation";
            AnimationClip.Loop loop = AnimationClip.Loop.ONCE;
            if (a.has("loop")) {
                String l = a.get("loop").getAsString();
                loop = "loop".equals(l) ? AnimationClip.Loop.LOOP : "hold".equals(l) ? AnimationClip.Loop.HOLD
                        : AnimationClip.Loop.ONCE;
            }
            float length = a.has("length") ? a.get("length").getAsFloat() : 0f;
            Map<String, AnimationClip.BoneChannels> bones = new LinkedHashMap<>();
            float maxTime = 0f;
            if (a.has("animators")) {
                for (Map.Entry<String, JsonElement> en : a.getAsJsonObject("animators").entrySet()) {
                    JsonObject animator = en.getValue().getAsJsonObject();
                    if (animator.has("type") && !"bone".equals(animator.get("type").getAsString())) {
                        continue;
                    }
                    String bone = animator.has("name") ? animator.get("name").getAsString() : null;
                    if (bone == null || !animator.has("keyframes")) {
                        continue;
                    }
                    Map<String, List<AnimationClip.Keyframe>> channels = new HashMap<>();
                    for (JsonElement k : animator.getAsJsonArray("keyframes")) {
                        JsonObject kf = k.getAsJsonObject();
                        String channel = kf.get("channel").getAsString();
                        if (!channel.equals("rotation") && !channel.equals("position") && !channel.equals("scale")) {
                            continue;
                        }
                        AnimationClip.Keyframe key = keyframe(name + " / " + bone + " / " + channel, channel, kf);
                        maxTime = Math.max(maxTime, key.time());
                        channels.computeIfAbsent(channel, c -> new ArrayList<>()).add(key);
                    }
                    channels.values().forEach(l -> l.sort(java.util.Comparator.comparingDouble(AnimationClip.Keyframe::time)));
                    bones.put(bone, new AnimationClip.BoneChannels(channels.get("position"), channels.get("rotation"),
                            channels.get("scale")));
                }
            }
            clips.put(name, new AnimationClip(name, length > 0 ? length : maxTime, loop, bones));
        }
        return clips;
    }

    private static AnimationClip.Keyframe keyframe(String where, String channel, JsonObject kf) throws ParseException {
        float time = kf.has("time") ? kf.get("time").getAsFloat() : 0f;
        JsonArray points = kf.has("data_points") ? kf.getAsJsonArray("data_points") : new JsonArray();
        if (points.isEmpty()) {
            throw new ParseException("A keyframe in " + where + " has no value");
        }
        AnimationParser.Vals pre = values(where, channel, points.get(0).getAsJsonObject());
        AnimationParser.Vals post = points.size() > 1 ? values(where, channel, points.get(1).getAsJsonObject()) : pre;
        AnimationClip.Interp interp = AnimationClip.Interp.LINEAR;
        if (kf.has("interpolation")) {
            String m = kf.get("interpolation").getAsString();
            interp = "catmullrom".equals(m) ? AnimationClip.Interp.CATMULLROM
                    : "step".equals(m) ? AnimationClip.Interp.STEP : AnimationClip.Interp.LINEAR;
        }
        return new AnimationClip.Keyframe(time, pre.c(), post.c(), interp, pre.e(), post.e());
    }

    private static AnimationParser.Vals values(String where, String channel, JsonObject point) throws ParseException {
        JsonElement[] axis = new JsonElement[3];
        String[] names = {"x", "y", "z"};
        for (int i = 0; i < 3; i++) {
            JsonElement v = point.has(names[i]) ? point.get(names[i]) : new JsonPrimitive(0);
            boolean flip = (channel.equals("rotation") && i < 2) || (channel.equals("position") && i == 0);
            axis[i] = flip ? negate(v) : v;
        }
        return AnimationParser.fill(axis, where);
    }

    /** Negates a number or a Molang expression. */
    private static JsonElement negate(JsonElement v) {
        if (v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber()) {
            return new JsonPrimitive(v.getAsFloat() == 0f ? 0f : -v.getAsFloat());
        }
        String s = v.getAsString().trim();
        try {
            float f = Float.parseFloat(s);
            return new JsonPrimitive(f == 0f ? 0f : -f);
        } catch (NumberFormatException e) {
            return new JsonPrimitive(s.isEmpty() ? "0" : "-(" + s + ")");
        }
    }

    /** The embedded textures (PNG bytes) of a project, in order; entries that are not embedded are {@code null}. */
    public static List<byte[]> embeddedTextures(String json) throws ParseException {
        JsonObject root = root(json);
        List<byte[]> result = new ArrayList<>();
        if (root.has("textures")) {
            for (JsonElement t : root.getAsJsonArray("textures")) {
                JsonObject o = t.getAsJsonObject();
                String source = o.has("source") ? o.get("source").getAsString() : "";
                int comma = source.indexOf(',');
                if (source.startsWith("data:image/png") && comma > 0) {
                    try {
                        result.add(Base64.getDecoder().decode(source.substring(comma + 1)));
                    } catch (IllegalArgumentException e) {
                        throw new ParseException("A texture of the project is not valid base64", e);
                    }
                } else {
                    result.add(null);
                }
            }
        }
        return result;
    }
}
