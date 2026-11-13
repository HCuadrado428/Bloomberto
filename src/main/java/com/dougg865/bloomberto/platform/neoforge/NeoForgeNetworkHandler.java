package com.dougg865.bloomberto.platform.neoforge;

import com.dougg865.bloomberto.api.network.PacketContext;
import com.dougg865.bloomberto.api.network.PacketType;
import com.dougg865.bloomberto.impl.network.BloombertoPayload;
import com.dougg865.bloomberto.impl.network.NetworkHandler;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * NeoForge only accepts payload registration while {@link RegisterPayloadHandlersEvent} fires, so
 * registrations are queued here and applied by {@link #onRegisterPayloads}.
 */
public final class NeoForgeNetworkHandler implements NetworkHandler {
    private static final List<Consumer<PayloadRegistrar>> QUEUED = new CopyOnWriteArrayList<>();
    private static volatile boolean registrationClosed;

    private final Map<PacketType<?>, CustomPacketPayload.Type<?>> payloadTypes = new ConcurrentHashMap<>();

    /** Called from the mod event bus (see {@code BloombertoNeoForge}). */
    static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        for (Consumer<PayloadRegistrar> action : QUEUED) {
            action.accept(registrar);
        }
        registrationClosed = true;
    }

    private static void enqueue(Consumer<PayloadRegistrar> action) {
        if (registrationClosed) {
            throw new IllegalStateException("Packets must be registered while the mod is loading, before payload registration closes");
        }
        QUEUED.add(action);
    }

    @Override
    public <T> void registerS2C(PacketType<T> type, BiConsumer<T, PacketContext> handler) {
        CustomPacketPayload.Type<BloombertoPayload<T>> payloadType = BloombertoPayload.typeFor(type);
        payloadTypes.put(type, payloadType);
        enqueue(registrar -> registrar.playToClient(payloadType, BloombertoPayload.codecFor(type, payloadType),
                (payload, context) -> handler.accept(payload.packet(), new Context(context.player(), true))));
    }

    @Override
    public <T> void registerC2S(PacketType<T> type, BiConsumer<T, PacketContext> handler) {
        CustomPacketPayload.Type<BloombertoPayload<T>> payloadType = BloombertoPayload.typeFor(type);
        payloadTypes.put(type, payloadType);
        enqueue(registrar -> registrar.playToServer(payloadType, BloombertoPayload.codecFor(type, payloadType),
                (payload, context) -> handler.accept(payload.packet(), new Context(context.player(), false))));
    }

    @Override
    public <T> void sendToPlayer(ServerPlayer player, PacketType<T> type, T packet) {
        PacketDistributor.sendToPlayer(player, new BloombertoPayload<>(type, payloadType(type), packet));
    }

    @Override
    public <T> void sendToServer(PacketType<T> type, T packet) {
        PacketDistributor.sendToServer(new BloombertoPayload<>(type, payloadType(type), packet));
    }

    @SuppressWarnings("unchecked")
    private <T> CustomPacketPayload.Type<BloombertoPayload<T>> payloadType(PacketType<T> type) {
        return (CustomPacketPayload.Type<BloombertoPayload<T>>) payloadTypes.get(type);
    }

    record Context(Player player, boolean isClientSide) implements PacketContext {
    }
}
