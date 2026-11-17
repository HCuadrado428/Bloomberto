package com.dougg865.bloomberto.testmod;

import com.dougg865.bloomberto.Bloomberto;
import com.dougg865.bloomberto.api.animation.model.ModelLibrary;
import com.dougg865.bloomberto.api.client.ClientEvents;
import com.dougg865.bloomberto.api.client.ConfigScreen;
import com.dougg865.bloomberto.api.client.HudOverlays;
import com.dougg865.bloomberto.api.config.Configs;
import com.dougg865.bloomberto.api.data.EntityData;
import com.dougg865.bloomberto.api.render.CameraShake;
import com.dougg865.bloomberto.api.render.CameraZoom;
import com.dougg865.bloomberto.api.render.Shaders;
import com.dougg865.bloomberto.api.render.TextureHandle;
import com.dougg865.bloomberto.api.render.Textures;
import com.dougg865.bloomberto.api.render.post.PostEffect;
import com.dougg865.bloomberto.api.render.post.PostEffects;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import static com.dougg865.bloomberto.testmod.SelfTest.check;
import static com.dougg865.bloomberto.testmod.SelfTest.expect;

/** The client half of {@link SelfTest}: runs in a singleplayer world, checks what only a client can do, then closes the game. */
public final class SelfTestClient {
    private static final Queue<Runnable> ON_RENDER_THREAD = new ConcurrentLinkedQueue<>();
    private static int worldRenders;
    private static int hudRenders;
    private static int ticks = -1;
    private static int waitTicks;
    private static boolean shaderOk;
    private static boolean shaderOkAfterReload;
    private static volatile boolean reloadDone;
    private static final List<TextureHandle> TEXTURES = new ArrayList<>();
    private static BlockPos totemA;
    private static BlockPos totemB;
    private static List<PostEffect> effects;
    private static long framesBefore;
    private static int effectIndex = -1;
    private static long effectFrame;
    private static int drawnEffects;
    private static int effectTicks;

    private SelfTestClient() {
    }

    static void register() {
        if (!SelfTest.client()) {
            return;
        }
        ClientEvents.WORLD_RENDER.register(ctx -> worldRenders++);
        HudOverlays.register(Bloomberto.id(TestMod.MOD_ID, "selftest"), 2000, true, (graphics, partialTick) -> {
            hudRenders++;
            for (Runnable r; (r = ON_RENDER_THREAD.poll()) != null; ) {
                r.run();
            }
        });
        ClientEvents.CLIENT_TICK.register(SelfTestClient::tick);
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (ticks < 0) {
            // wait until the world is visible and the server has greeted us (or give up waiting after 10 s)
            if (!TestMod.greetingReceived && ++waitTicks < 200) {
                return;
            }
            ticks = 0;
        }
        try {
            step(mc, ticks++);
        } catch (Throwable e) {
            check("client step " + (ticks - 1), () -> {
                throw e;
            });
        }
    }

