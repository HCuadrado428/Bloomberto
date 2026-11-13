package com.dougg865.bloomberto.testmod.neoforge;

import com.dougg865.bloomberto.testmod.TestMod;
import com.dougg865.bloomberto.testmod.TestModClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(TestMod.MOD_ID)
public final class TestModNeoForge {
    public TestModNeoForge() {
        TestMod.init();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            TestModClient.init();
        }
    }
}
