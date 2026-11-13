package com.dougg865.bloomberto.impl.config;

import com.dougg865.bloomberto.Bloomberto;
import com.dougg865.bloomberto.api.config.Config;
import com.dougg865.bloomberto.api.config.Configs;
import com.dougg865.bloomberto.api.event.BloombertoEvents;
import com.dougg865.bloomberto.api.network.Network;
import com.dougg865.bloomberto.api.network.PacketCodec;
import com.dougg865.bloomberto.api.network.PacketType;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Sends the options marked {@code synced()} from the server to each client that joins. */
public final class ConfigSync {
    /** The values of one config. */
    public record Packet(String modId, Map<String, String> values) {
        public static final PacketType<Packet> TYPE = PacketType.of(Bloomberto.id("config_sync"), Packet.class,
                PacketCodec.of(Packet::write, Packet::read));

        private static void write(Packet packet, net.minecraft.network.FriendlyByteBuf buf) {
            buf.writeUtf(packet.modId());
            buf.writeVarInt(packet.values().size());
            packet.values().forEach((key, value) -> buf.writeUtf(key).writeUtf(value));
        }

        private static Packet read(net.minecraft.network.FriendlyByteBuf buf) {
            String modId = buf.readUtf();
            int size = buf.readVarInt();
            Map<String, String> values = new LinkedHashMap<>();
            for (int i = 0; i < size; i++) {
                values.put(buf.readUtf(), buf.readUtf());
            }
            return new Packet(modId, values);
        }
    }

    private ConfigSync() {
    }

    public static void register() {
        Network.registerS2C(Packet.TYPE, (packet, ctx) -> {
            Config config = Configs.get(packet.modId());
            if (config != null) {
                config.applyServerValues(packet.values());
            }
        });
        BloombertoEvents.PLAYER_JOIN.register(ConfigSync::sendTo);
    }

    /** Sends every registered config's synced values to one player. */
    public static void sendTo(ServerPlayer player) {
        for (Config config : Configs.all()) {
            Map<String, String> values = config.syncedValues();
            if (!values.isEmpty()) {
                Network.sendToPlayer(player, new Packet(config.modId(), values));
            }
        }
    }

    /** Re-sends the synced values to every connected player, e.g. after the server reloaded its config. */
    public static void sendToAll(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(ConfigSync::sendTo);
    }

    /** Forgets server-provided values; called when the client leaves a server. */
    public static void clearServerValues() {
        Configs.all().forEach(Config::clearServerValues);
    }
}
