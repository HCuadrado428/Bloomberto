package com.dougg865.bloomberto.testmod;

import com.dougg865.bloomberto.Bloomberto;
import com.dougg865.bloomberto.api.animation.Easing;
import com.dougg865.bloomberto.api.client.ClientEvents;
import com.dougg865.bloomberto.api.render.ShaderProgram;
import com.dougg865.bloomberto.api.render.Shaders;
import com.dougg865.bloomberto.api.render.TextureHandle;
import com.dougg865.bloomberto.api.render.Textures;
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

    private static final String[] EFFECTS = {"blur", "bloom", "pixelate", "color_grade", "desaturate", "zoom"};
    private static int effectIndex = -1;
    private static boolean autoApplied;
    private static TextureHandle urlTexture;
    private static ShaderProgram demoShader;

    /** Registers the renderer of the totem block entity. Fabric calls it right away; Forge and NeoForge from their client setup. */
    public static void registerRenderers() {
        @SuppressWarnings("unchecked")
        var type = (net.minecraft.world.level.block.entity.BlockEntityType<TotemBlockEntity>)
                (net.minecraft.world.level.block.entity.BlockEntityType<?>) TestMod.TOTEM_BE.get();
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(type, ctx ->
                new com.dougg865.bloomberto.api.animation.model.AnimatedBlockEntityRenderer<TotemBlockEntity>(
                        Bloomberto.id(TestMod.MOD_ID, "geo/totem.geo.json"),
                        Bloomberto.id(TestMod.MOD_ID, "textures/entity/totem.png")));
    }

    public static void init() {
        // A key (default: Y) that turns on one post effect after another: blur, bloom, pixelate, color grade, desaturate, zoom, then none.
        Keybinds.register(TestMod.MOD_ID, "next_effect", org.lwjgl.glfw.GLFW.GLFW_KEY_Y, "general", () -> {
            for (String name : EFFECTS) {
                PostEffects.get(Bloomberto.id(name)).disable();
            }
            effectIndex = (effectIndex + 1) % (EFFECTS.length + 1);
            if (effectIndex == EFFECTS.length) {
                Minecraft.getInstance().gui.getChat().addMessage(Component.literal("[testmod] effect: none"));
                return;
            }
            var effect = PostEffects.get(Bloomberto.id(EFFECTS[effectIndex]));
            if (effect instanceof BuiltInEffects.ColorGrade grade) {
                grade.contrast(1.6f).saturation(1.8f).tint(Color.fromHex("#ffd9a0"));
            }
            effect.setIntensity(1f).enable();
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal("[testmod] effect: " + EFFECTS[effectIndex]));
        });
        // HUD demo: a texture downloaded from -Dbloomberto.demoUrl and a rectangle drawn with a custom shader.
        HudOverlays.register(Bloomberto.id(TestMod.MOD_ID, "demo"), (graphics, partialTick) -> {
            String url = System.getProperty("bloomberto.demoUrl");
            if (url != null && urlTexture == null) {
                urlTexture = Textures.fromUrl(Bloomberto.id(TestMod.MOD_ID, "url_demo"), url)
                        .placeholder(Bloomberto.id("minecraft", "textures/item/apple.png")).load();
            }
            if (urlTexture != null) {
                graphics.blit(urlTexture.location(), 8, 24, 0f, 0f, 32, 32, 32, 32);
                graphics.drawString(Minecraft.getInstance().font, urlTexture.state().name(), 44, 36, 0xFFFFFF);
            }
            if (demoShader == null) {
                demoShader = Shaders.create(Bloomberto.id(TestMod.MOD_ID, "demo"), com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR);
            }
            demoShader.setFloat("Time", (System.nanoTime() / 1_000_000L % 1_000_000L) / 1000f);
            demoShader.drawQuad(graphics.pose().last().pose(), 8, 64, 96, 18, 0xFF55AAFF);
            graphics.drawString(Minecraft.getInstance().font, "custom shader", 112, 69, 0xFFFFFF);
        });
        // A key (default: K) that toggles the demo HUD overlay, an overlay and a tooltip.
        net.minecraft.resources.ResourceLocation overlay = Bloomberto.id(TestMod.MOD_ID, "hud");
        HudOverlays.register(overlay, (graphics, partialTick) ->
                graphics.drawString(Minecraft.getInstance().font, "Bloomberto testmod HUD (press K)", 4, 4, 0xFFFFFF));
        Keybinds.register(TestMod.MOD_ID, "toggle_hud", org.lwjgl.glfw.GLFW.GLFW_KEY_K, "general",
                () -> HudOverlays.setVisible(overlay, !HudOverlays.isVisible(overlay)));
        // A key (default: J) that shakes the screen; a ribbon that follows the player.
        Keybinds.register(TestMod.MOD_ID, "shake", org.lwjgl.glfw.GLFW.GLFW_KEY_J, "general",
                () -> com.dougg865.bloomberto.api.render.CameraShake.shared().shake(1f, 30));
        // A key (default: Z) that toggles a 3x field-of-view zoom (works with shader packs).
        Keybinds.register(TestMod.MOD_ID, "zoom", org.lwjgl.glfw.GLFW.GLFW_KEY_Z, "general", () -> {
            var zoom = com.dougg865.bloomberto.api.render.CameraZoom.shared();
            if (zoom.factor(0f) > 1.5f) {
                zoom.reset(8, Easing.OUT_CUBIC);
            } else {
                zoom.zoomTo(3f, 8, Easing.OUT_CUBIC);
            }
        });
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

        // Debug: -Dbloomberto.autoEffect=blur turns an effect on as soon as a world is on screen (used by headless tests).
        ClientEvents.CLIENT_TICK.register(() -> {
            String auto = System.getProperty("bloomberto.autoEffect");
            if ("camera_shake".equals(auto) && Minecraft.getInstance().level != null) {
                var shake = com.dougg865.bloomberto.api.render.CameraShake.shared();
                if (!shake.isShaking()) {
                    shake.shake(1f, 200, 6f);
                }
            } else if ("camera_zoom".equals(auto) && !autoApplied && Minecraft.getInstance().level != null) {
                autoApplied = true;
                com.dougg865.bloomberto.api.render.CameraZoom.shared().zoomTo(3f, 8, Easing.OUT_CUBIC);
            } else if (auto != null && !autoApplied && Minecraft.getInstance().level != null) {
                autoApplied = true;
                PostEffects.get(Bloomberto.id(auto)).setIntensity(1f).enable();
            }
        });

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
                    .text(Component.literal("Bloomberto testmod"), spawn.add(0, 3.5, 0), 0.03f, Color.WHITE)
                    .billboard(Bloomberto.id("minecraft", "textures/item/apple.png"), spawn.add(2, 1.5, 0), 1f, Color.WHITE)
                    .throughWalls(true)
                    .text(Component.literal("through walls"), spawn.add(0, 4.3, 0), 0.02f, Color.fromHex("#ff55ff"));
        });
    }

    /** Also available: {@code new BuiltInEffects.Bloom().strength(2f)} registered with {@code PostEffects.register}. */
    @SuppressWarnings("unused")
    private static void moreExamples() {
        PostEffects.register(Bloomberto.id(TestMod.MOD_ID, "bloom"), () -> new BuiltInEffects.Bloom().strength(2f));
    }

    /**
     * Blockbench example (the files are in {@code assets/bloomberto_testmod/geo} and {@code animations}). An entity or block
     * entity implements {@code Animatable}, keeps one controller and plays clips by name:
     *
     * <pre>{@code
     * AnimationController controller = ModelLibrary.animations(Bloomberto.id(MOD_ID, "animations/totem.animation.json")).controller();
     * controller.play("animation.totem.idle").transition(5);
     * // register: EntityRenderers.register(TYPE, ctx -> new AnimatedEntityRenderer<>(ctx, geoId, textureId));
     * }</pre>
     */
    @SuppressWarnings("unused")
    private static com.dougg865.bloomberto.api.animation.model.AnimationController modelExample() {
        var controller = com.dougg865.bloomberto.api.animation.model.ModelLibrary
                .animations(Bloomberto.id(TestMod.MOD_ID, "animations/totem.animation.json")).controller();
        controller.play("animation.totem.idle").transition(5);
        return controller;
    }
}
