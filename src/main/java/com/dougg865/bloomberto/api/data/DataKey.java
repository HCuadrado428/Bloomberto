package com.dougg865.bloomberto.api.data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;

/**
 * A named piece of data that can be attached to players and other entities with {@link EntityData}.
 * Create keys once (a {@code static final} field) while your mod initializes, on both client and server.
 *
 * <pre>{@code
 * public static final DataKey<Integer> MANA = DataKey.ofInt(Bloomberto.id("mymod", "mana"), 0).synced();
 * }</pre>
 */
public final class DataKey<T> {
    private static final Map<ResourceLocation, DataKey<?>> REGISTRY = new ConcurrentHashMap<>();

    private final ResourceLocation id;
    private final DataSerializer<T> serializer;
    private final Supplier<T> defaultValue;
    private boolean synced;

    private DataKey(ResourceLocation id, DataSerializer<T> serializer, Supplier<T> defaultValue) {
        this.id = id;
        this.serializer = serializer;
        this.defaultValue = defaultValue;
    }

    /** Creates and registers a key. The default is created on demand, so mutable defaults are safe. */
    public static <T> DataKey<T> of(ResourceLocation id, DataSerializer<T> serializer, Supplier<T> defaultValue) {
        DataKey<T> key = new DataKey<>(id, serializer, defaultValue);
        if (REGISTRY.putIfAbsent(id, key) != null) {
            throw new IllegalStateException("Data key already registered: " + id);
        }
        return key;
    }

    public static DataKey<Integer> ofInt(ResourceLocation id, int defaultValue) {
        return of(id, DataSerializer.INT, () -> defaultValue);
    }

    public static DataKey<Double> ofDouble(ResourceLocation id, double defaultValue) {
        return of(id, DataSerializer.DOUBLE, () -> defaultValue);
    }

    public static DataKey<Boolean> ofBoolean(ResourceLocation id, boolean defaultValue) {
        return of(id, DataSerializer.BOOLEAN, () -> defaultValue);
    }

    public static DataKey<String> ofString(ResourceLocation id, String defaultValue) {
        return of(id, DataSerializer.STRING, () -> defaultValue);
    }

    /**
     * Sends this data to the client of the player it belongs to, so {@link EntityData#get} also works on the
     * client for that player. Only players are synced; other entities keep their data on the server.
     */
    public DataKey<T> synced() {
        this.synced = true;
        return this;
    }

    public boolean isSynced() {
        return synced;
    }

    public ResourceLocation id() {
        return id;
    }

    public DataSerializer<T> serializer() {
        return serializer;
    }

    public T defaultValue() {
        return defaultValue.get();
    }

    /** Looks up a registered key by id, or {@code null}. */
    public static DataKey<?> byId(ResourceLocation id) {
        return REGISTRY.get(id);
    }
}
