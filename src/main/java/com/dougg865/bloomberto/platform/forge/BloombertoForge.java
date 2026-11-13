package com.dougg865.bloomberto.platform.forge;

import com.dougg865.bloomberto.Bloomberto;
import net.minecraftforge.fml.common.Mod;

@Mod(Bloomberto.MOD_ID)
public final class BloombertoForge {
    public BloombertoForge() {
        Bloomberto.init();
    }
}
