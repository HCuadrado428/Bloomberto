package com.dougg865.bloomberto.platform.fabric;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;

/** Client-only Fabric calls, kept apart so the server never loads them. */
final class FabricClientHooks {
    private FabricClientHooks() {
    }

    static void registerKeyMapping(KeyMapping mapping) {
        KeyBindingHelper.registerKeyBinding(mapping);
    }
}
