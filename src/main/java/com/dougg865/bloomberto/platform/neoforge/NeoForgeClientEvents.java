package com.dougg865.bloomberto.platform.neoforge;

import com.dougg865.bloomberto.Bloomberto;
import com.dougg865.bloomberto.api.client.ClientEvents;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client-only NeoForge event hooks. Only loaded when running on the physical client. */
final class NeoForgeClientEvents {
    private NeoForgeClientEvents() {
    }

    static void register() {
        Bloomberto.initClient();
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) ->
                ClientEvents.CLIENT_TICK.invoke(Runnable::run));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) ->
                ClientEvents.DISCONNECT.invoke(Runnable::run));
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent e) -> {
            if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
                net.minecraft.client.renderer.MultiBufferSource.BufferSource buffers =
                        net.minecraft.client.Minecraft.getInstance().renderBuffers().bufferSource();
                com.mojang.blaze3d.vertex.PoseStack poseStack = new com.mojang.blaze3d.vertex.PoseStack();
                poseStack.mulPose(e.getModelViewMatrix());
                ClientEvents.WORLD_RENDER.invoke(l -> l.render(new com.dougg865.bloomberto.api.client.WorldRenderContext(
                        poseStack, e.getCamera(), e.getPartialTick().getGameTimeDeltaPartialTick(false), buffers)));
                buffers.endBatch();
            }
        });
        NeoForge.EVENT_BUS.addListener((RenderGuiEvent.Post e) ->
                ClientEvents.HUD_RENDER.invoke(l ->
                        l.render(e.getGuiGraphics(), e.getPartialTick().getGameTimeDeltaPartialTick(false))));
    }

    /** Adds the "config" button of the mod list for the mod that is currently loading. */
    static void registerConfigScreen(com.dougg865.bloomberto.api.config.Config config) {
        net.neoforged.fml.ModLoadingContext.get().getActiveContainer().registerExtensionPoint(
                net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                (container, parent) -> com.dougg865.bloomberto.api.client.ConfigScreen.create(parent, config));
    }
}
