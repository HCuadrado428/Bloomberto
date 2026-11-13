package com.dougg865.bloomberto.api.network;

import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.network.FriendlyByteBuf;

/** Writes and reads a packet to/from a buffer. The same codec is used on every version and loader. */
public interface PacketCodec<T> {
    void encode(T packet, FriendlyByteBuf buf);

    T decode(FriendlyByteBuf buf);

    /** Builds a codec from an encoder and a decoder. */
    static <T> PacketCodec<T> of(BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder) {
        return new PacketCodec<>() {
            @Override
            public void encode(T packet, FriendlyByteBuf buf) {
                encoder.accept(packet, buf);
            }

            @Override
            public T decode(FriendlyByteBuf buf) {
                return decoder.apply(buf);
            }
        };
    }
}
