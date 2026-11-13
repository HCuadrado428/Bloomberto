package com.dougg865.bloomberto.platform.neoforge;

import com.dougg865.bloomberto.api.event.BloombertoEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Common (client and server) NeoForge event hooks. */
final class NeoForgeEvents {
    private NeoForgeEvents() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent e) ->
                com.dougg865.bloomberto.impl.command.BloombertoCommand.register(e.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) ->
                BloombertoEvents.SERVER_TICK.invoke(l -> l.accept(e.getServer())));
        NeoForge.EVENT_BUS.addListener((LevelEvent.Load e) -> {
            if (e.getLevel() instanceof ServerLevel level) {
                BloombertoEvents.LEVEL_LOAD.invoke(l -> l.accept(level));
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                BloombertoEvents.PLAYER_JOIN.invoke(l -> l.accept(player));
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                BloombertoEvents.PLAYER_LEAVE.invoke(l -> l.accept(player));
            }
        });
    }
}
