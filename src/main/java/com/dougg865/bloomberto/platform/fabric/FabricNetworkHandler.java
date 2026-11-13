package com.dougg865.bloomberto.platform.fabric;

import com.dougg865.bloomberto.api.network.PacketContext;
import com.dougg865.bloomberto.api.network.PacketType;
import com.dougg865.bloomberto.impl.network.NetworkHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
//? if >=1.21 {
import com.dougg865.bloomberto.impl.network.BloombertoPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?} else {
/*import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
*///?}

public final class FabricNetworkHandler implements NetworkHandler {
    //? if >=1.21 {
    private final Map<PacketType<?>, CustomPacketPayload.Type<?>> payloadTypes = new ConcurrentHashMap<>();
    //?}

    private static boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    @Override
    public <T> void registerS2C(PacketType<T> type, BiConsumer<T, PacketContext> handler) {
        //? if >=1.21 {
        CustomPacketPayload.Type<BloombertoPayload<T>> payloadType = BloombertoPayload.typeFor(type);
        PayloadTypeRegistry.playS2C().register(payloadType, BloombertoPayload.codecFor(type, payloadType));
        payloadTypes.put(type, payloadType);
        //?}
        if (isClient()) {
            FabricClientNetwork.registerS2C(type, handler);
        }
    }

    @Override
    public <T> void registerC2S(PacketType<T> type, BiConsumer<T, PacketContext> handler) {
        //? if >=1.21 {
        CustomPacketPayload.Type<BloombertoPayload<T>> payloadType = BloombertoPayload.typeFor(type);
        PayloadTypeRegistry.playC2S().register(payloadType, BloombertoPayload.codecFor(type, payloadType));
        payloadTypes.put(type, payloadType);
        ServerPlayNetworking.registerGlobalReceiver(payloadType, (payload, context) ->
                handler.accept(payload.packet(), new Context(context.player(), false)));
        //?} else {
        /*ServerPlayNetworking.registerGlobalReceiver(type.id(), (server, player, listener, buf, responseSender) -> {
            T packet = type.codec().decode(buf);
            server.execute(() -> handler.accept(packet, new Context(player, false)));
        });
        *///?}
    }

    @Override
    public <T> void sendToPlayer(ServerPlayer player, PacketType<T> type, T packet) {
        //? if >=1.21 {
        @SuppressWarnings("unchecked")
        CustomPacketPayload.Type<BloombertoPayload<T>> payloadType =
                (CustomPacketPayload.Type<BloombertoPayload<T>>) payloadTypes.get(type);
        ServerPlayNetworking.send(player, new BloombertoPayload<>(type, payloadType, packet));
        //?} else {
        /*FriendlyByteBuf buf = PacketByteBufs.create();
        type.codec().encode(packet, buf);
        ServerPlayNetworking.send(player, type.id(), buf);
        *///?}
    }

    @Override
    public <T> void sendToServer(PacketType<T> type, T packet) {
        //? if >=1.21 {
        @SuppressWarnings("unchecked")
        CustomPacketPayload.Type<BloombertoPayload<T>> payloadType =
                (CustomPacketPayload.Type<BloombertoPayload<T>>) payloadTypes.get(type);
        FabricClientNetwork.send(new BloombertoPayload<>(type, payloadType, packet));
        //?} else {
        /*FabricClientNetwork.send(type, packet);
        *///?}
    }

    record Context(net.minecraft.world.entity.player.Player player, boolean isClientSide) implements PacketContext {
    }
}
