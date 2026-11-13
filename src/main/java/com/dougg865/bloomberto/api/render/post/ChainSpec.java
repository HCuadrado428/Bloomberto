package com.dougg865.bloomberto.api.render.post;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The description of a post-processing chain: temporary render targets and the passes that read one target and write
 * another. It is plain data; Bloomberto turns it into a real {@code PostChain} on the render thread.
 *
 * <pre>{@code
 * spec.target("swap");
 * spec.pass("mymod:myshader", ChainSpec.MAIN, "swap");
 * spec.pass("blit", "swap", ChainSpec.MAIN);   // vanilla "blit" copies a target
 * }</pre>
 *
 * Minecraft looks programs up only in its own namespace. A program named {@code mymod_blur} is the file
 * {@code assets/minecraft/shaders/program/mymod_blur.json}, whose {@code "vertex"} and {@code "fragment"} are the shaders
 * {@code assets/minecraft/shaders/program/<name>.vsh} / {@code .fsh}. Use a unique prefix for your mod: a name with a
 * {@code namespace:} is rejected by vanilla. Vanilla's own programs ({@code blit}, {@code box_blur}, ...) can be used by name.
 */
public final class ChainSpec {
    /** The name of the screen's main render target. */
    public static final String MAIN = "minecraft:main";

    /** One pass of the chain. */
    public static final class Pass {
        private final String program;
        private final String in;
        private final String out;
        private final Map<String, String> aux = new LinkedHashMap<>();
        private final Map<String, float[]> constants = new LinkedHashMap<>();
        private boolean linearFilter;

        Pass(String program, String in, String out) {
            this.program = program;
            this.in = in;
            this.out = out;
        }

        public String program() {
            return program;
        }

        public String in() {
            return in;
        }

        public String out() {
            return out;
        }

        /** Sample the input with linear filtering (needed by Minecraft's box blur on 1.21). */
        public Pass linearFilter() {
            this.linearFilter = true;
            return this;
        }

        public boolean isLinearFilter() {
            return linearFilter;
        }

        /** Also binds another target's color as the sampler {@code sampler} of this pass. */
        public Pass aux(String sampler, String target) {
            aux.put(sampler, target);
            return this;
        }

        public Map<String, String> auxTargets() {
            return Map.copyOf(aux);
        }

        /** A uniform set once when the chain is built (e.g. a blur direction). */
        public Pass uniform(String name, float... values) {
            constants.put(name, values.clone());
            return this;
        }

        public Map<String, float[]> constants() {
            return constants;
        }
    }

    private final List<String> targets = new ArrayList<>();
    private final List<Pass> passes = new ArrayList<>();

    /** Declares a temporary render target with the size of the screen. */
    public ChainSpec target(String name) {
        if (name.equals(MAIN) || targets.contains(name)) {
            throw new IllegalArgumentException("Target already exists: " + name);
        }
        targets.add(name);
        return this;
    }

    /** Adds a pass; both targets must already be declared (or be {@link #MAIN}). */
    public Pass pass(String program, String in, String out) {
        requireTarget(in);
        requireTarget(out);
        Pass pass = new Pass(program, in, out);
        passes.add(pass);
        return pass;
    }

    private void requireTarget(String name) {
        if (!name.equals(MAIN) && !targets.contains(name)) {
            throw new IllegalArgumentException("Unknown target: " + name);
        }
    }

    public List<String> targets() {
        return List.copyOf(targets);
    }

    public List<Pass> passes() {
        return List.copyOf(passes);
    }

    /** Checks aux targets too; called before the chain is built. */
    public void validate() {
        for (Pass p : passes) {
            for (String t : p.aux.values()) {
                requireTarget(t);
            }
        }
        if (passes.isEmpty()) {
            throw new IllegalStateException("A chain needs at least one pass");
        }
    }
}
