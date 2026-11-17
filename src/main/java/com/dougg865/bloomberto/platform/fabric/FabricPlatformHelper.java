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
    public java.nio.file.Path getGameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

    @Override
    public java.nio.file.Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public void registerKeyMapping(net.minecraft.client.KeyMapping mapping) {
        FabricClientHooks.registerKeyMapping(mapping);
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

    //? if >=1.21.11 {
    /*@Override
    public <T extends net.minecraft.world.level.block.entity.BlockEntity> net.minecraft.world.level.block.entity.BlockEntityType<T>
            createBlockEntityType(java.util.function.BiFunction<net.minecraft.core.BlockPos, net.minecraft.world.level.block.state.BlockState, T> factory,
                                  net.minecraft.world.level.block.Block... blocks) {
        return net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(factory::apply, blocks).build();
    }
    *///?}
}
