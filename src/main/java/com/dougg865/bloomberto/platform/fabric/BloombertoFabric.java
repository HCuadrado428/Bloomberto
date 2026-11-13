package com.dougg865.bloomberto.platform.fabric;

import com.dougg865.bloomberto.Bloomberto;
import net.fabricmc.api.ModInitializer;

public final class BloombertoFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Bloomberto.init();
    }
}
