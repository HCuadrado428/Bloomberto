package com.dougg865.bloomberto.testmod;

import com.dougg865.bloomberto.Bloomberto;
import com.dougg865.bloomberto.platform.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Example mod that uses the Bloomberto API. Add one example here per feature as it is implemented. */
public final class TestMod {
    public static final String MOD_ID = "bloomberto_testmod";
    private static final Logger LOGGER = LoggerFactory.getLogger("Bloomberto Testmod");

    private TestMod() {
    }

    public static void init() {
        LOGGER.info("Testmod loaded on {}; id helper -> {}; bloomberto loaded -> {}",
                Services.PLATFORM.getLoader(), Bloomberto.id(MOD_ID, "example"),
                Services.PLATFORM.isModLoaded(Bloomberto.MOD_ID));
    }
}
