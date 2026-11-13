package com.dougg865.bloomberto.platform.neoforge;

import com.dougg865.bloomberto.Bloomberto;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(Bloomberto.MOD_ID)
public final class BloombertoNeoForge {
    public BloombertoNeoForge() {
        Bloomberto.init();
        NeoForgeEvents.register();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            NeoForgeClientEvents.register();
        }
    }
}
