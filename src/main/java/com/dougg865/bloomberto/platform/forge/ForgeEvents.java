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
    /** Levels already passed to {@code LEVEL_LOAD}, so the fallback never announces one twice. */
    private static final java.util.Set<ServerLevel> ANNOUNCED =
            java.util.Collections.synchronizedSet(java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>()));

    private static volatile net.minecraft.server.MinecraftServer checkedServer;

    private ForgeEvents() {
    }

    private static void announce(ServerLevel level) {
        if (ANNOUNCED.add(level)) {
            BloombertoEvents.LEVEL_LOAD.invoke(l -> l.accept(level));
        }
    }

    static void register() {
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.RegisterCommandsEvent e) ->
                com.dougg865.bloomberto.impl.command.BloombertoCommand.register(e.getDispatcher()));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent e) -> {
            if (e.phase == TickEvent.Phase.END) {
                net.minecraft.server.MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                if (server != null && server != checkedServer) {
                    checkedServer = server;
                    server.getAllLevels().forEach(ForgeEvents::announce);
                }
                BloombertoEvents.SERVER_TICK.invoke(l -> l.accept(server));
            }
        });
        MinecraftForge.EVENT_BUS.addListener((LevelEvent.Load e) -> {
            if (e.getLevel() instanceof ServerLevel level) {
                announce(level);
            }
        });
        // In a Forge 1.21.1 singleplayer game run by our test launcher, neither LevelEvent.Load for the integrated server's
        // levels nor the server lifecycle events arrived, while tick and player events did. Announce any level that was
        // missed on the first tick of each server, so LEVEL_LOAD always fires once per level before tick-based logic runs.
        MinecraftForge.EVENT_BUS.addListener((LevelEvent.Unload e) -> {
            if (e.getLevel() instanceof ServerLevel level) {
                ANNOUNCED.remove(level);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                BloombertoEvents.PLAYER_JOIN.invoke(l -> l.accept(player));
            }
        });
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.server.ServerStoppedEvent e) ->
                BloombertoEvents.SERVER_STOPPED.invoke(l -> l.accept(e.getServer())));
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.entity.EntityLeaveLevelEvent e) ->
                com.dougg865.bloomberto.impl.data.EntityDataCleanup.onLeave(e.getEntity()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                BloombertoEvents.PLAYER_LEAVE.invoke(l -> l.accept(player));
            }
        });
    }
}
