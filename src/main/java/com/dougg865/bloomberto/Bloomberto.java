package com.dougg865.bloomberto;

import com.dougg865.bloomberto.api.animation.TweenManager;
import com.dougg865.bloomberto.api.event.BloombertoEvents;
import com.dougg865.bloomberto.api.scheduler.Schedulers;
import com.dougg865.bloomberto.platform.Services;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point and shared helpers of Bloomberto.
 *
 * <p>The public API is identical for every supported Minecraft version and loader;
 * version differences are handled inside this library.
 */
public final class Bloomberto {
    /** The mod id of Bloomberto itself. */
    public static final String MOD_ID = "bloomberto";
    public static final String MOD_NAME = "Bloomberto";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private Bloomberto() {
    }

    /** Called once by each loader entrypoint. */
    public static void init() {
        LOGGER.info("Initializing {} on {} ({})", MOD_NAME,
                Services.PLATFORM.getLoader(), Services.PLATFORM.getLoaderName());
        BloombertoEvents.SERVER_TICK.register(server -> Schedulers.server().tick());
        com.dougg865.bloomberto.impl.config.ConfigSync.register();
        com.dougg865.bloomberto.impl.data.DataSync.register();
        // Touch the network service early so loader channels exist before other mods register packets.
        LOGGER.debug("Networking: {}", Services.NETWORK.getClass().getSimpleName());
        if (Boolean.getBoolean("bloomberto.debugEvents")) {
            registerDebugListeners();
        }
    }

    /** Logs the first occurrence of each event; enabled with {@code -Dbloomberto.debugEvents=true}. */
    private static void registerDebugListeners() {
        com.dougg865.bloomberto.impl.network.DebugPackets.register();
        com.dougg865.bloomberto.impl.registry.DebugContent.register();
        AtomicBoolean firstTick = new AtomicBoolean(true);
        BloombertoEvents.SERVER_TICK.register(server -> {
            if (firstTick.getAndSet(false)) {
                LOGGER.info("[debugEvents] server_tick");
            }
        });
        BloombertoEvents.LEVEL_LOAD.register(level -> {
            LOGGER.info("[debugEvents] level_load {}", level.dimension().location());
            com.dougg865.bloomberto.impl.registry.DebugContent.verify();
            LOGGER.info("[debugEvents] entity data store ok ({} entities)",
                    com.dougg865.bloomberto.impl.data.EntityDataSavedData.get(level.getServer()).store().entityCount());
        });
        BloombertoEvents.PLAYER_JOIN.register(p -> LOGGER.info("[debugEvents] player_join {}", p.getGameProfile().getName()));
        BloombertoEvents.PLAYER_LEAVE.register(p -> LOGGER.info("[debugEvents] player_leave {}", p.getGameProfile().getName()));
    }

    /** Called by the client entrypoint of each loader, after {@link #init()}. Never call on a server. */
    public static void initClient() {
        com.dougg865.bloomberto.api.render.Shaders.listenForReloads();
        com.dougg865.bloomberto.api.render.post.PostEffects.registerBuiltIns();
        com.dougg865.bloomberto.impl.render.DebugOverlay.register();
        com.dougg865.bloomberto.api.client.ClientEvents.WORLD_RENDER.register(
                ctx -> com.dougg865.bloomberto.api.render.post.PostEffects.render(ctx.partialTick()));
        com.dougg865.bloomberto.api.client.ClientEvents.DISCONNECT.register(
                com.dougg865.bloomberto.impl.config.ConfigSync::clearServerValues);
        com.dougg865.bloomberto.api.client.ClientEvents.DISCONNECT.register(
                com.dougg865.bloomberto.impl.data.ClientEntityData::clear);
        com.dougg865.bloomberto.api.client.ClientEvents.CLIENT_TICK.register(() -> {
            com.dougg865.bloomberto.impl.render.ClientReloads.ensureRegistered();
            Schedulers.client().tick();
            com.dougg865.bloomberto.api.render.post.PostEffects.tick();
            TweenManager.shared().tick();
        });
    }

    /**
     * Creates a {@link ResourceLocation}. Minecraft 1.21 made the constructor private,
     * so all code should go through this helper.
     */
    public static ResourceLocation id(String namespace, String path) {
        //? if >=1.21 {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
        //?} else {
        /*return new ResourceLocation(namespace, path);
        *///?}
    }

    /** Creates a {@link ResourceLocation} in Bloomberto's own namespace. */
    public static ResourceLocation id(String path) {
        return id(MOD_ID, path);
    }
}
