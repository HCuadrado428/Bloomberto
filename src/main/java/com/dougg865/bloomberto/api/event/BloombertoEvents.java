package com.dougg865.bloomberto.api.event;

import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Events available on every loader and Minecraft version. These are safe to reference on a
 * dedicated server; client-only events live in {@code ClientEvents}.
 */
public final class BloombertoEvents {
    /** End of every server tick. */
    public static final Event<Consumer<MinecraftServer>> SERVER_TICK = new Event<>("server_tick");
    /** A server level finished loading. */
    public static final Event<Consumer<ServerLevel>> LEVEL_LOAD = new Event<>("level_load");
    /** A player joined the server. */
    public static final Event<Consumer<ServerPlayer>> PLAYER_JOIN = new Event<>("player_join");
    /** A player left the server. */
    public static final Event<Consumer<ServerPlayer>> PLAYER_LEAVE = new Event<>("player_leave");

    private BloombertoEvents() {
    }
}
