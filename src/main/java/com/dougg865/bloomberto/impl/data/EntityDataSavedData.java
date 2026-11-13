package com.dougg865.bloomberto.impl.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
//? if >=1.21 {
import net.minecraft.core.HolderLookup;
//?}

/** Stores {@link EntityDataStore} in the overworld's data folder ({@code data/bloomberto_entity_data.dat}). */
public final class EntityDataSavedData extends SavedData {
    private static final String NAME = "bloomberto_entity_data";

    private final EntityDataStore store;

    private EntityDataSavedData(EntityDataStore store) {
        this.store = store;
    }

    public EntityDataStore store() {
        return store;
    }

    /** The store of this server's world (kept in the overworld, shared by all dimensions). */
    public static EntityDataSavedData get(MinecraftServer server) {
        //? if >=1.21 {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(() -> new EntityDataSavedData(new EntityDataStore()),
                        (tag, provider) -> new EntityDataSavedData(EntityDataStore.fromTag(tag)), null), NAME);
        //?} else {
        /*return server.overworld().getDataStorage().computeIfAbsent(
                tag -> new EntityDataSavedData(EntityDataStore.fromTag(tag)),
                () -> new EntityDataSavedData(new EntityDataStore()), NAME);
        *///?}
    }

    //? if >=1.21 {
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        return store.toTag();
    }
    //?} else {
    /*@Override
    public CompoundTag save(CompoundTag tag) {
        return store.toTag();
    }
    *///?}
}
