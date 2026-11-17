package com.dougg865.bloomberto.platform;

/** Loader-specific functionality, implemented once per loader and found through {@link java.util.ServiceLoader}. */
public interface PlatformHelper {
    Loader getLoader();

    /** Human readable loader name and version, for logging. */
    String getLoaderName();

    boolean isModLoaded(String modId);

    /** The game directory (where {@code config}, {@code mods} and {@code saves} live). */
    java.nio.file.Path getGameDir();

    /** The loader's config directory (usually {@code <game dir>/config}). */
    java.nio.file.Path getConfigDir();

    boolean isDevelopmentEnvironment();

    /**
     * Shows a "config" button for the mod in the loader's mod list, opening the generated config screen. Does nothing
     * on a dedicated server and on Fabric (Fabric has no mod list of its own; use Mod Menu's API with
     * {@code ConfigScreen.create}).
     */
    /** Registers a key mapping with the loader. Client only; see {@code Keybinds}. */
    default void registerKeyMapping(net.minecraft.client.KeyMapping mapping) {
    }

    default void registerConfigScreen(String modId, com.dougg865.bloomberto.api.config.Config config) {
    }

    /** {@code true} when running on the physical client (not a dedicated server). */
    boolean isClient();

    /**
     * Creates a block entity type for {@code blocks}. Minecraft 1.21.11 made the vanilla constructor private, so each
     * loader provides its own way; older versions use the vanilla builder.
     */
    default <T extends net.minecraft.world.level.block.entity.BlockEntity> net.minecraft.world.level.block.entity.BlockEntityType<T>
            createBlockEntityType(java.util.function.BiFunction<net.minecraft.core.BlockPos, net.minecraft.world.level.block.state.BlockState, T> factory,
                                  net.minecraft.world.level.block.Block... blocks) {
        //? if >=1.21.11 {
        /*try {
            // the constructor and BlockEntitySupplier are private; loaders that have an API for this override the method
            Class<?> supplierType = Class.forName(net.minecraft.world.level.block.entity.BlockEntityType.class.getName() + "$BlockEntitySupplier");
            Object supplier = java.lang.reflect.Proxy.newProxyInstance(supplierType.getClassLoader(), new Class<?>[] {supplierType},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "create" -> factory.apply((net.minecraft.core.BlockPos) args[0],
                                (net.minecraft.world.level.block.state.BlockState) args[1]);
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        case "toString" -> "BlockEntitySupplier(" + factory + ")";
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
            var constructor = net.minecraft.world.level.block.entity.BlockEntityType.class.getDeclaredConstructor(supplierType, java.util.Set.class);
            constructor.setAccessible(true);
            @SuppressWarnings("unchecked")
            var type = (net.minecraft.world.level.block.entity.BlockEntityType<T>) constructor.newInstance(supplier, java.util.Set.of(blocks));
            return type;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not create a block entity type", e);
        }
        *///?} else {
        return net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(factory::apply, blocks).build(null);
        //?}
    }
}
