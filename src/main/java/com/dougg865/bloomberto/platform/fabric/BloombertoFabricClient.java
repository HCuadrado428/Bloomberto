package com.dougg865.bloomberto.platform.fabric;

import com.dougg865.bloomberto.Bloomberto;
import com.dougg865.bloomberto.api.client.ClientEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public final class BloombertoFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Bloomberto.initClient();
        ClientTickEvents.END_CLIENT_TICK.register(client ->
                ClientEvents.CLIENT_TICK.invoke(Runnable::run));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                ClientEvents.DISCONNECT.invoke(Runnable::run));
        WorldRenderEvents.END.register(context -> {
            //? if >=1.21 {
            float partial = context.tickCounter().getGameTimeDeltaPartialTick(false);
            //?} else {
            /*float partial = context.tickDelta();
            *///?}
            ClientEvents.WORLD_RENDER.invoke(l -> l.render(new com.dougg865.bloomberto.api.client.WorldRenderContext(
                    context.matrixStack(), context.camera(), partial,
                    net.minecraft.client.Minecraft.getInstance().renderBuffers().bufferSource())));
            net.minecraft.client.Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
        });
        //? if >=1.21 {
        net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack, context, flag, lines) ->
                com.dougg865.bloomberto.api.client.Tooltips.apply(stack, lines, flag.isAdvanced()));
        //?} else {
        /*net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack, flag, lines) ->
                com.dougg865.bloomberto.api.client.Tooltips.apply(stack, lines, flag.isAdvanced()));
        *///?}
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
