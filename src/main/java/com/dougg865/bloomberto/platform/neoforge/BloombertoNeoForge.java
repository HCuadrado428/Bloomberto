package com.dougg865.bloomberto.platform.neoforge;

import com.dougg865.bloomberto.Bloomberto;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(Bloomberto.MOD_ID)
public final class BloombertoNeoForge {
    public BloombertoNeoForge(IEventBus modEventBus) {
        modEventBus.addListener(NeoForgeNetworkHandler::onRegisterPayloads);
        Bloomberto.init();
        NeoForgeEvents.register();
        //? if >=1.21.11 {
        /*if (FMLEnvironment.getDist() == Dist.CLIENT) {
        *///?} else {
        if (FMLEnvironment.dist == Dist.CLIENT) {
        //?}
            NeoForgeClientEvents.register(modEventBus);
        }
    }
}
