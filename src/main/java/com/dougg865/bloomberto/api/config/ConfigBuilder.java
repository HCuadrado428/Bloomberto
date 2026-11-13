package com.dougg865.bloomberto.api.config;

import com.dougg865.bloomberto.platform.Services;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Declares the options of a mod's config file.
 *
 * <pre>{@code
 * ConfigBuilder b = ConfigBuilder.create("mymod");
 * b.push("general");
 * ConfigValue<Boolean> enabled = b.defineBoolean("enabled", true, "Turns the feature on");
 * ConfigValue<Integer> radius = b.defineInt("radius", 8, 1, 64, "Search radius in blocks").synced();
 * b.pop();
 * Config config = b.build(); // loads (or creates) config/mymod.toml
 * }</pre>
 */
public final class ConfigBuilder {
    private final String modId;
    private final Deque<String> sections = new ArrayDeque<>();
    private final List<ConfigValue<?>> values = new ArrayList<>();
    private final Set<String> paths = new HashSet<>();

    private ConfigBuilder(String modId) {
        this.modId = modId;
    }

    public static ConfigBuilder create(String modId) {
        return new ConfigBuilder(modId);
    }

    /** Starts a section (a TOML table); sections can be nested. */
    public ConfigBuilder push(String section) {
        if (section.isBlank() || section.contains(".")) {
            throw new IllegalArgumentException("Invalid section name: '" + section + "'");
        }
        sections.addLast(section);
        return this;
    }

    public ConfigBuilder pop() {
        if (sections.isEmpty()) {
            throw new IllegalStateException("pop() without push()");
        }
        sections.removeLast();
        return this;
    }

    public ConfigValue<Boolean> defineBoolean(String key, boolean defaultValue, String comment) {
        return define(key, defaultValue, comment, ValueType.BOOLEAN);
    }

    public ConfigValue<Integer> defineInt(String key, int defaultValue, int min, int max, String comment) {
        requireInRange(key, defaultValue >= min && defaultValue <= max);
        return define(key, defaultValue, comment, ValueType.intRange(min, max));
    }

    public ConfigValue<Double> defineDouble(String key, double defaultValue, double min, double max, String comment) {
        requireInRange(key, defaultValue >= min && defaultValue <= max);
        return define(key, defaultValue, comment, ValueType.doubleRange(min, max));
    }

    public ConfigValue<String> defineString(String key, String defaultValue, String comment) {
        return define(key, defaultValue, comment, ValueType.STRING);
    }

    public <E extends Enum<E>> ConfigValue<E> defineEnum(String key, E defaultValue, String comment) {
        return define(key, defaultValue, comment, ValueType.enumOf(defaultValue.getDeclaringClass()));
    }

    public ConfigValue<List<String>> defineStringList(String key, List<String> defaultValue, String comment) {
        return define(key, List.copyOf(defaultValue), comment, ValueType.STRING_LIST);
    }

    /** Defines an option with a custom {@link ValueType}. */
    public <T> ConfigValue<T> define(String key, T defaultValue, String comment, ValueType<T> type) {
        if (key.isBlank() || key.contains(".") || key.contains("=") || key.contains("#")) {
            throw new IllegalArgumentException("Invalid key: '" + key + "'");
        }
        String path = sections.isEmpty() ? key : String.join(".", sections) + "." + key;
        if (!paths.add(path)) {
            throw new IllegalStateException("Duplicate config option: " + path);
        }
        ConfigValue<T> value = new ConfigValue<>(path, comment == null ? "" : comment, defaultValue, type);
        values.add(value);
        return value;
    }

    private static void requireInRange(String key, boolean ok) {
        if (!ok) {
            throw new IllegalArgumentException("Default value of '" + key + "' is outside its range");
        }
    }

    /** Loads (creating if needed) {@code <config dir>/<modId>.toml} and registers the config for syncing and screens. */
    public Config build() {
        Config config = build(Services.PLATFORM.getConfigDir().resolve(modId + ".toml"));
        Configs.register(config);
        return config;
    }

    /** Loads the given file without registering the config anywhere. */
    public Config build(Path file) {
        if (!sections.isEmpty()) {
            throw new IllegalStateException("Unclosed section: call pop()");
        }
        Config config = new Config(modId, file, values);
        config.load();
        return config;
    }
}
