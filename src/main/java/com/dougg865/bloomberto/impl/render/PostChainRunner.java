package com.dougg865.bloomberto.impl.render;

import com.dougg865.bloomberto.Bloomberto;
import com.dougg865.bloomberto.api.render.post.ChainSpec;
import com.dougg865.bloomberto.api.render.post.PostEffect;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;

/**
 * Builds Minecraft {@code PostChain}s from {@link ChainSpec}s and runs them. This is the only class that touches the
 * post-processing API, whose methods differ between 1.20.1 and 1.21.1 ({@code addPass}, {@code setUniform}).
 * Everything here runs on the render thread.
 */
public final class PostChainRunner {
    private static final ResourceLocation EMPTY_CHAIN = Bloomberto.id("shaders/post/empty.json");

    private static final class Realized {
        final PostChain chain;
        final List<PostPass> passes = new ArrayList<>();
        int width;
        int height;

        Realized(PostChain chain, int width, int height) {
            this.chain = chain;
            this.width = width;
            this.height = height;
        }
    }

    private final Map<PostEffect, Realized> chains = new IdentityHashMap<>();
    private final Map<PostEffect, Boolean> broken = new IdentityHashMap<>();

    /** Frees every GPU chain; they are rebuilt on demand. Called after resource reloads. */
    public void invalidate() {
        chains.values().forEach(r -> r.chain.close());
        chains.clear();
        broken.clear();
    }

    /** Runs one effect on the main render target. */
    public void run(PostEffect effect, float partialTick) {
        if (broken.containsKey(effect)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        Realized realized = chains.get(effect);
        try {
            if (realized == null) {
                realized = realize(effect.buildSpec(), mc, main);
                chains.put(effect, realized);
            }
            if (realized.width != main.width || realized.height != main.height) {
                realized.chain.resize(main.width, main.height);
                realized.width = main.width;
                realized.height = main.height;
            }
        } catch (IOException | RuntimeException e) {
            broken.put(effect, Boolean.TRUE);
            Bloomberto.LOGGER.error("Post effect {} could not be built and is disabled", effect.getClass().getSimpleName(), e);
            return;
        }
        Realized r = realized;
        effect.writeUniforms((name, values) -> {
            for (PostPass pass : r.passes) {
                Uniform uniform = pass.getEffect().getUniform(name);
                if (uniform != null) {
                    set(uniform, values);
                }
            }
        });
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.resetTextureMatrix();
        if (Boolean.getBoolean("bloomberto.debugPost")) { Bloomberto.LOGGER.info("[post] run {} passes={} {}x{} intensity={}", effect.getClass().getSimpleName(), r.passes.size(), main.width, main.height, effect.getIntensity()); }
        r.chain.process(partialTick);
        RenderSystem.enableDepthTest();
    }

    private static void set(Uniform uniform, float[] v) {
        switch (v.length) {
            case 1 -> uniform.set(v[0]);
            case 2 -> uniform.set(v[0], v[1]);
            case 3 -> uniform.set(v[0], v[1], v[2]);
            default -> uniform.set(v[0], v[1], v[2], v[3]);
        }
    }

    private static Realized realize(ChainSpec spec, Minecraft mc, RenderTarget main) throws IOException {
        PostChain chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), main, EMPTY_CHAIN);
        Realized realized = new Realized(chain, main.width, main.height);
        try {
            Map<String, RenderTarget> targets = new HashMap<>();
            targets.put(ChainSpec.MAIN, main);
            for (String name : spec.targets()) {
                chain.addTempTarget(name, main.width, main.height);
                targets.put(name, chain.getTempTarget(name));
            }
            for (ChainSpec.Pass p : spec.passes()) {
                RenderTarget in = targets.get(p.in());
                RenderTarget out = targets.get(p.out());
                //? if >=1.21 {
                PostPass pass = chain.addPass(p.program(), in, out, p.isLinearFilter());
                //?} else {
                /*PostPass pass = chain.addPass(p.program(), in, out);
                *///?}
                p.auxTargets().forEach((sampler, target) -> {
                    RenderTarget aux = targets.get(target);
                    pass.addAuxAsset(sampler, aux::getColorTextureId, aux.width, aux.height);
                });
                p.constants().forEach((name, values) -> {
                    Uniform uniform = pass.getEffect().getUniform(name);
                    if (uniform != null) {
                        set(uniform, values);
                    }
                });
                realized.passes.add(pass);
            }
            chain.resize(main.width, main.height);
            return realized;
        } catch (IOException | RuntimeException e) {
            chain.close();
            throw e;
        }
    }
}
