package com.dougg865.bloomberto.platform.neoforge;

import com.dougg865.bloomberto.Bloomberto;
import com.dougg865.bloomberto.api.client.ClientEvents;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client-only NeoForge event hooks. Only loaded when running on the physical client. */
final class NeoForgeClientEvents {
    private NeoForgeClientEvents() {
    }

    static void register() {
        Bloomberto.initClient();
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) ->
                ClientEvents.CLIENT_TICK.invoke(Runnable::run));
        NeoForge.EVENT_BUS.addListener((RenderGuiEvent.Post e) ->
                ClientEvents.HUD_RENDER.invoke(l ->
                        l.render(e.getGuiGraphics(), e.getPartialTick().getGameTimeDeltaPartialTick(false))));
    }
}
