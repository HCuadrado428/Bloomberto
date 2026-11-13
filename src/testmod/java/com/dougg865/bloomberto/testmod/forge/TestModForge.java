package com.dougg865.bloomberto.testmod.forge;

import com.dougg865.bloomberto.testmod.TestMod;
import net.minecraftforge.fml.common.Mod;

@Mod(TestMod.MOD_ID)
public final class TestModForge {
    public TestModForge() {
        TestMod.init();
    }
}
