package com.dougg865.bloomberto.api.config;

import com.dougg865.bloomberto.impl.config.Toml;

/**
 * One typed option of a {@link Config}. Reading {@link #get()} is cheap and thread-safe; changes made with
 * {@link #set(Object)} are written to disk by {@link Config#save()}.
 */
public final class ConfigValue<T> {
    private final String path;
    private final String comment;
    private final T defaultValue;
    private final ValueType<T> type;

    private volatile T value;
    private volatile T serverOverride;
    private boolean synced;

    ConfigValue(String path, String comment, T defaultValue, ValueType<T> type) {
        this.path = path;
        this.comment = comment;
        this.defaultValue = defaultValue;
        this.type = type;
        this.value = defaultValue;
    }

    /** The effective value: the one received from the server while connected to one, otherwise the local value. */
    public T get() {
        T override = serverOverride;
        return override != null ? override : value;
    }

    /** Sets the local value (the one saved to disk). */
    public void set(T newValue) {
        // Round-trip through the type so out-of-range values are corrected the same way as when loading.
        T checked = type.fromToml(type.toToml(newValue));
        this.value = checked != null ? checked : defaultValue;
    }

    public void reset() {
        this.value = defaultValue;
    }

    /** Marks this option as owned by the server: its value is sent to clients when they join. */
    public ConfigValue<T> synced() {
        this.synced = true;
        return this;
    }

    public boolean isSynced() {
        return synced;
    }

    public String path() {
        return path;
    }

    public String comment() {
        return comment;
    }

    public T defaultValue() {
        return defaultValue;
    }

    public ValueType<T> type() {
        return type;
    }

    /** The local value, ignoring any server override. */
    public T localValue() {
        return value;
    }

    // --- used by Config / sync ---

    /** @return {@code true} if the raw value was valid (possibly after correction), {@code false} if the default was used */
    boolean load(Object raw) {
        T parsed = raw == null ? null : type.fromToml(raw);
        if (parsed == null) {
            this.value = defaultValue;
            return false;
        }
        this.value = parsed;
        return true;
    }

    /** The current local value in TOML form. */
    public String encode() {
        return Toml.format(type.toToml(value));
    }

    /** The default value in TOML form. */
    public String encodeDefault() {
        return Toml.format(type.toToml(defaultValue));
    }

    /** Applies a value received from the server; invalid input is ignored. */
    public void applyServerValue(String encoded) {
        try {
            T parsed = type.fromToml(Toml.parseValue(encoded));
            if (parsed != null) {
                serverOverride = parsed;
            }
        } catch (Toml.ParseException ignored) {
            // keep the previous value
        }
    }

    public void clearServerOverride() {
        serverOverride = null;
    }
}
