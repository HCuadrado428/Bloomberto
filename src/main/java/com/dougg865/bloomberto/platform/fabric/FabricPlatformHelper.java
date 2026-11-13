package com.dougg865.bloomberto.platform.fabric;

import com.dougg865.bloomberto.platform.Loader;
import com.dougg865.bloomberto.platform.PlatformHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

public final class FabricPlatformHelper implements PlatformHelper {
    @Override
    public Loader getLoader() {
        return Loader.FABRIC;
    }

    @Override
    public String getLoaderName() {
        return "Fabric Loader " + FabricLoader.getInstance().getModContainer("fabricloader")
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");
    }

    @Override
    public java.nio.file.Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }
}
