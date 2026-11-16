package com.dougg865.bloomberto.impl.network;

import com.dougg865.bloomberto.Bloomberto;
import com.dougg865.bloomberto.api.BloombertoApi;
import com.dougg865.bloomberto.api.event.BloombertoEvents;
import com.dougg865.bloomberto.api.network.Network;
import com.dougg865.bloomberto.api.network.PacketCodec;
import com.dougg865.bloomberto.api.network.PacketType;

/**
 * Tells each joining client which Bloomberto API level the server runs. When client and server differ, the client logs a
 * warning and shows it in chat, so a "works in singleplayer, breaks on the server" problem points at the right cause.
 * Nothing is refused: older and newer levels may still be compatible for what the installed mods use.
 */
public final class VersionCheck {
    public record Packet(int apiLevel) {
        public static final PacketType<Packet> TYPE = PacketType.of(Bloomberto.id("version"), Packet.class,
                PacketCodec.of((p, buf) -> buf.writeVarInt(p.apiLevel()), buf -> new Packet(buf.readVarInt())));
    }

    private VersionCheck() {
    }

    public static void register() {
        Network.registerS2C(Packet.TYPE, (packet, ctx) -> {
            String message = message(BloombertoApi.level(), packet.apiLevel());
            if (message != null) {
                Bloomberto.LOGGER.warn(message);
                com.dougg865.bloomberto.impl.client.ClientMessages.chat(message);
            }
        });
        BloombertoEvents.PLAYER_JOIN.register(player -> Network.sendToPlayer(player, new Packet(BloombertoApi.level())));
    }

    /** The warning to show, or {@code null} when both sides have the same level. */
    public static String message(int clientLevel, int serverLevel) {
        if (clientLevel == serverLevel) {
            return null;
        }
        return "Bloomberto: this server uses API level " + serverLevel + " and your game has level " + clientLevel
                + (clientLevel < serverLevel ? "; update Bloomberto if something does not work." : "; the server's Bloomberto is older.");
    }
}
