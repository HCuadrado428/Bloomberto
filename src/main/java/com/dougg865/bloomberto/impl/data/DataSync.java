package com.dougg865.bloomberto.impl.data;

import com.dougg865.bloomberto.Bloomberto;
import com.dougg865.bloomberto.api.data.DataKey;
import com.dougg865.bloomberto.api.event.BloombertoEvents;
import com.dougg865.bloomberto.api.network.Network;
import com.dougg865.bloomberto.api.network.PacketCodec;
import com.dougg865.bloomberto.api.network.PacketType;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Sends synced entity data to the player it belongs to. */
public final class DataSync {
    private static final String VALUE = "v";

    /** One value; {@code value == null} means the value was removed. */
    public record Packet(UUID entity, ResourceLocation key, Tag value) {
        public static final PacketType<Packet> TYPE = PacketType.of(Bloomberto.id("data_sync"), Packet.class,
                PacketCodec.of(Packet::write, Packet::read));

        private static void write(Packet packet, FriendlyByteBuf buf) {
            buf.writeUUID(packet.entity());
            buf.writeResourceLocation(packet.key());
            buf.writeBoolean(packet.value() != null);
            if (packet.value() != null) {
                CompoundTag wrapper = new CompoundTag();
                wrapper.put(VALUE, packet.value());
                buf.writeNbt(wrapper);
            }
        }

        private static Packet read(FriendlyByteBuf buf) {
            UUID entity = buf.readUUID();
            ResourceLocation key = buf.readResourceLocation();
            Tag value = null;
            if (buf.readBoolean()) {
                CompoundTag wrapper = buf.readNbt();
                value = wrapper == null ? null : wrapper.get(VALUE);
            }
            return new Packet(entity, key, value);
        }
    }

    private DataSync() {
    }

    public static void register() {
        Network.registerS2C(Packet.TYPE, (packet, ctx) -> {
            if (packet.value() == null) {
                ClientEntityData.remove(packet.entity(), packet.key().toString());
            } else {
                ClientEntityData.set(packet.entity(), packet.key().toString(), packet.value());
            }
        });
        BloombertoEvents.PLAYER_JOIN.register(DataSync::sendAll);
    }

    /** Sends every synced value stored for this player (called when they join). */
    public static void sendAll(ServerPlayer player) {
        EntityDataStore store = EntityDataSavedData.get(player.getServer()).store();
        for (Map.Entry<String, Tag> entry : store.entries(player.getUUID()).entrySet()) {
            ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
            DataKey<?> key = id == null ? null : DataKey.byId(id);
            if (key != null && key.isSynced()) {
                Network.sendToPlayer(player, new Packet(player.getUUID(), id, entry.getValue()));
            }
        }
    }
}