    private static void step(Minecraft mc, int t) throws Exception {
        switch (t) {
            case 0 -> start(mc);
            case 5 -> {
                check("camera shake", () -> expect(CameraShake.shared().isShaking(), "not shaking"));
                check("camera zoom", () -> expect(CameraZoom.shared().factor(0f) > 1.9f, "factor " + CameraZoom.shared().factor(0f)));
                CameraShake.shared().stop();
                CameraZoom.shared().zoomTo(1f, 0, null);
                effects = PostEffects.all();
                framesBefore = PostEffects.framesDrawn();
            }
            case 50 -> {
                check("post effects ran without failing" + (PostEffects.isSuppressed() ? " (over a shader pack)" : ""), () -> {
                    expect(drawnEffects == effects.size(), "only " + drawnEffects + " of " + effects.size() + " effects were drawn");
                    for (PostEffect e : effects) {
                        e.disable();
                    }
                    for (PostEffect e : PostEffects.all()) {
                        expect(!PostEffects.hasFailed(e), e.getClass().getSimpleName() + " failed to build");
                    }
                });
                check("textures (good, corrupt, oversized)", () -> {
                    expect(TEXTURES.get(0).isReady(), "good image: " + TEXTURES.get(0).state() + " " + TEXTURES.get(0).error());
                    expect(TEXTURES.get(0).width() == 4 && TEXTURES.get(0).height() == 2, "size");
                    expect(TEXTURES.get(1).state() == TextureHandle.State.FAILED, "corrupt image: " + TEXTURES.get(1).state());
                    expect(TEXTURES.get(2).state() == TextureHandle.State.FAILED, "oversized image: " + TEXTURES.get(2).state());
                });
                check("custom shader loads", () -> expect(shaderOk, "use() returned false"));
                check("two animated block entities both animate", () -> {
                    for (BlockPos pos : new BlockPos[] {totemA, totemB}) {
                        var be = mc.level.getBlockEntity(pos);
                        expect(be instanceof TotemBlockEntity, "no totem at " + pos + ": " + be);
                        float time = ((TotemBlockEntity) be).animationController().time();
                        expect(time > 0.5f, "totem at " + pos + " played only " + time + " s");
                    }
                });
                check("world and HUD render events", () -> expect(worldRenders > 20 && hudRenders > 20,
                        "world " + worldRenders + ", hud " + hudRenders));
                mc.setScreen(new TestScreen());
            }
            case 60 -> {
                check("ui example screen stays open", () -> expect(mc.screen instanceof TestScreen, "screen " + mc.screen));
                mc.setScreen(ConfigScreen.create(null, Configs.get(TestMod.MOD_ID)));
            }
            case 70 -> {
                check("config screen", () -> expect(mc.screen instanceof ConfigScreen, "screen " + mc.screen));
                mc.setScreen(null);
                check("tooltip lines", () -> {
                    List<Component> lines = tooltip(mc, new ItemStack(TestMod.EXAMPLE_ITEM.get()));
                    expect(lines.stream().anyMatch(c -> c.getString().contains("Shift")), "no shift hint in " + lines);
                });
                check("network and synced entity data", () -> {
                    expect(TestMod.greetingReceived, "no greeting from the server");
                    expect(EntityData.get(mc.player, TestMod.JOINS) >= 1, "joins on the client: " + EntityData.get(mc.player, TestMod.JOINS));
                });
                mc.reloadResourcePacks().thenRun(() -> reloadDone = true);
            }
            default -> {
                if (t > 5 && t < 50 && effects != null && effectIndex < effects.size()) {
                    // one effect at a time; move on only once a frame has really been drawn with it (slow GPUs draw few frames)
                    if (effectIndex < 0 || PostEffects.framesDrawn() > effectFrame) {
                        if (effectIndex >= 0) {
                            effects.get(effectIndex).disable();
                            drawnEffects++;
                        }
                        effectIndex++;
                        if (effectIndex < effects.size()) {
                            effectFrame = PostEffects.framesDrawn();
                            effects.get(effectIndex).setIntensity(1f).enable();
                        }
                    }
                    if (++effectTicks > 2400) {
                        effectIndex = effects.size(); // give up after 2 minutes; the check below then reports it
                    }
                    if (effectIndex < effects.size()) {
                        ticks = t; // hold the clock until every effect was drawn
                    }
                }
                if (t > 70 && reloadDone && !shaderOkAfterReload && t < 600) {
                    reloadDone = false;
                    ON_RENDER_THREAD.add(() -> shaderOkAfterReload = Shaders.create(Bloomberto.id(TestMod.MOD_ID, "demo"),
                            com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR).use());
                    ticks = 590; // finish shortly after the next frame
                }
                if (t == 600) {
                    check("after F3+T: shader and models load again", () -> {
                        expect(shaderOkAfterReload, "shader not usable after reload");
                        expect(!ModelLibrary.model(id("models/totem.bbmodel")).bones().isEmpty(), "model empty after reload");
                    });
                    SelfTest.summary("client");
                    if (SelfTest.exitWhenDone()) {
                        mc.stop();
                    }
                }
            }
        }
    }

