package com.dougg865.bloomberto.api.client;

import com.dougg865.bloomberto.Bloomberto;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Named, ordered HUD overlays that can be shown or hidden. Draws after the vanilla HUD, and not while a screen is open
 * unless the overlay asks for it. Client only.
 *
 * <pre>{@code
 * HudOverlays.register(Bloomberto.id("mymod", "mana"), (graphics, partialTick) ->
 *         graphics.drawString(font, "Mana: " + mana, 4, 4, 0xFFFFFF));
 * HudOverlays.setVisible(Bloomberto.id("mymod", "mana"), false);
 * }</pre>
 */
public final class HudOverlays {
    /** Draws one overlay. */
    @FunctionalInterface
    public interface Overlay {
        void render(GuiGraphics graphics, float partialTick);
    }

    private static final class Entry {
        final ResourceLocation id;
        final int order;
        final boolean showOverScreens;
        final Overlay overlay;
        volatile boolean visible = true;

        Entry(ResourceLocation id, int order, boolean showOverScreens, Overlay overlay) {
            this.id = id;
            this.order = order;
            this.showOverScreens = showOverScreens;
            this.overlay = overlay;
        }
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static boolean hooked;

    private HudOverlays() {
    }

    /** Registers an overlay drawn in registration order. */
    public static void register(ResourceLocation id, Overlay overlay) {
        register(id, 0, false, overlay);
    }

    /**
     * @param order          overlays with a lower order are drawn first (below)
     * @param showOverScreens draw even while a screen (inventory, menu...) is open
     */
    public static synchronized void register(ResourceLocation id, int order, boolean showOverScreens, Overlay overlay) {
        if (find(id) != null) {
            throw new IllegalStateException("HUD overlay already registered: " + id);
        }
        ENTRIES.add(new Entry(id, order, showOverScreens, overlay));
        ENTRIES.sort(Comparator.comparingInt(e -> e.order)); // stable: equal orders keep registration order
        hook();
    }

    public static synchronized void setVisible(ResourceLocation id, boolean visible) {
        Entry e = find(id);
        if (e == null) {
            throw new IllegalArgumentException("Unknown HUD overlay: " + id);
        }
        e.visible = visible;
    }

    public static synchronized boolean isVisible(ResourceLocation id) {
        Entry e = find(id);
        return e != null && e.visible;
    }

    private static Entry find(ResourceLocation id) {
        for (Entry e : ENTRIES) {
            if (e.id.equals(id)) {
                return e;
            }
        }
        return null;
    }

    private static void hook() {
        if (hooked) {
            return;
        }
        hooked = true;
        ClientEvents.HUD_RENDER.register(HudOverlays::renderAll);
    }

    private static void renderAll(GuiGraphics graphics, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) {
            return;
        }
        List<Entry> snapshot;
        synchronized (HudOverlays.class) {
            snapshot = new ArrayList<>(ENTRIES);
        }
        boolean screenOpen = mc.screen != null;
        for (Entry e : snapshot) {
            if (e.visible && (!screenOpen || e.showOverScreens)) {
                try {
                    e.overlay.render(graphics, partialTick);
                } catch (RuntimeException ex) {
                    Bloomberto.LOGGER.error("HUD overlay {} failed and was hidden", e.id, ex);
                    e.visible = false;
                }
            }
        }
    }
}
