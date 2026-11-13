package com.dougg865.bloomberto.impl.render;

import com.dougg865.bloomberto.Bloomberto;
import com.dougg865.bloomberto.api.animation.TweenManager;
import com.dougg865.bloomberto.api.client.HudOverlays;
import com.dougg865.bloomberto.api.client.Keybinds;
import com.dougg865.bloomberto.api.render.Textures;
import com.dougg865.bloomberto.api.render.post.PostEffect;
import com.dougg865.bloomberto.api.render.post.PostEffects;
import com.dougg865.bloomberto.api.scheduler.Schedulers;
import com.dougg865.bloomberto.platform.Services;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

/** A small HUD panel with Bloomberto's runtime state, toggled with F8. Only available in development or with {@code -Dbloomberto.debugOverlay=true}. */
public final class DebugOverlay {
    private DebugOverlay() {
    }

    public static boolean enabled() {
        return Services.PLATFORM.isDevelopmentEnvironment() || Boolean.getBoolean("bloomberto.debugOverlay");
    }

    /** The lines shown in the panel. */
    static List<String> lines() {
        List<String> lines = new ArrayList<>();
        lines.add("Bloomberto debug");
        lines.add("tweens: " + TweenManager.shared().size() + "  client tasks: " + Schedulers.client().size());
        lines.add("textures: " + Textures.activeCount());
        for (PostEffect effect : PostEffects.all()) {
            if (effect.isEnabled()) {
                lines.add("fx " + effect.getClass().getSimpleName() + " " + String.format("%.2f", effect.getIntensity()));
            }
        }
        if (PostEffects.isSuppressed()) {
            lines.add("post effects paused: shader pack active");
        }
        return lines;
    }

    public static void register() {
        if (!enabled()) {
            return;
        }
        ResourceLocation id = Bloomberto.id("debug");
        HudOverlays.register(id, 1000, true, (graphics, partialTick) -> {
            Minecraft mc = Minecraft.getInstance();
            int y = 4;
            for (String line : lines()) {
                graphics.drawString(mc.font, line, 4, y, 0xFFFFAA, true);
                y += 10;
            }
        });
        HudOverlays.setVisible(id, false);
        Keybinds.register(Bloomberto.MOD_ID, "toggle_debug", GLFW.GLFW_KEY_F8, "general",
                () -> HudOverlays.setVisible(id, !HudOverlays.isVisible(id)));
    }
}
