package com.dougg865.bloomberto.impl.render;

import com.dougg865.bloomberto.Bloomberto;
import com.dougg865.bloomberto.api.render.post.ChainSpec;
import com.dougg865.bloomberto.api.render.post.PostEffect;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

/**
 * Minecraft 1.21.11 version: runs {@link ChainSpec}s with its own passes on the GPU device API, since vanilla post
 * chains there are only loaded from JSON resources. The legacy programs ({@code assets/<ns>/shaders/program/<name>.json}
 * and their fragment shaders) are still the source: {@link LegacyGlsl} translates each fragment shader at runtime and
 * the vertex stage is a full-screen triangle. Everything here runs on the render thread.
 */
public final class PostChainRunner {
    private static final String SCREENQUAD = """
            #version 330

            out vec2 texCoord;

            void main() {
                vec2 uv = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
                gl_Position = vec4(uv * 2.0 - 1.0, 0.0, 1.0);
                texCoord = uv;
            }
            """;
    private static final String BLIT = """
            #version 150
            uniform sampler2D DiffuseSampler;
            in vec2 texCoord;
            out vec4 fragColor;
            void main() {
                fragColor = texture(DiffuseSampler, texCoord);
            }
            """;
    private static final List<String> SIZES = List.of("InSize", "OutSize");
    private static final AtomicInteger GENERATION = new AtomicInteger();

    /** A compiled program: its pipeline, block layout and default uniform values. */
    private record Program(RenderPipeline pipeline, String fragment, LegacyGlsl.Result layout,
                           Map<String, float[]> defaults) {
        void compile() {
            CompiledRenderPipeline compiled = RenderSystem.getDevice().precompilePipeline(pipeline,
                    (id, type) -> type == ShaderType.VERTEX ? SCREENQUAD : fragment);
            if (!compiled.isValid()) {
                throw new IllegalStateException("Shader " + pipeline.getLocation() + " did not compile (see the log above)");
            }
        }
    }

    private static final class Pass {
        final Program program;
        final ChainSpec.Pass spec;
        final Map<String, float[]> values = new HashMap<>();
        final MappableRingBuffer ubo;

        Pass(Program program, ChainSpec.Pass spec) {
            this.program = program;
            this.spec = spec;
            values.putAll(program.defaults);
            values.putAll(spec.constants());
            this.ubo = new MappableRingBuffer(() -> "Bloomberto post " + spec.program(),
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE, program.layout.blockSize());
        }
    }

    private static final class Realized implements AutoCloseable {
        final Map<String, TextureTarget> targets = new LinkedHashMap<>();
        final List<Pass> passes = new ArrayList<>();
        int width;
        int height;

        @Override
        public void close() {
            targets.values().forEach(RenderTarget::destroyBuffers);
            passes.forEach(p -> p.ubo.close());
        }
    }

    private final Map<String, Program> programs = new HashMap<>();
    private final Map<PostEffect, Realized> chains = new IdentityHashMap<>();
    private final Map<PostEffect, Boolean> broken = new IdentityHashMap<>();

    /** Frees every GPU chain and program; they are rebuilt on demand. Called after resource reloads. */
    public void invalidate() {
        chains.values().forEach(Realized::close);
        chains.clear();
        programs.clear();
        broken.clear();
        GENERATION.incrementAndGet();
    }

    /** {@code true} if the effect's chain could not be built (it is skipped until the next resource reload). */
    public boolean isBroken(PostEffect effect) {
        return broken.containsKey(effect);
    }

