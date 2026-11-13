package com.dougg865.bloomberto.platform.forge;

import com.dougg865.bloomberto.Bloomberto;
import com.dougg865.bloomberto.api.client.ClientEvents;
//? if >=1.21 {
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
//?} else {
/*import net.minecraftforge.client.event.RenderGuiEvent;
*///?}
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

/** Client-only Forge event hooks. Only loaded when running on the physical client. */
final class ForgeClientEvents {
    private ForgeClientEvents() {
    }

    static void register() {
        Bloomberto.initClient();
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e) -> {
            if (e.phase == TickEvent.Phase.END) {
                ClientEvents.CLIENT_TICK.invoke(Runnable::run);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) ->
                ClientEvents.DISCONNECT.invoke(Runnable::run));
        MinecraftForge.EVENT_BUS.addListener((RenderLevelStageEvent e) -> {
            if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
                float partial = e.getPartialTick();
                net.minecraft.client.renderer.MultiBufferSource.BufferSource buffers =
                        net.minecraft.client.Minecraft.getInstance().renderBuffers().bufferSource();
                //? if >=1.21 {
                // Forge 1.21 hands out the view matrix instead of a pose stack.
                com.mojang.blaze3d.vertex.PoseStack poseStack = new com.mojang.blaze3d.vertex.PoseStack();
                poseStack.mulPose(e.getPoseStack());
                //?} else {
                /*com.mojang.blaze3d.vertex.PoseStack poseStack = e.getPoseStack();
                *///?}
                ClientEvents.WORLD_RENDER.invoke(l -> l.render(new com.dougg865.bloomberto.api.client.WorldRenderContext(
                        poseStack, e.getCamera(), partial, buffers)));
                buffers.endBatch();
            }
        });
        //? if >=1.21 {
        FMLJavaModLoadingContext.get().getModEventBus().addListener((AddGuiOverlayLayersEvent e) ->
                e.getLayeredDraw().add(Bloomberto.id("hud"), (graphics, delta) ->
                        ClientEvents.HUD_RENDER.invoke(l -> l.render(graphics, delta.getGameTimeDeltaPartialTick(false)))));
        //?} else {
        /*MinecraftForge.EVENT_BUS.addListener((RenderGuiEvent.Post e) ->
                ClientEvents.HUD_RENDER.invoke(l -> l.render(e.getGuiGraphics(), e.getPartialTick())));
        *///?}
    }

    /** Adds the "config" button of the mod list for the mod that is currently loading. */
    static void registerConfigScreen(com.dougg865.bloomberto.api.config.Config config) {
        net.minecraftforge.fml.ModLoadingContext.get().registerExtensionPoint(
                net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> com.dougg865.bloomberto.api.client.ConfigScreen.create(parent, config)));
    }
}
