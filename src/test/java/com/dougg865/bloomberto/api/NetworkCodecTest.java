package com.dougg865.bloomberto.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dougg865.bloomberto.api.network.PacketCodec;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

class NetworkCodecTest {
    private record Sample(String name, int count, boolean flag) { }

    private static final PacketCodec<Sample> CODEC = PacketCodec.of(
            (p, buf) -> buf.writeUtf(p.name()).writeVarInt(p.count()).writeBoolean(p.flag()),
            buf -> new Sample(buf.readUtf(), buf.readVarInt(), buf.readBoolean()));

    @Test
    void roundTrip() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        Sample original = new Sample("bloomberto", 300, true);
        CODEC.encode(original, buf);
        assertEquals(original, CODEC.decode(buf));
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void decodesFromWrappedBytes() {
        FriendlyByteBuf out = new FriendlyByteBuf(Unpooled.buffer());
        CODEC.encode(new Sample("x", 1, false), out);
        byte[] bytes = new byte[out.readableBytes()];
        out.readBytes(bytes);
        assertEquals(new Sample("x", 1, false), CODEC.decode(new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes))));
    }
}
