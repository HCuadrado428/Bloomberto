package com.dougg865.bloomberto.api.network;

import net.minecraft.world.entity.player.Player;

/** Passed to packet handlers. Handlers always run on the game thread (client or server main thread). */
public interface PacketContext {
    /** The receiving player: the local player for server-to-client packets, the sender for client-to-server ones. */
    Player player();

    /** {@code true} when the packet arrived on the client. */
    boolean isClientSide();
}
