package com.dougg865.bloomberto.impl.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.Tag;

/** The synced values the client knows about; cleared when the client disconnects. */
public final class ClientEntityData {
    private static final Map<UUID, Map<String, Tag>> CACHE = new HashMap<>();

    private ClientEntityData() {
    }

    public static synchronized Tag get(UUID entity, String key) {
        Map<String, Tag> map = CACHE.get(entity);
        return map == null ? null : map.get(key);
    }

    public static synchronized void set(UUID entity, String key, Tag value) {
        CACHE.computeIfAbsent(entity, id -> new HashMap<>()).put(key, value);
    }

    public static synchronized void remove(UUID entity, String key) {
        Map<String, Tag> map = CACHE.get(entity);
        if (map != null) {
            map.remove(key);
        }
    }

    public static synchronized void clear() {
        CACHE.clear();
    }
}
