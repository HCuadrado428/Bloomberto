package com.dougg865.bloomberto.platform.fabric;

import com.dougg865.bloomberto.Bloomberto;
import com.dougg865.bloomberto.api.event.BloombertoEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public final class BloombertoFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Bloomberto.init();
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                com.dougg865.bloomberto.impl.command.BloombertoCommand.register(dispatcher));
        ServerTickEvents.END_SERVER_TICK.register(server ->
                BloombertoEvents.SERVER_TICK.invoke(l -> l.accept(server)));
        ServerWorldEvents.LOAD.register((server, level) ->
                BloombertoEvents.LEVEL_LOAD.invoke(l -> l.accept(level)));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                BloombertoEvents.PLAYER_JOIN.invoke(l -> l.accept(handler.player)));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                BloombertoEvents.PLAYER_LEAVE.invoke(l -> l.accept(handler.player)));
    }
}
