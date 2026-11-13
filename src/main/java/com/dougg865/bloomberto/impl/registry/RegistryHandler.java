package com.dougg865.bloomberto.impl.registry;

import com.dougg865.bloomberto.api.registry.Registrar;

/** Loader-specific registration; implemented once per loader. Use {@link Registrar#register()} instead. */
public interface RegistryHandler {
    void register(Registrar registrar);

    /**
     * A creative tab builder. Vanilla only has {@code builder(Row, int)}; every loader adds its own way to create a tab
     * that is placed automatically.
     */
    net.minecraft.world.item.CreativeModeTab.Builder creativeTabBuilder();
}
