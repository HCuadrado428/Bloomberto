package com.dougg865.bloomberto.platform.fabric.mixin;

import com.dougg865.bloomberto.api.render.CameraShake;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fabric has no camera-angle event, so the shake of {@link CameraShake} is applied here. */
@Mixin(Camera.class)
abstract class CameraMixin {
    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Shadow
    public abstract float getYRot();

    @Shadow
    public abstract float getXRot();

    @Inject(method = "setup", at = @At("TAIL"))
    private void bloomberto$shake(BlockGetter level, Entity entity, boolean detached, boolean mirrored, float partialTick,
                                  CallbackInfo ci) {
        float[] offset = CameraShake.shared().offset(partialTick);
        if (offset[0] != 0f || offset[1] != 0f) {
            setRotation(getYRot() + offset[0], getXRot() + offset[1]);
        }
    }
}
