package com.dougg865.bloomberto.impl.data;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Deletes the stored data of an entity that was destroyed (killed or discarded), so data of dead mobs does not pile up in
 * the world forever. Entities that are only unloaded with their chunk, or that change dimension, keep their data; players
 * always keep theirs.
 */
public final class EntityDataCleanup {
    private EntityDataCleanup() {
    }

    /** Called by each loader when an entity leaves a server level. */
    public static void onLeave(Entity entity) {
        if (entity instanceof Player || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        Entity.RemovalReason reason = entity.getRemovalReason();
        if (reason == null || !reason.shouldDestroy()) {
            return;
        }
        EntityDataSavedData data = EntityDataSavedData.get(level.getServer());
        if (data.store().clear(entity.getUUID())) {
            data.setDirty();
        }
    }
}
