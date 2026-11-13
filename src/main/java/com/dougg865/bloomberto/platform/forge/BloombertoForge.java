package com.dougg865.bloomberto.platform.forge;

import com.dougg865.bloomberto.Bloomberto;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(Bloomberto.MOD_ID)
public final class BloombertoForge {
    public BloombertoForge() {
        Bloomberto.init();
        ForgeEvents.register();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ForgeClientEvents.register();
        }
    }
}
