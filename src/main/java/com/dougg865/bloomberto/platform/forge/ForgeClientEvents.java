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
        //? if >=1.21 {
        FMLJavaModLoadingContext.get().getModEventBus().addListener((AddGuiOverlayLayersEvent e) ->
                e.getLayeredDraw().add(Bloomberto.id("hud"), (graphics, delta) ->
                        ClientEvents.HUD_RENDER.invoke(l -> l.render(graphics, delta.getGameTimeDeltaPartialTick(false)))));
        //?} else {
        /*MinecraftForge.EVENT_BUS.addListener((RenderGuiEvent.Post e) ->
                ClientEvents.HUD_RENDER.invoke(l -> l.render(e.getGuiGraphics(), e.getPartialTick())));
        *///?}
    }
}
