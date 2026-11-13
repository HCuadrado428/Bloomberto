package com.dougg865.bloomberto.platform.forge;

import com.dougg865.bloomberto.api.registry.Registrar;
import com.dougg865.bloomberto.impl.registry.RegistryHandler;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;

/** Registers into the calling mod's event bus, so call {@code Registrar.register()} from that mod's constructor. */
public final class ForgeRegistryHandler implements RegistryHandler {
    @Override
    public void register(Registrar registrar) {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
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
