package com.dougg865.bloomberto.platform.forge;

import com.dougg865.bloomberto.platform.Loader;
import net.minecraftforge.fml.loading.FMLPaths;
import com.dougg865.bloomberto.platform.PlatformHelper;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLLoader;

public final class ForgePlatformHelper implements PlatformHelper {
    @Override
    public Loader getLoader() {
        return Loader.FORGE;
    }

    @Override
    public String getLoaderName() {
        return "Forge";
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
        return ModList.get() != null ? ModList.get().isLoaded(modId) : FMLLoader.getLoadingModList().getModFileById(modId) != null;
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }

    @Override
    public void registerConfigScreen(String modId, com.dougg865.bloomberto.api.config.Config config) {
        if (FMLEnvironment.dist.isClient()) {
            ForgeClientEvents.registerConfigScreen(config);
        }
    }

    @Override
    public boolean isClient() {
        return FMLEnvironment.dist.isClient();
    }
}
