package com.dougg865.bloomberto.platform.forge;

import com.dougg865.bloomberto.api.event.BloombertoEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

/** Common (client and server) Forge event hooks. */
final class ForgeEvents {
    private ForgeEvents() {
    }

    static void register() {
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.RegisterCommandsEvent e) ->
                com.dougg865.bloomberto.impl.command.BloombertoCommand.register(e.getDispatcher()));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent e) -> {
            if (e.phase == TickEvent.Phase.END) {
                BloombertoEvents.SERVER_TICK.invoke(l -> l.accept(ServerLifecycleHooks.getCurrentServer()));
            }
        });
        MinecraftForge.EVENT_BUS.addListener((LevelEvent.Load e) -> {
            if (e.getLevel() instanceof ServerLevel level) {
                BloombertoEvents.LEVEL_LOAD.invoke(l -> l.accept(level));
            }
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                BloombertoEvents.PLAYER_JOIN.invoke(l -> l.accept(player));
            }
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                BloombertoEvents.PLAYER_LEAVE.invoke(l -> l.accept(player));
            }
        });
    }
}
