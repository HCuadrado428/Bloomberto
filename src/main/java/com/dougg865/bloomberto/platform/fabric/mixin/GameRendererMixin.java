package com.dougg865.bloomberto.platform.fabric.mixin;

import com.dougg865.bloomberto.api.render.CameraZoom;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric has no field-of-view event, so the zoom of {@link CameraZoom} is applied here. Only to the world's field of
 * view ({@code useFovSetting}): the first-person hand is drawn with its own fixed one and must not be magnified.
 */
@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
    /** With a shader pack, post effects run on the pack's finished image right before the HUD (see PostEffects). */
    //? if >=1.21 {
    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"))
    private void bloomberto$effectsBeforeHud(net.minecraft.client.DeltaTracker delta, boolean renderLevel, CallbackInfo ci) {
        com.dougg865.bloomberto.api.render.post.PostEffects.renderOverHud(delta.getGameTimeDeltaPartialTick(false));
    }
    //?} else {
    /*@Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;render(Lnet/minecraft/client/gui/GuiGraphics;F)V"))
    private void bloomberto$effectsBeforeHud(float partialTick, long nanoTime, boolean renderLevel, CallbackInfo ci) {
        com.dougg865.bloomberto.api.render.post.PostEffects.renderOverHud(partialTick);
    }
    *///?}

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void bloomberto$zoom(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Double> cir) {
        if (useFovSetting && CameraZoom.shared().isZoomed()) {
            cir.setReturnValue(CameraZoom.shared().apply(cir.getReturnValueD(), partialTick));
        }
    }
}
