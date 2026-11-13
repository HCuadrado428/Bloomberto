package com.dougg865.bloomberto.platform.forge;

import com.dougg865.bloomberto.Bloomberto;
import com.dougg865.bloomberto.api.network.PacketContext;
import com.dougg865.bloomberto.api.network.PacketType;
import com.dougg865.bloomberto.impl.network.NetworkHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;
//? if >=1.21 {
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;
//?} else {
/*import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
*///?}

/**
 * Forge sends every Bloomberto packet through one channel inside an {@link Envelope} that carries the
 * packet id and its bytes, so other mods never have to register Forge channels themselves.
 */
public final class ForgeNetworkHandler implements NetworkHandler {
    private record Registration(PacketType<?> type, BiConsumer<Object, PacketContext> handler, boolean toClient) {
    }

    private static final Map<ResourceLocation, Registration> REGISTRATIONS = new ConcurrentHashMap<>();
    private static final SimpleChannel CHANNEL = createChannel();

    /** The wire format: packet id followed by the encoded packet. */
    record Envelope(ResourceLocation id, byte[] data) {
        void encode(FriendlyByteBuf buf) {
            buf.writeResourceLocation(id);
            buf.writeByteArray(data);
        }

        static Envelope decode(FriendlyByteBuf buf) {
            return new Envelope(buf.readResourceLocation(), buf.readByteArray());
        }
    }

    private static SimpleChannel createChannel() {
        //? if >=1.21 {
        SimpleChannel channel = ChannelBuilder.named(Bloomberto.id("main"))
                .networkProtocolVersion(1)
                .acceptedVersions((status, version) -> true)
                .simpleChannel();
        channel.messageBuilder(Envelope.class)
                .encoder(Envelope::encode)
                .decoder(Envelope::decode)
                .consumerMainThread((msg, ctx) -> dispatch(msg, ctx.isClientSide(), ctx.getSender()))
                .add();
        return channel;
        //?} else {
        /*SimpleChannel channel = NetworkRegistry.newSimpleChannel(Bloomberto.id("main"), () -> "1", v -> true, v -> true);
        channel.registerMessage(0, Envelope.class, Envelope::encode, Envelope::decode, (msg, supplier) -> {
            NetworkEvent.Context ctx = supplier.get();
            ctx.enqueueWork(() -> dispatch(msg, ctx.getDirection().getReceptionSide().isClient(), ctx.getSender()));
            ctx.setPacketHandled(true);
        });
        return channel;
        *///?}
    }

    private static void dispatch(Envelope envelope, boolean clientSide, ServerPlayer sender) {
        Registration registration = REGISTRATIONS.get(envelope.id());
        if (registration == null) {
            Bloomberto.LOGGER.warn("Received a packet with unknown id {}", envelope.id());
            return;
        }
        if (registration.toClient() != clientSide) {
            Bloomberto.LOGGER.warn("Packet {} arrived on the wrong side; ignoring", envelope.id());
            return;
        }
        Object packet = registration.type().codec().decode(new FriendlyByteBuf(Unpooled.wrappedBuffer(envelope.data())));
        Player player = clientSide ? ForgeClientNetwork.localPlayer() : sender;
        registration.handler().accept(packet, new Context(player, clientSide));
    }

    @SuppressWarnings("unchecked")
    private <T> void register(PacketType<T> type, BiConsumer<T, PacketContext> handler, boolean toClient) {
        REGISTRATIONS.put(type.id(), new Registration(type, (BiConsumer<Object, PacketContext>) (BiConsumer<?, ?>) handler, toClient));
    }

    @Override
    public <T> void registerS2C(PacketType<T> type, BiConsumer<T, PacketContext> handler) {
        register(type, handler, true);
    }

    @Override
    public <T> void registerC2S(PacketType<T> type, BiConsumer<T, PacketContext> handler) {
        register(type, handler, false);
    }

    private static <T> Envelope wrap(PacketType<T> type, T packet) {
        ByteBuf raw = Unpooled.buffer();
        FriendlyByteBuf buf = new FriendlyByteBuf(raw);
        type.codec().encode(packet, buf);
        byte[] data = new byte[raw.readableBytes()];
        raw.readBytes(data);
        raw.release();
        return new Envelope(type.id(), data);
    }

    @Override
    public <T> void sendToPlayer(ServerPlayer player, PacketType<T> type, T packet) {
        //? if >=1.21 {
        CHANNEL.send(wrap(type, packet), PacketDistributor.PLAYER.with(player));
        //?} else {
        /*CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), wrap(type, packet));
        *///?}
    }

    @Override
    public <T> void sendToServer(PacketType<T> type, T packet) {
        //? if >=1.21 {
        CHANNEL.send(wrap(type, packet), PacketDistributor.SERVER.noArg());
        //?} else {
        /*CHANNEL.sendToServer(wrap(type, packet));
        *///?}
    }

    record Context(Player player, boolean isClientSide) implements PacketContext {
    }
}
