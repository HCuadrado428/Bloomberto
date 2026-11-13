package com.dougg865.bloomberto.api.client;

import com.dougg865.bloomberto.api.event.Event;
import net.minecraft.client.gui.GuiGraphics;

/** Client-only events. Never reference this class from code that can run on a dedicated server. */
public final class ClientEvents {
    /** End of every client tick. */
    public static final Event<Runnable> CLIENT_TICK = new Event<>("client_tick");
    /** The client left a world or server. */
    public static final Event<Runnable> DISCONNECT = new Event<>("disconnect");
    /**
     * After the world (terrain, entities, particles) has been drawn and before the hand and HUD. Use it to draw
     * in the world; post-processing effects also run here.
     */
    public static final Event<WorldRender> WORLD_RENDER = new Event<>("world_render");
    /** After the in-game HUD has been drawn. */
    public static final Event<HudRender> HUD_RENDER = new Event<>("hud_render");

    private ClientEvents() {
    }

    @FunctionalInterface
    public interface WorldRender {
        void render(WorldRenderContext context);
    }

    @FunctionalInterface
    public interface HudRender {
        void render(GuiGraphics graphics, float partialTick);
    }
}
