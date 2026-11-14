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
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.entity.player.ItemTooltipEvent e) ->
                com.dougg865.bloomberto.api.client.Tooltips.apply(e.getItemStack(), e.getToolTip(), e.getFlags().isAdvanced()));
        net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus().addListener(
                (net.minecraftforge.client.event.RegisterKeyMappingsEvent e) ->
                        com.dougg865.bloomberto.api.client.Keybinds.drainPending().forEach(e::register));
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) ->
                ClientEvents.DISCONNECT.invoke(Runnable::run));
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles e) -> {
            float[] shake = com.dougg865.bloomberto.api.render.CameraShake.shared().offset((float) e.getPartialTick());
            e.setYaw(e.getYaw() + shake[0]);
            e.setPitch(e.getPitch() + shake[1]);
        });
        MinecraftForge.EVENT_BUS.addListener((RenderLevelStageEvent e) -> {
            if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
                //? if >=1.21 {
                // The matrix Forge 1.21 provides drew nothing in-game; null makes the bridge derive the view from the camera.
                com.dougg865.bloomberto.impl.render.WorldRenderBridge.fire(new com.mojang.blaze3d.vertex.PoseStack(), e.getCamera(),
                        e.getPartialTick(), null);
                //?} else {
                /*com.dougg865.bloomberto.impl.render.WorldRenderBridge.fire(e.getPoseStack(), e.getCamera(), e.getPartialTick(), null);
                *///?}
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
