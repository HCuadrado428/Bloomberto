package com.dougg865.bloomberto.testmod.fabric;

import com.dougg865.bloomberto.testmod.TestModClient;
import net.fabricmc.api.ClientModInitializer;

public final class TestModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        TestModClient.init();
    }
}
