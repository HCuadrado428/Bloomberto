package com.dougg865.bloomberto;

import com.dougg865.bloomberto.platform.Services;
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
