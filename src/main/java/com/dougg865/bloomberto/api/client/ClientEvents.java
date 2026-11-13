package com.dougg865.bloomberto.api.client;

import com.dougg865.bloomberto.api.event.Event;
import net.minecraft.client.gui.GuiGraphics;

/** Client-only events. Never reference this class from code that can run on a dedicated server. */
public final class ClientEvents {
    /** End of every client tick. */
    public static final Event<Runnable> CLIENT_TICK = new Event<>("client_tick");
    /** After the in-game HUD has been drawn. */
    public static final Event<HudRender> HUD_RENDER = new Event<>("hud_render");

    private ClientEvents() {
    }

    @FunctionalInterface
    public interface HudRender {
        void render(GuiGraphics graphics, float partialTick);
    }
}
