package com.dougg865.bloomberto.api.network;

import com.dougg865.bloomberto.platform.Services;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * One networking API for every loader and Minecraft version. It hides the difference between
 * {@code FriendlyByteBuf} channels (1.20.1) and custom payloads with stream codecs (1.21.1).
 *
 * <p>Register packets while your mod initializes, on both client and server (the same code), before
 * any player can connect. Handlers run on the game thread.
 */
public final class Network {
    private static final Map<Class<?>, PacketType<?>> TYPES = new ConcurrentHashMap<>();

    private Network() {
    }

    /** Registers a server-to-client packet. Call on both sides; the handler only runs on the client. */
    public static <T> void registerS2C(PacketType<T> type, BiConsumer<T, PacketContext> handler) {
        remember(type);
        Services.NETWORK.registerS2C(type, handler);
    }

    /** Registers a client-to-server packet. Call on both sides; the handler only runs on the server. */
    public static <T> void registerC2S(PacketType<T> type, BiConsumer<T, PacketContext> handler) {
        remember(type);
        Services.NETWORK.registerC2S(type, handler);
    }

    public static <T> void sendToPlayer(ServerPlayer player, T packet) {
        Services.NETWORK.sendToPlayer(player, typeOf(packet), packet);
    }

    public static <T> void sendToAll(MinecraftServer server, T packet) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendToPlayer(player, packet);
        }
    }

    /** Sends to every player in {@code level} within {@code radius} blocks of {@code center}. */
    public static <T> void sendToNear(ServerLevel level, BlockPos center, double radius, T packet) {
        double max = radius * radius;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5) <= max) {
                sendToPlayer(player, packet);
            }
        }
    }

    /** Sends to the server. Client only. */
    public static <T> void sendToServer(T packet) {
        Services.NETWORK.sendToServer(typeOf(packet), packet);
    }

    private static void remember(PacketType<?> type) {
        PacketType<?> previous = TYPES.putIfAbsent(type.type(), type);
        if (previous != null && previous != type) {
            throw new IllegalStateException("Packet class " + type.type().getName() + " is already registered as "
                    + previous.id());
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> PacketType<T> typeOf(T packet) {
        PacketType<?> type = TYPES.get(packet.getClass());
        if (type == null) {
            throw new IllegalArgumentException("Packet class " + packet.getClass().getName() + " is not registered");
        }
        return (PacketType<T>) type;
    }
}
