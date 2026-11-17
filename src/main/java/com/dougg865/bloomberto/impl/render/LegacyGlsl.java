package com.dougg865.bloomberto.impl.render;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns a legacy post-processing fragment shader ({@code #version 150}, plain {@code uniform float/vec*} declarations)
 * into one that Minecraft 1.21.11's renderer can run: plain uniforms are not supported there, so they move into one
 * std140 uniform block, {@code BloombertoParams}, with one {@code vec4} slot per uniform. A {@code #define} keeps every
 * old name working, so the shader body is untouched. Pure Java: no game classes.
 */
public final class LegacyGlsl {
    /** The name of the uniform block holding every non-sampler uniform. */
    public static final String BLOCK = "BloombertoParams";

    private static final Pattern VERSION = Pattern.compile("^\\s*#version\\s+\\d+.*$", Pattern.MULTILINE);
    private static final Pattern UNIFORM = Pattern.compile(
            "^\\s*uniform\\s+(?:(?:lowp|mediump|highp)\\s+)?(\\w+)\\s+([\\w\\s,\\[\\]]+?)\\s*;[ \\t]*(//.*)?$",
            Pattern.MULTILINE);
    private static final Pattern NAME = Pattern.compile("^(\\w+)$");

    /** One uniform moved into the block: its index in the block, type and float count (1 to 4, or 16 for a mat4). */
    public record Slot(int index, String type, int components) {
        /** Number of {@code vec4} rows this slot takes in the block. */
        public int rows() {
            return components == 16 ? 4 : 1;
        }
    }

    /** The translated sources and the block layout the Java side must fill. */
    public record Program(List<String> sources, Map<String, Slot> slots, List<String> samplers) {
        /** Size of the uniform block in bytes (at least one row). */
        public int blockSize() {
            return Math.max(1, slots.values().stream().mapToInt(Slot::rows).sum()) * 16;
        }

        /** The slots in block order. */
        public List<Map.Entry<String, Slot>> ordered() {
            List<Map.Entry<String, Slot>> list = new ArrayList<>(slots.entrySet());
            list.sort(java.util.Comparator.comparingInt(e -> e.getValue().index()));
            return list;
        }
    }

    /** A single translated shader. */
    public record Result(String source, Map<String, Slot> slots, List<String> samplers) {
        public int blockSize() {
            return new Program(List.of(source), slots, samplers).blockSize();
        }
    }

    private LegacyGlsl() {
    }

    /**
     * @param source the legacy fragment shader
     * @param extra  uniforms to reserve even if the shader does not declare them (e.g. {@code InSize}); they get slots
     *               after the declared ones
     * @throws IllegalArgumentException for a uniform type the block cannot hold (arrays, unknown types)
     */
    public static Result translate(String source, List<String> extra) {
        Program p = translateAll(List.of(source), extra);
        return new Result(p.sources().get(0), p.slots(), p.samplers());
    }

    /**
     * Translates the stages of one program (e.g. vertex and fragment) so they share the same uniform block: a uniform
     * declared in either stage is visible in both.
     */
    public static Program translateAll(List<String> sources, List<String> extra) {
        Map<String, Slot> slots = new LinkedHashMap<>();
        List<String> samplers = new ArrayList<>();
        List<String> bodies = new ArrayList<>();
        for (String source : sources) {
            bodies.add(strip(source, slots, samplers));
        }
        for (String name : extra) {
            if (!slots.containsKey(name)) {
                slots.put(name, new Slot(slots.size(), "vec4", 4));
            }
        }
        StringBuilder header = new StringBuilder("#version 330\n\nlayout(std140) uniform " + BLOCK + " {\n");
        if (slots.isEmpty()) {
            header.append("    vec4 _bb_unused;\n");
        }
        for (Map.Entry<String, Slot> e : slots.entrySet()) {
            header.append(e.getValue().components() == 16 ? "    mat4 _bb_" : "    vec4 _bb_").append(e.getKey()).append(";\n");
        }
        header.append("};\n");
        for (Map.Entry<String, Slot> e : slots.entrySet()) {
            header.append("#define ").append(e.getKey()).append(' ').append(access(e.getKey(), e.getValue().type())).append('\n');
        }
        List<String> out = new ArrayList<>();
        for (String body : bodies) {
            Matcher v = VERSION.matcher(body);
            if (v.find()) {
                body = body.substring(0, v.start()) + body.substring(v.end());
            }
            out.add(header + body);
        }
        return new Program(List.copyOf(out), Map.copyOf(slots), List.copyOf(samplers));
    }

    /**
     * Lays out uniform values as the block's floats (std140: one {@code vec4} row per slot, four for a {@code mat4},
     * column-major). Missing values and missing components are 0.
     */
    public static float[] pack(Map<String, Slot> slots, Map<String, float[]> values) {
        List<Map.Entry<String, Slot>> ordered = new ArrayList<>(slots.entrySet());
        ordered.sort(java.util.Comparator.comparingInt(e -> e.getValue().index()));
        int rows = Math.max(1, ordered.stream().mapToInt(e -> e.getValue().rows()).sum());
        float[] out = new float[rows * 4];
        int row = 0;
        for (Map.Entry<String, Slot> e : ordered) {
            float[] v = values.get(e.getKey());
            int n = e.getValue().rows() * 4;
            if (v != null) {
                System.arraycopy(v, 0, out, row * 4, Math.min(n, v.length));
            }
            row += e.getValue().rows();
        }
        return out;
    }

    private static String strip(String source, Map<String, Slot> slots, List<String> samplers) {
        StringBuilder body = new StringBuilder();
        Matcher m = UNIFORM.matcher(source);
        int last = 0;
        while (m.find()) {
            String type = m.group(1);
            body.append(source, last, m.start());
            last = m.end();
            for (String raw : m.group(2).split(",")) {
                String name = raw.trim();
                if (!NAME.matcher(name).matches()) {
                    throw new IllegalArgumentException("Unsupported uniform declaration: " + m.group().trim());
                }
                if (type.startsWith("sampler")) {
                    if (!samplers.contains(name)) {
                        samplers.add(name);
                    }
                    body.append("uniform ").append(type).append(' ').append(name).append(";\n");
                    continue;
                }
                int components = components(type);
                if (components == 0) {
                    throw new IllegalArgumentException("Unsupported uniform type " + type + " for " + name);
                }
                Slot existing = slots.get(name);
                if (existing == null) {
                    slots.put(name, new Slot(slots.size(), type, components));
                } else if (!existing.type().equals(type)) {
                    throw new IllegalArgumentException("Uniform " + name + " is " + existing.type() + " in one stage and " + type + " in another");
                }
            }
        }
        body.append(source, last, source.length());
        return body.toString();
    }

    private static int components(String type) {
        return switch (type) {
            case "float", "int", "bool" -> 1;
            case "vec2", "ivec2" -> 2;
            case "vec3", "ivec3" -> 3;
            case "vec4", "ivec4" -> 4;
            case "mat4" -> 16;
            default -> 0;
        };
    }

    private static String access(String name, String type) {
        String f = "_bb_" + name;
        return switch (type) {
            case "float" -> "(" + f + ".x)";
            case "int" -> "int(" + f + ".x)";
            case "bool" -> "(" + f + ".x != 0.0)";
            case "vec2" -> "(" + f + ".xy)";
            case "vec3" -> "(" + f + ".xyz)";
            case "ivec2" -> "ivec2(" + f + ".xy)";
            case "ivec3" -> "ivec3(" + f + ".xyz)";
            case "ivec4" -> "ivec4(" + f + ")";
            default -> "(" + f + ")";
        };
    }
}
