package com.dougg865.bloomberto.api.network;

import net.minecraft.resources.ResourceLocation;

/**
 * Identifies a packet: its channel id, the Java class and how to serialize it.
 *
 * <pre>{@code
 * record PingPacket(String text) {
 *     static final PacketType<PingPacket> TYPE = PacketType.of(Bloomberto.id("mymod", "ping"), PingPacket.class,
 *         PacketCodec.of((p, buf) -> buf.writeUtf(p.text()), buf -> new PingPacket(buf.readUtf())));
 * }
 * }</pre>
 */
public record PacketType<T>(ResourceLocation id, Class<T> type, PacketCodec<T> codec) {
    public static <T> PacketType<T> of(ResourceLocation id, Class<T> type, PacketCodec<T> codec) {
        return new PacketType<>(id, type, codec);
    }
}
