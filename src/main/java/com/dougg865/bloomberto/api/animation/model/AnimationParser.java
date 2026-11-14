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
 * Reads Blockbench animation files ({@code .animation.json}). Values are numbers or Molang expressions (see {@code api.animation.molang}); expressions
 * are compiled once and evaluated every frame.
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
            Vals v = vec(e, where, scalarFallback);
            keys.add(new AnimationClip.Keyframe(0f, v.c, v.c, AnimationClip.Interp.LINEAR, v.e, v.e));
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
            Vals pre;
            Vals post;
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
            keys.add(new AnimationClip.Keyframe(time, pre.c, post.c, interp, pre.e, post.e));
        }
        keys.sort(java.util.Comparator.comparingDouble(AnimationClip.Keyframe::time));
        return keys;
    }

    /** Constant parts of a vector and, for axes that are Molang expressions, the compiled expression (or null). */
    record Vals(float[] c, com.dougg865.bloomberto.api.animation.molang.Molang[] e) {
    }

    private static Vals vec(JsonElement e, String where, float scalarFallback) throws ParseException {
        if (e == null) {
            throw new ParseException("A keyframe in " + where + " has no value");
        }
        if (e.isJsonPrimitive()) {
            return fill(new JsonElement[] {e, e, e}, where);
        }
        JsonArray a = e.getAsJsonArray();
        if (a.size() != 3) {
            throw new ParseException("A keyframe value in " + where + " must have three numbers or expressions");
        }
        return fill(new JsonElement[] {a.get(0), a.get(1), a.get(2)}, where);
    }

    static Vals fill(JsonElement[] items, String where) throws ParseException {
        float[] c = new float[3];
        com.dougg865.bloomberto.api.animation.molang.Molang[] expr = null;
        for (int i = 0; i < 3; i++) {
            JsonElement item = items[i];
            if (item.isJsonPrimitive() && item.getAsJsonPrimitive().isString()
                    && !com.dougg865.bloomberto.api.animation.molang.Molang.isNumber(item.getAsString())) {
                try {
                    if (expr == null) {
                        expr = new com.dougg865.bloomberto.api.animation.molang.Molang[3];
                    }
                    expr[i] = com.dougg865.bloomberto.api.animation.molang.Molang.compile(item.getAsString());
                } catch (com.dougg865.bloomberto.api.animation.molang.MolangException ex) {
                    throw new ParseException("Expression '" + item.getAsString() + "' in " + where + ": " + ex.getMessage(), ex);
                }
            } else {
                c[i] = number(item, where);
            }
        }
        return new Vals(c, expr);
    }

    private static float number(JsonElement e, String where) throws ParseException {
        try {
            return e.getAsFloat();
        } catch (NumberFormatException | UnsupportedOperationException ex) {
            throw new ParseException("Value '" + e + "' in " + where + " is not a number or an expression", ex);
        }
    }
}
