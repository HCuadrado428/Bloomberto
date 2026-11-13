package com.dougg865.bloomberto.api.data;

import com.dougg865.bloomberto.api.network.Network;
import com.dougg865.bloomberto.impl.data.ClientEntityData;
import com.dougg865.bloomberto.impl.data.DataSync;
import com.dougg865.bloomberto.impl.data.EntityDataSavedData;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Attaches your own data to players and other entities, saved with the world and identical on every
 * loader. Data belongs to the entity's UUID, so a player keeps it across death, respawn and dimension changes.
 *
 * <p>Write only on the server. Reading works on the server, and on the client for {@link DataKey#synced()}
 * keys of the local player. Data of entities that no longer exist is not removed automatically; call
 * {@link #clear(Entity)} when you know an entity is gone for good.
 */
public final class EntityData {
    private EntityData() {
    }

    /** The value, or the key's default if none was set. */
    public static <T> T get(Entity entity, DataKey<T> key) {
        Tag tag = entity.level().isClientSide()
                ? ClientEntityData.get(entity.getUUID(), key.id().toString())
                : store(entity).get(entity.getUUID(), key.id().toString());
        T value = tag == null ? null : key.serializer().read(tag);
        return value != null ? value : key.defaultValue();
    }

    public static <T> boolean has(Entity entity, DataKey<T> key) {
        Tag tag = entity.level().isClientSide()
                ? ClientEntityData.get(entity.getUUID(), key.id().toString())
                : store(entity).get(entity.getUUID(), key.id().toString());
        return tag != null && key.serializer().read(tag) != null;
    }

    /** Sets the value. Server only. */
    public static <T> void set(Entity entity, DataKey<T> key, T value) {
        requireServer(entity);
        Tag tag = key.serializer().write(value);
        EntityDataSavedData data = saved(entity);
        data.store().set(entity.getUUID(), key.id().toString(), tag);
        data.setDirty();
        if (key.isSynced() && entity instanceof ServerPlayer player) {
            Network.sendToPlayer(player, new DataSync.Packet(player.getUUID(), key.id(), tag));
        }
    }

    /** Removes the value so {@link #get} returns the default again. Server only. */
    public static <T> void remove(Entity entity, DataKey<T> key) {
        requireServer(entity);
        EntityDataSavedData data = saved(entity);
        if (data.store().remove(entity.getUUID(), key.id().toString())) {
            data.setDirty();
            if (key.isSynced() && entity instanceof ServerPlayer player) {
                Network.sendToPlayer(player, new DataSync.Packet(player.getUUID(), key.id(), null));
            }
        }
    }

    /** Deletes all data of the entity. Server only. */
    public static void clear(Entity entity) {
        requireServer(entity);
        EntityDataSavedData data = saved(entity);
        if (data.store().clear(entity.getUUID())) {
            data.setDirty();
        }
    }

    private static void requireServer(Entity entity) {
        if (entity.level().isClientSide()) {
            throw new IllegalStateException("EntityData can only be written on the server");
        }
    }

    private static EntityDataSavedData saved(Entity entity) {
        return EntityDataSavedData.get(entity.getServer());
    }

    private static com.dougg865.bloomberto.impl.data.EntityDataStore store(Entity entity) {
        return saved(entity).store();
    }
}
