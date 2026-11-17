package com.dougg865.bloomberto.platform.fabric.mixin;

import com.dougg865.bloomberto.impl.data.EntityDataCleanup;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric's entity unload event only fires for entities whose chunk is tracked; an entity destroyed anywhere else (e.g. in
 * a chunk that is still loading) would keep its stored data forever. Every removal goes through {@code setRemoved}.
 */
@Mixin(Entity.class)
abstract class EntityMixin {
    @Inject(method = "setRemoved", at = @At("TAIL"))
    private void bloomberto$cleanup(Entity.RemovalReason reason, CallbackInfo ci) {
        EntityDataCleanup.onLeave((Entity) (Object) this);
    }
}
