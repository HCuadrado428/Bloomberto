package com.dougg865.bloomberto.platform.forge;

import com.dougg865.bloomberto.api.event.BloombertoEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;

/** Common (client and server) Forge event hooks; Forge 1.21.11 (EventBus 7: one bus per event type). */
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
        net.minecraftforge.event.RegisterCommandsEvent.BUS.addListener(e ->
                com.dougg865.bloomberto.impl.command.BloombertoCommand.register(e.getDispatcher()));
        TickEvent.ServerTickEvent.Post.BUS.addListener(e -> {
            net.minecraft.server.MinecraftServer server = e.server();
            if (server != null && server != checkedServer) {
                checkedServer = server;
                server.getAllLevels().forEach(ForgeEvents::announce);
            }
            BloombertoEvents.SERVER_TICK.invoke(l -> l.accept(server));
        });
        LevelEvent.Load.BUS.addListener(e -> {
            if (e.getLevel() instanceof ServerLevel level) {
                announce(level);
            }
        });
        LevelEvent.Unload.BUS.addListener(e -> {
            if (e.getLevel() instanceof ServerLevel level) {
                ANNOUNCED.remove(level);
            }
        });
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(e -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                BloombertoEvents.PLAYER_JOIN.invoke(l -> l.accept(player));
            }
        });
        net.minecraftforge.event.server.ServerStoppedEvent.BUS.addListener(e ->
                BloombertoEvents.SERVER_STOPPED.invoke(l -> l.accept(e.getServer())));
        net.minecraftforge.event.entity.EntityLeaveLevelEvent.BUS.addListener(e ->
                com.dougg865.bloomberto.impl.data.EntityDataCleanup.onLeave(e.getEntity()));
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(e -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                BloombertoEvents.PLAYER_LEAVE.invoke(l -> l.accept(player));
            }
        });
    }
}
