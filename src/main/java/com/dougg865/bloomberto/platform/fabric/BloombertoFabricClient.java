package com.dougg865.bloomberto.platform.fabric;

import com.dougg865.bloomberto.Bloomberto;
import com.dougg865.bloomberto.api.client.ClientEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public final class BloombertoFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Bloomberto.initClient();
        ClientTickEvents.END_CLIENT_TICK.register(client ->
                ClientEvents.CLIENT_TICK.invoke(Runnable::run));
        HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
            //? if >=1.21 {
            float partial = tickDelta.getGameTimeDeltaPartialTick(false);
            //?} else {
            /*float partial = tickDelta;
            *///?}
            ClientEvents.HUD_RENDER.invoke(l -> l.render(graphics, partial));
        });
    }
}
