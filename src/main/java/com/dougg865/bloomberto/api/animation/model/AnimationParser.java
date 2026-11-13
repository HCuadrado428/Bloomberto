package com.dougg865.bloomberto.api.animation.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads Blockbench animation files ({@code .animation.json}). Values must be numbers (or numeric strings); Molang
 * expressions are rejected with a clear error.
 */
public final class AnimationParser {
    private AnimationParser() {
    }

    /** @return the clips by name (e.g. {@code animation.golem.walk}), in file order */
    public static Map<String, AnimationClip> parse(String json) throws ParseException {
        JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            throw new ParseException("Not a valid JSON object: " + e.getMessage(), e);
        }
        if (!root.has("animations") || !root.get("animations").isJsonObject()) {
            throw new ParseException("The file has no 'animations' object");
        }
        Map<String, AnimationClip> clips = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("animations").entrySet()) {
            clips.put(entry.getKey(), clip(entry.getKey(), entry.getValue().getAsJsonObject()));
        }
        return clips;
    }

    private static AnimationClip clip(String name, JsonObject a) throws ParseException {
        AnimationClip.Loop loop = AnimationClip.Loop.ONCE;
        if (a.has("loop")) {
            JsonElement l = a.get("loop");
            if (l.isJsonPrimitive() && l.getAsJsonPrimitive().isBoolean()) {
                loop = l.getAsBoolean() ? AnimationClip.Loop.LOOP : AnimationClip.Loop.ONCE;
            } else if ("hold_on_last_frame".equals(l.getAsString())) {
                loop = AnimationClip.Loop.HOLD;
            }
        }
        Map<String, AnimationClip.BoneChannels> bones = new LinkedHashMap<>();
        float maxTime = 0f;
        if (a.has("bones")) {
            for (Map.Entry<String, JsonElement> b : a.getAsJsonObject("bones").entrySet()) {
                JsonObject channels = b.getValue().getAsJsonObject();
                List<AnimationClip.Keyframe> pos = track(name, b.getKey(), "position", channels, 0f);
                List<AnimationClip.Keyframe> rot = track(name, b.getKey(), "rotation", channels, 0f);
                List<AnimationClip.Keyframe> scl = track(name, b.getKey(), "scale", channels, 1f);
                for (List<AnimationClip.Keyframe> t : java.util.Arrays.asList(pos, rot, scl)) {
                    if (t != null && !t.isEmpty()) {
                        maxTime = Math.max(maxTime, t.get(t.size() - 1).time());
                    }
                }
                bones.put(b.getKey(), new AnimationClip.BoneChannels(pos, rot, scl));
            }
        }
        float length = a.has("animation_length") ? a.get("animation_length").getAsFloat() : maxTime;
        return new AnimationClip(name, length, loop, bones);
    }

    private static List<AnimationClip.Keyframe> track(String clip, String bone, String channel, JsonObject channels,
                                                      float scalarFallback) throws ParseException {
        if (!channels.has(channel)) {
            return null;
        }
        String where = clip + " / " + bone + " / " + channel;
        JsonElement e = channels.get(channel);
        List<AnimationClip.Keyframe> keys = new ArrayList<>();
        if (e.isJsonArray() || e.isJsonPrimitive()) {
            // a constant value, no keyframes
            float[] v = vec(e, where, scalarFallback);
            keys.add(new AnimationClip.Keyframe(0f, v, v, AnimationClip.Interp.LINEAR));
            return keys;
        }
        for (Map.Entry<String, JsonElement> k : e.getAsJsonObject().entrySet()) {
            float time;
            try {
                time = Float.parseFloat(k.getKey());
            } catch (NumberFormatException ex) {
                throw new ParseException("Keyframe time '" + k.getKey() + "' in " + where + " is not a number", ex);
            }
            JsonElement value = k.getValue();
            float[] pre;
            float[] post;
            AnimationClip.Interp interp = AnimationClip.Interp.LINEAR;
            if (value.isJsonObject()) {
                JsonObject o = value.getAsJsonObject();
                post = o.has("post") ? vec(o.get("post"), where, scalarFallback) : vec(o.get("pre"), where, scalarFallback);
                pre = o.has("pre") ? vec(o.get("pre"), where, scalarFallback) : post;
                if (o.has("lerp_mode")) {
                    String mode = o.get("lerp_mode").getAsString();
                    interp = "catmullrom".equals(mode) ? AnimationClip.Interp.CATMULLROM
                            : "step".equals(mode) ? AnimationClip.Interp.STEP : AnimationClip.Interp.LINEAR;
                }
            } else {
                pre = vec(value, where, scalarFallback);
                post = pre;
            }
            keys.add(new AnimationClip.Keyframe(time, pre, post, interp));
        }
        keys.sort(java.util.Comparator.comparingDouble(AnimationClip.Keyframe::time));
        return keys;
    }

    private static float[] vec(JsonElement e, String where, float scalarFallback) throws ParseException {
        if (e == null) {
            throw new ParseException("A keyframe in " + where + " has no value");
        }
        if (e.isJsonPrimitive()) {
            float v = number(e, where);
            return new float[] {v, v, v};
        }
        JsonArray a = e.getAsJsonArray();
        if (a.size() != 3) {
            throw new ParseException("A keyframe value in " + where + " must have three numbers");
        }
        return new float[] {number(a.get(0), where), number(a.get(1), where), number(a.get(2), where)};
    }

    private static float number(JsonElement e, String where) throws ParseException {
        try {
            return e.getAsFloat();
        } catch (NumberFormatException | UnsupportedOperationException ex) {
            throw new ParseException("Value '" + e + "' in " + where + " is not a number; Molang expressions are not supported yet", ex);
        }
    }
}
