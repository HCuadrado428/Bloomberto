package com.dougg865.bloomberto.testmod.fabric;

import com.dougg865.bloomberto.testmod.TestMod;
import net.fabricmc.api.ModInitializer;

public final class TestModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        TestMod.init();
    }
}