    private static void start(Minecraft mc) throws Exception {
        check("translations", () -> {
            for (String key : new String[] {"key.bloomberto.toggle_debug", "bloomberto.config.reset_all",
                    "bloomberto.tooltip.hold_shift", "key.bloomberto_testmod.zoom"}) {
                expect(I18n.exists(key), "missing translation " + key);
            }
        });
        check("key mappings registered", () -> {
            List<String> names = new ArrayList<>();
            for (KeyMapping k : mc.options.keyMappings) {
                names.add(k.getName());
            }
            expect(names.contains("key.bloomberto.toggle_debug") && names.contains("key.bloomberto_testmod.zoom"), "missing");
        });
        check("blockbench models from resources", () -> {
            expect(!ModelLibrary.model(id("models/totem.bbmodel")).bones().isEmpty(), ".bbmodel");
            expect(!ModelLibrary.model(id("geo/totem.geo.json")).bones().isEmpty(), ".geo.json");
            expect(ModelLibrary.animations(id("models/totem.bbmodel")).clips().containsKey("animation.totem.sway"), "animations");
        });
        // textures load in the background; checked at tick 50
        TEXTURES.add(Textures.fromBytes(id("selftest_good"), png(4, 2)).load());
        TEXTURES.add(Textures.fromBytes(id("selftest_corrupt"), new byte[] {1, 2, 3, 4, 5}).load());
        TEXTURES.add(Textures.fromBytes(id("selftest_big"), png(64, 64)).maxPixels(100).load());
        ON_RENDER_THREAD.add(() -> shaderOk = Shaders.create(Bloomberto.id(TestMod.MOD_ID, "demo"),
                com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR).use());
        // two totems in front of the player, placed on the integrated server
        mc.player.setXRot(15f);
        var look = mc.player.getLookAngle();
        BlockPos feet = mc.player.blockPosition();
        totemA = feet.offset((int) Math.round(look.x * 4), 0, (int) Math.round(look.z * 4));
        totemB = totemA.offset((int) Math.round(-look.z), 0, (int) Math.round(look.x));
        if (totemB.equals(totemA)) {
            totemB = totemA.above();
        }
        var server = mc.getSingleplayerServer();
        expect(server != null, "self-test needs a singleplayer world");
        BlockPos a = totemA;
        BlockPos b = totemB;
        server.execute(() -> {
            ServerLevel level = server.getLevel(mc.level.dimension());
            level.setBlock(a, TestMod.TOTEM_BLOCK.get().defaultBlockState(), 3);
            level.setBlock(b, TestMod.TOTEM_BLOCK.get().defaultBlockState(), 3);
        });
        CameraShake.shared().shake(1f, 40);
        CameraZoom.shared().zoomTo(2f, 4, null);
    }

    /**
     * The real tooltip of a stack, built by Minecraft (so the loader's tooltip event runs). The testmod is compiled once for
     * both versions, so the 1.20.1 / 1.21 signatures of {@code getTooltipLines} are found by their parameter types.
     */
    @SuppressWarnings("unchecked")
    private static List<Component> tooltip(Minecraft mc, ItemStack stack) throws Exception {
        for (java.lang.reflect.Method m : ItemStack.class.getMethods()) {
            Class<?>[] p = m.getParameterTypes();
            if (m.getReturnType() != List.class || p.length < 2 || p[p.length - 1] != TooltipFlag.class) {
                continue;
            }
            if (p.length == 2) {
                return (List<Component>) m.invoke(stack, mc.player, TooltipFlag.NORMAL);
            }
            if (p.length == 3) {
                Object context = p[0].getMethod("of", net.minecraft.world.level.Level.class).invoke(null, mc.level);
                return (List<Component>) m.invoke(stack, context, mc.player, TooltipFlag.NORMAL);
            }
        }
        throw new AssertionError("no getTooltipLines method found");
    }

    private static ResourceLocation id(String path) {
        return Bloomberto.id(TestMod.MOD_ID, path);
    }

    private static byte[] png(int w, int h) throws Exception {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, 0xFFFF0000);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", out);
        return out.toByteArray();
    }
}
