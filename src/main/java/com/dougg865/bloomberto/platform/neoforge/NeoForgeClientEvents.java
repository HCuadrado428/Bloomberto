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

    static void register(net.neoforged.bus.api.IEventBus modEventBus) {
        Bloomberto.initClient();
        modEventBus.addListener((net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent e) ->
                com.dougg865.bloomberto.api.client.Keybinds.drainPending().forEach(e::register));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.ItemTooltipEvent e) ->
                com.dougg865.bloomberto.api.client.Tooltips.apply(e.getItemStack(), e.getToolTip(), e.getFlags().isAdvanced()));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) ->
                ClientEvents.CLIENT_TICK.invoke(Runnable::run));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) ->
                ClientEvents.DISCONNECT.invoke(Runnable::run));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ViewportEvent.ComputeFov e) ->
                e.setFOV(com.dougg865.bloomberto.api.render.CameraZoom.shared().apply(e.getFOV(), (float) e.getPartialTick())));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles e) -> {
            float[] shake = com.dougg865.bloomberto.api.render.CameraShake.shared().offset((float) e.getPartialTick());
            e.setYaw(e.getYaw() + shake[0]);
            e.setPitch(e.getPitch() + shake[1]);
        });
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent e) -> {
            if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
                com.dougg865.bloomberto.impl.render.WorldRenderBridge.fire(new com.mojang.blaze3d.vertex.PoseStack(), e.getCamera(),
                        e.getPartialTick().getGameTimeDeltaPartialTick(false), e.getModelViewMatrix());
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
