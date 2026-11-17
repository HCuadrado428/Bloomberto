package com.dougg865.bloomberto.api.render.post;

import com.dougg865.bloomberto.Bloomberto;
import com.dougg865.bloomberto.impl.render.PostChainRunner;
import com.dougg865.bloomberto.impl.render.RenderCompat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/**
 * Registry and runner of full-screen effects. Effects that are enabled run in registration order every frame, after the
 * world is drawn and before the hand and HUD. Client only.
 *
 * <pre>{@code
 * PostEffects.register(Bloomberto.id("mymod", "vignette"), BuiltInEffects.Vignette::new);
 * PostEffects.get(Bloomberto.id("mymod", "vignette")).setIntensity(0.6f).enable();
 * }</pre>
 *
 * The built-in effects are already registered under {@code bloomberto:blur}, {@code bloomberto:bloom},
 * {@code bloomberto:vignette}, {@code bloomberto:desaturate}, {@code bloomberto:pixelate}, {@code bloomberto:color_grade}, {@code bloomberto:shake} and {@code bloomberto:zoom}.
 * While Iris, Oculus or OptiFine render with a shader pack, effects are applied to the pack's finished image just before the
 * HUD (see {@link #isSuppressed()}).
 */
public final class PostEffects {
    private static final Map<ResourceLocation, PostEffect> EFFECTS = new LinkedHashMap<>();
    private static final PostChainRunner RUNNER = new PostChainRunner();

    private PostEffects() {
    }

    /** Creates and registers an effect. @throws IllegalStateException if the id is taken */
    public static synchronized <T extends PostEffect> T register(ResourceLocation id, Supplier<T> factory) {
        if (EFFECTS.containsKey(id)) {
            throw new IllegalStateException("Post effect already registered: " + id);
        }
        T effect = factory.get();
        EFFECTS.put(id, effect);
        return effect;
    }

    /** @throws IllegalArgumentException if no effect has that id */
    public static synchronized PostEffect get(ResourceLocation id) {
        PostEffect effect = EFFECTS.get(id);
        if (effect == null) {
            throw new IllegalArgumentException("Unknown post effect: " + id);
        }
        return effect;
    }

    public static synchronized List<PostEffect> all() {
        return new ArrayList<>(EFFECTS.values());
    }

    /** {@code true} if {@code effect} failed to build and is being skipped (the reason is in the log); cleared by F3+T. */
    public static boolean hasFailed(PostEffect effect) {
        return RUNNER.isBroken(effect);
    }

    /**
     * {@code true} while a shader mod is rendering a shader pack. Effects still work then, but they run on the finished image
     * just before the HUD (so they also cover the hand) instead of right after the world.
     */
    public static boolean isSuppressed() {
        return RenderCompat.shaderPackActive();
    }

    /** Registers the built-in effects; called when the client starts. */
    public static synchronized void registerBuiltIns() {
        registerIfAbsent("blur", BuiltInEffects.Blur::new);
        registerIfAbsent("bloom", BuiltInEffects.Bloom::new);
        registerIfAbsent("vignette", BuiltInEffects.Vignette::new);
        registerIfAbsent("desaturate", BuiltInEffects.Desaturate::new);
        registerIfAbsent("pixelate", BuiltInEffects.Pixelate::new);
        registerIfAbsent("color_grade", BuiltInEffects.ColorGrade::new);
        registerIfAbsent("shake", BuiltInEffects.Shake::new);
        registerIfAbsent("zoom", BuiltInEffects.Zoom::new);
    }

    private static void registerIfAbsent(String name, Supplier<? extends PostEffect> factory) {
        ResourceLocation id = Bloomberto.id(name);
        if (!EFFECTS.containsKey(id)) {
            EFFECTS.put(id, factory.get());
        }
    }

    /** Advances effect fades by one tick; called every client tick. */
    public static void tick() {
        all().forEach(PostEffect::tick);
    }

    private static long framesDrawn;

    /** How many frames had at least one effect drawn (diagnostics and tests). */
    public static long framesDrawn() {
        return framesDrawn;
    }

    /**
     * Runs every active effect; called by Bloomberto in the world render event. While a shader pack is active this does
     * nothing: the world is then still in the shader mod's own buffers, so the effects run later, in {@link #renderOverHud}.
     */
    public static void render(float partialTick) {
        if (!isSuppressed()) {
            run(partialTick);
        }
    }

    /**
     * Runs the effects on the shader pack's finished image, right before the HUD is drawn; called by Bloomberto. Does
     * nothing without a shader pack (the effects already ran in {@link #render}). Here the effects also cover the hand.
     */
    public static void renderOverHud(float partialTick) {
        if (isSuppressed()) {
            run(partialTick);
        }
    }

    private static void run(float partialTick) {
        com.dougg865.bloomberto.impl.render.ClientReloads.ensureRegistered();
        if (Minecraft.getInstance().level == null) {
            return;
        }
        List<PostEffect> active = all().stream().filter(PostEffect::isActive).toList();
        if (active.isEmpty()) {
            return;
        }
        try {
            for (PostEffect effect : active) {
                RUNNER.run(effect, partialTick);
            }
            framesDrawn++;
        } finally {
            // even if an effect failed, the hand and HUD must be drawn into the main target, not into an effect's buffer
            Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
        }
    }

    /** Frees the GPU chains so they are rebuilt from resources; called after resource reloads. */
    public static void invalidate() {
        RUNNER.invalidate();
    }
}
