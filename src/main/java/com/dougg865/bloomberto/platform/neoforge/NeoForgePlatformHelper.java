package com.dougg865.bloomberto.platform.neoforge;

import com.dougg865.bloomberto.platform.Loader;
import net.neoforged.fml.loading.FMLPaths;
import com.dougg865.bloomberto.platform.PlatformHelper;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.LoadingModList;

public final class NeoForgePlatformHelper implements PlatformHelper {
    @Override
    public Loader getLoader() {
        return Loader.NEOFORGE;
    }

    @Override
    public String getLoaderName() {
        return "NeoForge";
    }

    @Override
    public java.nio.file.Path getGameDir() {
        return FMLPaths.GAMEDIR.get();
    }

    @Override
    public java.nio.file.Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get() != null ? ModList.get().isLoaded(modId) : LoadingModList.get().getModFileById(modId) != null;
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }

    @Override
    public void registerConfigScreen(String modId, com.dougg865.bloomberto.api.config.Config config) {
        if (FMLEnvironment.dist.isClient()) {
            NeoForgeClientEvents.registerConfigScreen(config);
        }
    }

    @Override
    public boolean isClient() {
        return FMLEnvironment.dist.isClient();
    }
}
