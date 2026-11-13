package com.dougg865.bloomberto.api.client;

import com.dougg865.bloomberto.platform.Services;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;

/**
 * Registers key bindings that show up in the controls screen. Client only.
 *
 * <pre>{@code
 * Keybinds.register("mymod", "open_menu", GLFW.GLFW_KEY_M, "general", () -> openMenu());
 * }</pre>
 *
 * Register while your mod initializes: Forge and NeoForge only accept key mappings before the client finishes loading.
 * Translation keys are {@code key.<modId>.<name>} and {@code key.categories.<modId>.<category>}.
 */
public final class Keybinds {
    private static final List<KeyMapping> PENDING = new ArrayList<>();
    private static boolean flushed;

    private Keybinds() {
    }

    /**
     * @param glfwKey the default key, e.g. {@code GLFW.GLFW_KEY_M}
     * @param onPress runs once per press, on the client thread
     */
    public static KeyMapping register(String modId, String name, int glfwKey, String category, Runnable onPress) {
        KeyMapping mapping = new KeyMapping("key." + modId + "." + name, InputConstants.Type.KEYSYM, glfwKey,
                "key.categories." + modId + "." + category);
        synchronized (Keybinds.class) {
            if (flushed) {
                throw new IllegalStateException("Key mappings must be registered while the mod loads");
            }
            PENDING.add(mapping);
        }
        Services.PLATFORM.registerKeyMapping(mapping);
        ClientEvents.CLIENT_TICK.register(() -> {
            while (mapping.consumeClick()) {
                onPress.run();
            }
        });
        return mapping;
    }

    /** Key mappings waiting for the loader's registration event (Forge and NeoForge). */
    public static synchronized List<KeyMapping> drainPending() {
        List<KeyMapping> copy = new ArrayList<>(PENDING);
        PENDING.clear();
        flushed = true;
        return copy;
    }
}
