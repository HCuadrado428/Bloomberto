package com.dougg865.bloomberto.api.animation.molang;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * The values a Molang expression can read: {@code query.*}, {@code variable.*} (also {@code temp.*}). Names are lower case and
 * without the prefix alias, e.g. {@code query.anim_time} is stored as {@code "query.anim_time"}. Unknown names read as 0.
 */
public final class MolangContext {
    private final Map<String, Double> values = new HashMap<>();
    private final Random random = new Random(1234L);

    /** Sets {@code name} (e.g. {@code "query.anim_time"} or {@code "variable.speed"}); aliases like {@code q.} are accepted. */
    public MolangContext set(String name, double value) {
        values.put(Molang.canonical(name), value);
        return this;
    }

    public double get(String name) {
        return values.getOrDefault(Molang.canonical(name), 0.0);
    }

    public MolangContext copyFrom(MolangContext other) {
        values.putAll(other.values);
        return this;
    }

    double random() {
        return random.nextDouble();
    }
}
