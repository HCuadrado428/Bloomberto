package com.dougg865.bloomberto.platform.neoforge;

import com.dougg865.bloomberto.api.registry.Registrar;
import com.dougg865.bloomberto.impl.registry.RegistryHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Registers into the calling mod's event bus, so call {@code Registrar.register()} from that mod's constructor. */
public final class NeoForgeRegistryHandler implements RegistryHandler {
    @Override
    public void register(Registrar registrar) {
        IEventBus bus = ModLoadingContext.get().getActiveContainer().getEventBus();
        if (bus == null) {
            throw new IllegalStateException("No mod event bus: call Registrar.register() from the mod constructor");
        }
        bus.addListener((RegisterEvent event) -> {
            for (Registrar.Entry<?> entry : registrar.entries()) {
                registerOne(event, entry);
            }
        });
    }

    private static <T> void registerOne(RegisterEvent event, Registrar.Entry<T> entry) {
        if (event.getRegistryKey().equals(entry.registryKey())) {
            event.register(entry.registryKey(), helper -> helper.register(entry.id(), entry.create()));
        }
    }

    @Override
    public net.minecraft.world.item.CreativeModeTab.Builder creativeTabBuilder() {
        return net.minecraft.world.item.CreativeModeTab.builder();
    }
}
