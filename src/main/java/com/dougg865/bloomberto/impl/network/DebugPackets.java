package com.dougg865.bloomberto.impl.network;

import com.dougg865.bloomberto.Bloomberto;
import com.dougg865.bloomberto.api.event.BloombertoEvents;
import com.dougg865.bloomberto.api.network.Network;
import com.dougg865.bloomberto.api.network.PacketCodec;
import com.dougg865.bloomberto.api.network.PacketType;

/** Round-trip packets used to check networking on a real connection; only active with {@code -Dbloomberto.debugEvents=true}. */
public final class DebugPackets {
    public record Ping(String text) {
        public static final PacketType<Ping> S2C = PacketType.of(Bloomberto.id("debug_ping_s2c"), Ping.class,
                PacketCodec.of((p, buf) -> buf.writeUtf(p.text()), buf -> new Ping(buf.readUtf())));
    }

    public record Pong(String text) {
        public static final PacketType<Pong> C2S = PacketType.of(Bloomberto.id("debug_pong_c2s"), Pong.class,
                PacketCodec.of((p, buf) -> buf.writeUtf(p.text()), buf -> new Pong(buf.readUtf())));
    }

    private DebugPackets() {
    }

    public static void register() {
        Network.registerS2C(Ping.S2C, (packet, ctx) -> {
            Bloomberto.LOGGER.info("[debugEvents] client received ping '{}'", packet.text());
            Network.sendToServer(new Pong("pong:" + packet.text()));
        });
        Network.registerC2S(Pong.C2S, (packet, ctx) ->
                Bloomberto.LOGGER.info("[debugEvents] server received '{}' from {}", packet.text(),
                        ctx.player().getGameProfile().getName()));
        BloombertoEvents.PLAYER_JOIN.register(player -> Network.sendToPlayer(player, new Ping("hello " + player.getGameProfile().getName())));
        Bloomberto.LOGGER.info("[debugEvents] network packets registered");
    }
}
