package com.dougg865.bloomberto.impl.network;

//? if >=1.21 {
import com.dougg865.bloomberto.api.network.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Adapts any Bloomberto packet to a vanilla 1.21 custom payload. */
public final class BloombertoPayload<T> implements CustomPacketPayload {
    private final PacketType<T> packetType;
    private final CustomPacketPayload.Type<BloombertoPayload<T>> payloadType;
    private final T packet;

    public BloombertoPayload(PacketType<T> packetType, CustomPacketPayload.Type<BloombertoPayload<T>> payloadType, T packet) {
        this.packetType = packetType;
        this.payloadType = payloadType;
        this.packet = packet;
    }

    public T packet() {
        return packet;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return payloadType;
    }

    /** The payload type (channel id) for a packet type. */
    public static <T> CustomPacketPayload.Type<BloombertoPayload<T>> typeFor(PacketType<T> packetType) {
        return new CustomPacketPayload.Type<>(packetType.id());
    }

    public static <T> StreamCodec<FriendlyByteBuf, BloombertoPayload<T>> codecFor(
            PacketType<T> packetType, CustomPacketPayload.Type<BloombertoPayload<T>> payloadType) {
        return StreamCodec.of(
                (buf, payload) -> packetType.codec().encode(payload.packet, buf),
                buf -> new BloombertoPayload<>(packetType, payloadType, packetType.codec().decode(buf)));
    }
}
//?}
