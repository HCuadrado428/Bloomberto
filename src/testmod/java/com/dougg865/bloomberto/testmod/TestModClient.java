package com.dougg865.bloomberto.testmod;

import com.dougg865.bloomberto.Bloomberto;
import com.dougg865.bloomberto.api.animation.Easing;
import com.dougg865.bloomberto.api.client.ClientEvents;
import com.dougg865.bloomberto.api.client.HudOverlays;
import com.dougg865.bloomberto.api.client.Keybinds;
import com.dougg865.bloomberto.api.client.Tooltips;
import com.dougg865.bloomberto.api.effects.Trail;
import com.dougg865.bloomberto.api.render.post.BuiltInEffects;
import com.dougg865.bloomberto.api.color.Color;
import com.dougg865.bloomberto.api.render.WorldDraw;
import com.dougg865.bloomberto.api.render.post.BuiltInEffects;
import com.dougg865.bloomberto.api.render.post.PostEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Client-only examples: world drawing and a post-processing effect. Never load this class on a dedicated server. */
public final class TestModClient {
    private static boolean effectShown;

    private TestModClient() {
    }

    public static void init() {
        // A key (default: K) that toggles the demo HUD overlay, an overlay and a tooltip.
        net.minecraft.resources.ResourceLocation overlay = Bloomberto.id(TestMod.MOD_ID, "hud");
        HudOverlays.register(overlay, (graphics, partialTick) ->
                graphics.drawString(Minecraft.getInstance().font, "Bloomberto testmod HUD (press K)", 4, 4, 0xFFFFFF));
        Keybinds.register(TestMod.MOD_ID, "toggle_hud", org.lwjgl.glfw.GLFW.GLFW_KEY_K, "general",
                () -> HudOverlays.setVisible(overlay, !HudOverlays.isVisible(overlay)));
        // A key (default: J) that shakes the screen; a ribbon that follows the player.
        Keybinds.register(TestMod.MOD_ID, "shake", org.lwjgl.glfw.GLFW.GLFW_KEY_J, "general",
                () -> ((BuiltInEffects.Shake) PostEffects.get(Bloomberto.id("shake"))).trigger(1f, 20));
        Trail trail = new Trail(30, 30);
        ClientEvents.CLIENT_TICK.register(() -> {
            if (Minecraft.getInstance().player != null) {
                trail.add(Minecraft.getInstance().player.position().add(0, 0.1, 0));
            }
            trail.tick();
        });
        ClientEvents.WORLD_RENDER.register(ctx ->
                WorldDraw.of(ctx).trail(trail, 0.4f, Color.ofRgba(255, 200, 0, 200), Color.ofRgba(255, 0, 0, 0)));
        // A key (default: U) that opens the ui example screen.
        Keybinds.register(TestMod.MOD_ID, "open_ui", org.lwjgl.glfw.GLFW.GLFW_KEY_U, "general",
                () -> Minecraft.getInstance().setScreen(new TestScreen()));
        Tooltips.shiftInfo(stack -> stack.is(TestMod.EXAMPLE_ITEM.get()),
                java.util.List.of(Component.literal("An example item from the Bloomberto testmod.")));

        // A vignette that fades in the first time a world is on screen.
        ClientEvents.CLIENT_TICK.register(() -> {
            if (!effectShown && Minecraft.getInstance().level != null && TestMod.EFFECTS_ENABLED.get()) {
                effectShown = true;
                PostEffects.get(Bloomberto.id("vignette")).fadeIn(0.5f, 40, Easing.OUT_CUBIC);
            }
        });

        // A box, a line and floating text at the world spawn.
        ClientEvents.WORLD_RENDER.register(ctx -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || !TestMod.EFFECTS_ENABLED.get()) {
                return;
            }
            Vec3 spawn = Vec3.atCenterOf(mc.level.getSharedSpawnPos());
            WorldDraw.of(ctx)
                    .box(new AABB(spawn, spawn).inflate(0.5), Color.fromHex("#ffcc00"))
                    .line(spawn, spawn.add(0, 3, 0), Color.fromHex("#00ccff"))
                    .text(Component.literal("Bloomberto testmod"), spawn.add(0, 3.5, 0), 0.03f, Color.WHITE);
        });
    }

    /** Also available: {@code new BuiltInEffects.Bloom().strength(2f)} registered with {@code PostEffects.register}. */
    @SuppressWarnings("unused")
    private static void moreExamples() {
        PostEffects.register(Bloomberto.id(TestMod.MOD_ID, "bloom"), () -> new BuiltInEffects.Bloom().strength(2f));
    }
}