    /** Runs one effect on the main render target. */
    public void run(PostEffect effect, float partialTick) {
        if (broken.containsKey(effect)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        try {
            Realized r = chains.get(effect);
            if (r == null) {
                r = realize(effect.buildSpec(), main);
                chains.put(effect, r);
            }
            if (r.width != main.width || r.height != main.height) {
                for (TextureTarget t : r.targets.values()) {
                    t.resize(main.width, main.height);
                }
                r.width = main.width;
                r.height = main.height;
            }
            Realized realized = r;
            effect.writeUniforms((name, values) -> {
                for (Pass pass : realized.passes) {
                    if (pass.program.layout.slots().containsKey(name)) {
                        pass.values.put(name, values.clone());
                    }
                }
            });
            if (Boolean.getBoolean("bloomberto.debugPost")) {
                Bloomberto.LOGGER.info("[post] run {} passes={} {}x{} intensity={}", effect.getClass().getSimpleName(),
                        r.passes.size(), main.width, main.height, effect.getIntensity());
            }
            for (Pass pass : r.passes) {
                draw(pass, target(r, main, pass.spec.in()), target(r, main, pass.spec.out()), r, main);
            }
        } catch (IOException | RuntimeException e) {
            broken.put(effect, Boolean.TRUE);
            Realized r = chains.remove(effect);
            if (r != null) {
                r.close();
            }
            Bloomberto.LOGGER.error("Post effect {} could not be built and is disabled", effect.getClass().getSimpleName(), e);
        }
    }

    private static RenderTarget target(Realized r, RenderTarget main, String name) {
        return name.equals(ChainSpec.MAIN) ? main : r.targets.get(name);
    }

    private void draw(Pass pass, RenderTarget in, RenderTarget out, Realized r, RenderTarget main) {
        GpuDevice device = RenderSystem.getDevice();
        pass.program.compile();
        pass.values.put("InSize", new float[] {in.width, in.height});
        pass.values.put("OutSize", new float[] {out.width, out.height});
        CommandEncoder encoder = device.createCommandEncoder();
        LegacyGlsl.Result layout = pass.program.layout;
        try (GpuBuffer.MappedView view = encoder.mapBuffer(pass.ubo.currentBuffer(), false, true)) {
            Std140Builder b = Std140Builder.intoBuffer(view.data());
            float[] packed = LegacyGlsl.pack(layout.slots(), pass.values);
            for (int i = 0; i < packed.length; i += 4) {
                b.putVec4(packed[i], packed[i + 1], packed[i + 2], packed[i + 3]);
            }
        }
        GpuSampler sampler = RenderSystem.getSamplerCache()
                .getClampToEdge(pass.spec.isLinearFilter() ? FilterMode.LINEAR : FilterMode.NEAREST);
        try (RenderPass renderPass = encoder.createRenderPass(() -> "Bloomberto " + pass.spec.program(),
                out.getColorTextureView(), OptionalInt.empty())) {
            renderPass.setPipeline(pass.program.pipeline);
            renderPass.setUniform(LegacyGlsl.BLOCK, pass.ubo.currentBuffer());
            for (String samplerName : layout.samplers()) {
                RenderTarget source;
                if (samplerName.equals("DiffuseSampler")) {
                    source = in;
                } else {
                    String aux = pass.spec.auxTargets().get(samplerName);
                    source = aux == null ? in : target(r, main, aux);
                }
                renderPass.bindTexture(samplerName, source.getColorTextureView(), sampler);
            }
            renderPass.draw(0, 3);
        }
        pass.ubo.rotate();
    }

    private Realized realize(ChainSpec spec, RenderTarget main) throws IOException {
        Realized r = new Realized();
        try {
            r.width = main.width;
            r.height = main.height;
            for (String name : spec.targets()) {
                r.targets.put(name, new TextureTarget("Bloomberto " + name, main.width, main.height, false));
            }
            for (ChainSpec.Pass p : spec.passes()) {
                Program program = programs.get(p.program());
                if (program == null) {
                    program = load(p.program());
                    programs.put(p.program(), program);
                }
                r.passes.add(new Pass(program, p));
            }
            return r;
        } catch (IOException | RuntimeException e) {
            r.close();
            throw e;
        }
    }

    private static Program load(String name) throws IOException {
        Identifier id = Identifier.parse(name);
        String fragmentSource;
        Map<String, float[]> defaults = new HashMap<>();
        if (id.getNamespace().equals("minecraft") && id.getPath().equals("blit")) {
            fragmentSource = BLIT;
        } else {
            JsonObject json;
            try (Reader reader = resource(id.withPath(p -> "shaders/program/" + p + ".json")).openAsReader()) {
                json = JsonParser.parseReader(reader).getAsJsonObject();
            }
            Identifier fragment = Identifier.parse(json.get("fragment").getAsString());
            if (!id.getNamespace().equals("minecraft") && fragment.getNamespace().equals("minecraft")) {
                fragment = Identifier.fromNamespaceAndPath(id.getNamespace(), fragment.getPath());
            }
            try (Reader reader = resource(fragment.withPath(p -> "shaders/program/" + p + ".fsh")).openAsReader()) {
                fragmentSource = readAll(reader);
            }
            if (json.has("uniforms")) {
                for (JsonElement e : json.getAsJsonArray("uniforms")) {
                    JsonObject u = e.getAsJsonObject();
                    if (!u.has("values")) {
                        continue;
                    }
                    JsonArray values = u.getAsJsonArray("values");
                    float[] v = new float[Math.min(16, values.size())];
                    for (int i = 0; i < v.length; i++) {
                        v[i] = values.get(i).getAsFloat();
                    }
                    defaults.put(u.get("name").getAsString(), v);
                }
            }
        }
        LegacyGlsl.Result layout = LegacyGlsl.translate(fragmentSource, SIZES);
        String path = (id.getNamespace() + "/" + id.getPath()).toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9/._-]", "_");
        int generation = GENERATION.get();
        RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
                .withLocation(Bloomberto.id("bloomberto", "post/g" + generation + "/" + path))
                .withVertexShader(Bloomberto.id("bloomberto", "post/g" + generation + "/screenquad"))
                .withFragmentShader(Bloomberto.id("bloomberto", "post/g" + generation + "/" + path))
                .withUniform(LegacyGlsl.BLOCK, UniformType.UNIFORM_BUFFER);
        for (String sampler : layout.samplers()) {
            builder.withSampler(sampler);
        }
        Program program = new Program(builder.build(), layout.source(), layout, defaults);
        program.compile();
        return program;
    }

    private static Resource resource(Identifier id) throws IOException {
        return Minecraft.getInstance().getResourceManager().getResource(id)
                .orElseThrow(() -> new IOException("Missing resource " + id));
    }

    private static String readAll(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[4096];
        int n;
        while ((n = reader.read(buf)) > 0) {
            sb.append(buf, 0, n);
        }
        return sb.toString();
    }
}
