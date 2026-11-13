package com.dougg865.bloomberto.api.config;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** The configs registered with {@link ConfigBuilder#build()}. */
public final class Configs {
    private static final Map<String, Config> CONFIGS = new ConcurrentHashMap<>();

    private Configs() {
    }

    static void register(Config config) {
        if (CONFIGS.putIfAbsent(config.modId(), config) != null) {
            throw new IllegalStateException("A config for '" + config.modId() + "' is already registered");
        }
    }

    public static Config get(String modId) {
        return CONFIGS.get(modId);
    }

    public static Collection<Config> all() {
        return CONFIGS.values();
    }
}
