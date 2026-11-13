package com.dougg865.bloomberto.impl.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** The data of every entity, keyed by UUID and then by data key id. Plain logic; persistence is in {@link EntityDataSavedData}. */
public final class EntityDataStore {
    private final Map<UUID, CompoundTag> data = new HashMap<>();

    public Tag get(UUID entity, String key) {
        CompoundTag tag = data.get(entity);
        return tag == null ? null : tag.get(key);
    }

    public void set(UUID entity, String key, Tag value) {
        data.computeIfAbsent(entity, id -> new CompoundTag()).put(key, value);
    }

    /** @return whether something was removed */
    public boolean remove(UUID entity, String key) {
        CompoundTag tag = data.get(entity);
        if (tag == null || !tag.contains(key)) {
            return false;
        }
        tag.remove(key);
        if (tag.isEmpty()) {
            data.remove(entity);
        }
        return true;
    }

    /** @return whether the entity had any data */
    public boolean clear(UUID entity) {
        return data.remove(entity) != null;
    }

    /** A copy of all data of one entity ({@code key -> tag}); empty if none. */
    public Map<String, Tag> entries(UUID entity) {
        CompoundTag tag = data.get(entity);
        Map<String, Tag> result = new HashMap<>();
        if (tag != null) {
            for (String key : tag.getAllKeys()) {
                result.put(key, tag.get(key));
            }
        }
        return result;
    }

    public int entityCount() {
        return data.size();
    }

    public CompoundTag toTag() {
        CompoundTag root = new CompoundTag();
        data.forEach((id, tag) -> root.put(id.toString(), tag.copy()));
        return root;
    }

    public static EntityDataStore fromTag(CompoundTag root) {
        EntityDataStore store = new EntityDataStore();
        for (String key : root.getAllKeys()) {
            try {
                UUID id = UUID.fromString(key);
                if (root.get(key) instanceof CompoundTag tag && !tag.isEmpty()) {
                    store.data.put(id, tag.copy());
                }
            } catch (IllegalArgumentException ignored) {
                // skip entries with a malformed UUID
            }
        }
        return store;
    }
}
