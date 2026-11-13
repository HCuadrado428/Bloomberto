package com.dougg865.bloomberto.platform.forge;

import com.dougg865.bloomberto.platform.Loader;
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
    public boolean isModLoaded(String modId) {
        return ModList.get() != null ? ModList.get().isLoaded(modId) : FMLLoader.getLoadingModList().getModFileById(modId) != null;
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }

    @Override
    public boolean isClient() {
        return FMLEnvironment.dist.isClient();
    }
}
