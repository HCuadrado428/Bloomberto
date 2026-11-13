package com.dougg865.bloomberto.testmod.forge;

import com.dougg865.bloomberto.testmod.TestMod;
import com.dougg865.bloomberto.testmod.TestModClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(TestMod.MOD_ID)
public final class TestModForge {
    public TestModForge() {
        TestMod.init();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            TestModClient.init();
        }
    }
}
