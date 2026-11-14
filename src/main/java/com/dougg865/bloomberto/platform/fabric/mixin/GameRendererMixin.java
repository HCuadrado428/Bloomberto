package com.dougg865.bloomberto.platform.fabric.mixin;

import com.dougg865.bloomberto.api.render.CameraZoom;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fabric has no field-of-view event, so the zoom of {@link CameraZoom} is applied here. */
@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void bloomberto$zoom(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Double> cir) {
        if (CameraZoom.shared().isZoomed()) {
            cir.setReturnValue(CameraZoom.shared().apply(cir.getReturnValueD(), partialTick));
        }
    }
}
