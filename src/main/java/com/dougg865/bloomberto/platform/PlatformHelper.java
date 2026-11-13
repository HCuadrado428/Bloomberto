package com.dougg865.bloomberto.platform;

/** Loader-specific functionality, implemented once per loader and found through {@link java.util.ServiceLoader}. */
public interface PlatformHelper {
    Loader getLoader();

    /** Human readable loader name and version, for logging. */
    String getLoaderName();

    boolean isModLoaded(String modId);

    /** The loader's config directory (usually {@code <game dir>/config}). */
    java.nio.file.Path getConfigDir();

    boolean isDevelopmentEnvironment();

    /**
     * Shows a "config" button for the mod in the loader's mod list, opening the generated config screen. Does nothing
     * on a dedicated server and on Fabric (Fabric has no mod list of its own; use Mod Menu's API with
     * {@code ConfigScreen.create}).
     */
    default void registerConfigScreen(String modId, com.dougg865.bloomberto.api.config.Config config) {
    }

    /** {@code true} when running on the physical client (not a dedicated server). */
    boolean isClient();
}
