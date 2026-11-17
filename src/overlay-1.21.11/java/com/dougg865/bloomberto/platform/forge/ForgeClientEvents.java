package com.dougg865.bloomberto.platform.forge;

import com.dougg865.bloomberto.Bloomberto;
import com.dougg865.bloomberto.api.client.ClientEvents;
import com.mojang.blaze3d.framegraph.FramePass;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraftforge.client.FramePassManager;
import net.minecraftforge.client.event.AddFramePassEvent;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;

/** Client-only Forge event hooks; Forge 1.21.11. Only loaded when running on the physical client. */
final class ForgeClientEvents {
    private ForgeClientEvents() {
    }

    /** Runs Bloomberto's world rendering as a pass of the level's frame graph, after everything drawn into the main target. */
    private static final class WorldPass implements FramePassManager.PassDefinition {
        private float partialTick;

        @Override
        public void extracts(LevelTargetBundle bundle, FramePass pass, DeltaTracker deltaTracker) {
            partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
            bundle.main = pass.readsAndWrites(bundle.main);
        }

        @Override
        public void executes(LevelRenderState state) {
            Minecraft mc = Minecraft.getInstance();
            com.dougg865.bloomberto.impl.render.WorldRenderBridge.fire(new com.mojang.blaze3d.vertex.PoseStack(),
                    mc.gameRenderer.getMainCamera(), partialTick, null);
        }
    }

    static void register() {
        Bloomberto.initClient();
        net.minecraftforge.client.event.RegisterClientReloadListenersEvent.BUS.addListener(e -> {
            e.registerReloadListener(com.dougg865.bloomberto.impl.render.ClientReloads.listener());
            com.dougg865.bloomberto.impl.render.ClientReloads.markRegistered();
        });
        TickEvent.ClientTickEvent.Post.BUS.addListener(e -> ClientEvents.CLIENT_TICK.invoke(Runnable::run));
        net.minecraftforge.event.entity.player.ItemTooltipEvent.BUS.addListener(e ->
                com.dougg865.bloomberto.api.client.Tooltips.apply(e.getItemStack(), e.getToolTip(), e.getFlags().isAdvanced()));
        net.minecraftforge.client.event.RegisterKeyMappingsEvent.BUS.addListener(e ->
                com.dougg865.bloomberto.api.client.Keybinds.drainPending().forEach(e::register));
        ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(e -> ClientEvents.DISCONNECT.invoke(Runnable::run));
        net.minecraftforge.client.event.ViewportEvent.ComputeFov.BUS.addListener(e -> {
            // only the world's field of view: the first-person hand uses a fixed one and must not be magnified
            if (e.usedConfiguredFov()) {
                e.setFOV((float) com.dougg865.bloomberto.api.render.CameraZoom.shared().apply(e.getFOV(), e.getPartialTick()));
            }
        });
        net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles.BUS.addListener(e -> {
            float[] shake = com.dougg865.bloomberto.api.render.CameraShake.shared().offset(e.getPartialTick());
            e.setYaw(e.getYaw() + shake[0]);
            e.setPitch(e.getPitch() + shake[1]);
        });
        AddFramePassEvent.BUS.addListener(e -> e.addPass(Bloomberto.id("bloomberto", "world_render"), new WorldPass()));
        AddGuiOverlayLayersEvent.BUS.addListener(e ->
                e.getLayeredDraw().add(Bloomberto.id("hud"), (graphics, delta) ->
                        ClientEvents.HUD_RENDER.invoke(l -> l.render(graphics, delta.getGameTimeDeltaPartialTick(false)))));
    }

    /** Adds the "config" button of the mod list for the mod that is currently loading. */
    static void registerConfigScreen(com.dougg865.bloomberto.api.config.Config config) {
        net.minecraftforge.fml.ModLoadingContext.get().registerExtensionPoint(
                net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> com.dougg865.bloomberto.api.client.ConfigScreen.create(parent, config)));
    }
}
