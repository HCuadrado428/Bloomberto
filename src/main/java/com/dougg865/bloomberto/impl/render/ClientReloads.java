package com.dougg865.bloomberto.impl.render;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

/**
 * Runs Bloomberto's cache-clearing callbacks after a resource reload (F3+T).
 *
 * <p>Mods are initialized while Minecraft is still being constructed, when the client's resource manager does not exist yet,
 * so callbacks are collected here and handed to vanilla by {@link #ensureRegistered()} as soon as the manager exists
 * (called on the first client tick and before anything that needs a reloadable resource).
 */
public final class ClientReloads {
    private static final List<Runnable> CALLBACKS = new CopyOnWriteArrayList<>();
    private static boolean registered;

    private ClientReloads() {
    }

    /** Adds a callback to run after every resource reload. Safe to call at any time, including during mod initialization. */
    public static void onReload(Runnable callback) {
        CALLBACKS.add(callback);
    }

    /**
     * Registers with vanilla once the resource manager exists. Cheap and idempotent: call it as often as convenient.
     *
     * @return {@code true} once registered
     */
    public static synchronized boolean ensureRegistered() {
        if (registered) {
            return true;
        }
        Minecraft mc = Minecraft.getInstance();
        ResourceManager manager = mc == null ? null : mc.getResourceManager();
        if (manager instanceof ReloadableResourceManager reloadable) {
            reloadable.registerReloadListener((ResourceManagerReloadListener) m -> CALLBACKS.forEach(Runnable::run));
            registered = true;
        }
        return registered;
    }
}
