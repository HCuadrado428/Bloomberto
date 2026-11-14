package com.dougg865.bloomberto.platform.fabric.mixin;

import com.dougg865.bloomberto.api.render.CameraZoom;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric has no field-of-view event, so the zoom of {@link CameraZoom} is applied here. Only to the world's field of
 * view ({@code useFovSetting}): the first-person hand is drawn with its own fixed one and must not be magnified.
 */
@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void bloomberto$zoom(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Double> cir) {
        if (useFovSetting && CameraZoom.shared().isZoomed()) {
            cir.setReturnValue(CameraZoom.shared().apply(cir.getReturnValueD(), partialTick));
        }
    }
}
