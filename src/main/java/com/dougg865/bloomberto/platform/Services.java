package com.dougg865.bloomberto.platform;

import com.dougg865.bloomberto.impl.network.NetworkHandler;
import com.dougg865.bloomberto.impl.registry.RegistryHandler;
import java.util.ServiceLoader;

/** Access point for the loader-specific services. */
public final class Services {
    public static final PlatformHelper PLATFORM = load(PlatformHelper.class);
    public static final NetworkHandler NETWORK = load(NetworkHandler.class);
    public static final RegistryHandler REGISTRY = load(RegistryHandler.class);

    private Services() {
    }

    private static <T> T load(Class<T> type) {
        return ServiceLoader.load(type, Services.class.getClassLoader()).findFirst()
                .orElseThrow(() -> new IllegalStateException("No implementation of " + type.getName() + " found"));
    }
}
