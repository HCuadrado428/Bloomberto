package com.dougg865.bloomberto.platform.fabric;

import com.dougg865.bloomberto.api.registry.Registrar;
import com.dougg865.bloomberto.impl.registry.RegistryHandler;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/** Fabric registries are open while mods initialize, so entries are registered immediately, in declaration order. */
public final class FabricRegistryHandler implements RegistryHandler {
    @Override
    public void register(Registrar registrar) {
        for (Registrar.Entry<?> entry : registrar.entries()) {
            registerOne(entry);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void registerOne(Registrar.Entry<T> entry) {
        Registry<T> registry = (Registry<T>) BuiltInRegistries.REGISTRY.get(entry.registryKey().location());
        if (registry == null) {
            throw new IllegalStateException("Unknown registry " + entry.registryKey().location());
        }
        Registry.register(registry, entry.id(), entry.create());
    }

    @Override
    public net.minecraft.world.item.CreativeModeTab.Builder creativeTabBuilder() {
        return net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup.builder();
    }
}
