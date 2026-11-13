package com.dougg865.bloomberto.platform;

/** Loader-specific functionality, implemented once per loader and found through {@link java.util.ServiceLoader}. */
public interface PlatformHelper {
    Loader getLoader();

    /** Human readable loader name and version, for logging. */
    String getLoaderName();

    boolean isModLoaded(String modId);

    boolean isDevelopmentEnvironment();

    /** {@code true} when running on the physical client (not a dedicated server). */
    boolean isClient();
}
