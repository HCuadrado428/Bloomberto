package com.dougg865.bloomberto.platform.fabric;

import com.dougg865.bloomberto.api.network.PacketContext;
import com.dougg865.bloomberto.api.network.PacketType;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
//? if >=1.21 {
import com.dougg865.bloomberto.impl.network.BloombertoPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?} else {
/*import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
*///?}

/** Client-only networking calls. Only loaded on the physical client. */
final class FabricClientNetwork {
    private FabricClientNetwork() {
    }

    static <T> void registerS2C(PacketType<T> type, BiConsumer<T, PacketContext> handler) {
        //? if >=1.21 {
        @SuppressWarnings("unchecked")
        CustomPacketPayload.Type<BloombertoPayload<T>> payloadType = BloombertoPayload.typeFor(type);
        ClientPlayNetworking.registerGlobalReceiver(payloadType, (payload, context) ->
                handler.accept(payload.packet(), new FabricNetworkHandler.Context(context.player(), true)));
        //?} else {
        /*ClientPlayNetworking.registerGlobalReceiver(type.id(), (client, listener, buf, responseSender) -> {
            T packet = type.codec().decode(buf);
            client.execute(() -> handler.accept(packet, new FabricNetworkHandler.Context(client.player, true)));
        });
        *///?}
    }

    //? if >=1.21 {
    static void send(CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }
    //?} else {
    /*static <T> void send(PacketType<T> type, T packet) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        type.codec().encode(packet, buf);
        ClientPlayNetworking.send(type.id(), buf);
    }
    *///?}
}
