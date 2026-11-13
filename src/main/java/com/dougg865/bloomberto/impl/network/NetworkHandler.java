package com.dougg865.bloomberto.impl.network;

import com.dougg865.bloomberto.api.network.PacketContext;
import com.dougg865.bloomberto.api.network.PacketType;
import java.util.function.BiConsumer;
import net.minecraft.server.level.ServerPlayer;

/** Loader-specific networking; implemented once per loader. Use {@code Network} instead of this. */
public interface NetworkHandler {
    <T> void registerS2C(PacketType<T> type, BiConsumer<T, PacketContext> handler);

    <T> void registerC2S(PacketType<T> type, BiConsumer<T, PacketContext> handler);

    <T> void sendToPlayer(ServerPlayer player, PacketType<T> type, T packet);

    /** Client only. */
    <T> void sendToServer(PacketType<T> type, T packet);
}
