package com.dougg865.bloomberto.api.render;

import com.dougg865.bloomberto.Bloomberto;
import com.dougg865.bloomberto.api.color.Color;
import com.dougg865.bloomberto.impl.render.LegacyGlsl;
import com.dougg865.bloomberto.impl.render.UniformStore;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.BlitRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;

/**
 * A core shader you wrote: the program JSON at {@code assets/minecraft/shaders/core/<namespace>_<name>.json} and the
 * {@code .vsh}/{@code .fsh} it names, written for GLSL 150 with plain {@code uniform} declarations (as before 1.21.11).
 * On Minecraft 1.21.11 Bloomberto translates the stages at load time: every non-sampler uniform moves into one uniform
 * block, so the same shader files work on every version. {@code ModelViewMat} and {@code ProjMat} are set by the draw
 * methods. Uniforms are set with typed methods at any time; they are applied at the next draw.
 * The GPU program is created when first needed and rebuilt after every resource reload.
 *
 * <pre>{@code
 * static final ShaderProgram GLOW = Shaders.create(Bloomberto.id("mymod", "glow"), DefaultVertexFormat.POSITION_COLOR);
 * ...
 * GLOW.setFloat("Intensity", 0.5f);
 * GLOW.drawQuad(guiGraphics, x, y, width, height, 0xFFFFFFFF);
 * }</pre>
 */
public final class ShaderProgram {
    private static final AtomicInteger GENERATION = new AtomicInteger();
    private static final int POOL = 32;

    private final Identifier id;
    private final VertexFormat format;
    private final UniformStore uniforms = new UniformStore();
    private Loaded loaded;
    private boolean failed;
    private final List<TextureTarget> targets = new ArrayList<>();
    private long frame = -1;
    private int used;

    private record Loaded(RenderPipeline pipeline, LegacyGlsl.Program program, Map<String, float[]> defaults,
                          GpuBuffer ubo) {
        CompiledRenderPipeline compile() {
            return RenderSystem.getDevice().precompilePipeline(pipeline,
                    (id, type) -> program.sources().get(type == ShaderType.VERTEX ? 0 : 1));
        }
    }

    ShaderProgram(Identifier id, VertexFormat format) {
        this.id = id;
        this.format = format;
    }

    public Identifier id() {
        return id;
    }

    public VertexFormat format() {
        return format;
    }

    public ShaderProgram setFloat(String name, float v) {
        uniforms.setFloat(name, v);
        return this;
    }

    public ShaderProgram setVec2(String name, float x, float y) {
        uniforms.setVec2(name, x, y);
        return this;
    }

    public ShaderProgram setVec3(String name, float x, float y, float z) {
        uniforms.setVec3(name, x, y, z);
        return this;
    }

    public ShaderProgram setVec4(String name, float x, float y, float z, float w) {
        uniforms.setVec4(name, x, y, z, w);
        return this;
    }

    /** Sets a {@code vec4} from a color (channels as 0-1 floats). */
    public ShaderProgram setColor(String name, Color color) {
        return setVec4(name, color.red() / 255f, color.green() / 255f, color.blue() / 255f, color.alpha() / 255f);
    }

    public ShaderProgram setInt(String name, int v) {
        uniforms.setInt(name, v);
        return this;
    }

    public ShaderProgram setMatrix(String name, Matrix4f matrix) {
        uniforms.setMatrix(name, matrix);
        return this;
    }

    /**
     * Loads and compiles the program if needed. Render thread only. On 1.21.11 nothing is bound globally any more:
     * draw with {@link #drawQuad(GuiGraphics, float, float, float, float, int)}.
     *
     * @return {@code false} if the shader could not be loaded (the reason is in the log)
     */
    public boolean use() {
        Loaded l = instance();
        if (l == null) {
            return false;
        }
        if (!l.compile().isValid()) {
            failed = true;
            close(l);
            loaded = null;
            Bloomberto.LOGGER.error("Shader {} did not compile (see the log above)", id);
            return false;
        }
        return true;
    }

    /**
     * Draws a filled rectangle with this shader into a GUI, in GUI coordinates, keeping the GUI's drawing order. The
     * program must use the {@code POSITION_COLOR} vertex format.
     *
     * @return {@code false} if the shader could not be loaded
     */
    public boolean drawQuad(GuiGraphics graphics, float x, float y, float width, float height, int argb) {
        if (width <= 0 || height <= 0 || !use()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        double scale = mc.getWindow().getGuiScale();
        int pw = Math.max(1, (int) Math.ceil(width * scale));
        int ph = Math.max(1, (int) Math.ceil(height * scale));
        TextureTarget target = target(pw, ph);
        RenderSystem.getDevice().createCommandEncoder().clearColorTexture(target.getColorTexture(), 0);
        Matrix4f projection = new Matrix4f().setOrtho(x, x + width, y, y + height, -1000f, 1000f);
        draw(target, projection, new Matrix4f(), x, y, width, height, argb);
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        graphics.guiRenderState.submitGuiElement(new BlitRenderState(RenderPipelines.GUI_TEXTURED,
                TextureSetup.singleTexture(target.getColorTextureView(),
                        RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST)),
                new Matrix3x2f(graphics.pose()), x0, y0, x0 + (int) Math.ceil(width), y0 + (int) Math.ceil(height),
                0f, 1f, 0f, 1f, 0xFFFFFFFF, null));
        return true;
    }

    /**
     * Draws a filled rectangle with this shader straight into the main framebuffer, with the GUI's projection. On
     * 1.21.11 the GUI is drawn at the end of the frame, so anything the GUI draws covers it: use
     * {@link #drawQuad(GuiGraphics, float, float, float, float, int)} in screens and HUDs.
     *
     * @param pose the transform, in GUI coordinates
     * @return {@code false} if the shader could not be loaded
     */
    public boolean drawQuad(Matrix4f pose, float x, float y, float width, float height, int argb) {
        if (!use()) {
            return false;
        }
        var window = Minecraft.getInstance().getWindow();
        Matrix4f projection = new Matrix4f().setOrtho(0f, window.getGuiScaledWidth(), window.getGuiScaledHeight(), 0f,
                -11000f, 11000f);
        RenderTarget main = RenderSystem.outputColorTextureOverride == null ? Minecraft.getInstance().getMainRenderTarget() : null;
        if (main == null) {
            return false;
        }
        draw(main, projection, pose, x, y, width, height, argb);
        return true;
    }

    private void draw(RenderTarget out, Matrix4f projection, Matrix4f modelView, float x, float y, float w, float h, int argb) {
        Loaded l = loaded;
        Map<String, float[]> values = new HashMap<>(l.defaults);
        uniforms.applyTo(new UniformStore.Target() {
            @Override
            public void setFloats(String name, float... v) {
                values.put(name, v.clone());
            }

            @Override
            public void setInt(String name, int value) {
                values.put(name, new float[] {value});
            }

            @Override
            public void setMatrix(String name, Matrix4f matrix) {
                values.put(name, matrix.get(new float[16]));
            }
        });
        values.put("ProjMat", projection.get(new float[16]));
        values.put("ModelViewMat", modelView.get(new float[16]));
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        try (GpuBuffer.MappedView view = encoder.mapBuffer(l.ubo, false, true)) {
            Std140Builder b = Std140Builder.intoBuffer(view.data());
            float[] packed = LegacyGlsl.pack(l.program.slots(), values);
            for (int i = 0; i < packed.length; i += 4) {
                b.putVec4(packed[i], packed[i + 1], packed[i + 2], packed[i + 3]);
            }
        }
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, format);
        builder.addVertex(x, y + h, 0f).setColor(argb);
        builder.addVertex(x + w, y + h, 0f).setColor(argb);
        builder.addVertex(x + w, y, 0f).setColor(argb);
        builder.addVertex(x, y, 0f).setColor(argb);
        try (MeshData mesh = builder.buildOrThrow()) {
            GpuBuffer vertices = format.uploadImmediateVertexBuffer(mesh.vertexBuffer());
            RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
            int count = mesh.drawState().indexCount();
            try (RenderPass pass = encoder.createRenderPass(() -> "Bloomberto shader " + id, out.getColorTextureView(),
                    OptionalInt.empty())) {
                pass.setPipeline(l.pipeline);
                pass.setUniform(LegacyGlsl.BLOCK, l.ubo);
                pass.setVertexBuffer(0, vertices);
                pass.setIndexBuffer(indices.getBuffer(count), indices.type());
                pass.drawIndexed(0, 0, count, 1);
            }
        }
    }

    /** A texture to draw one GUI quad into; textures are reused from the next frame on. */
    private TextureTarget target(int width, int height) {
        long now = Minecraft.getInstance().getFrameTimeNs();
        if (now != frame) {
            frame = now;
            used = 0;
        }
        int slot = used++ % POOL;
        if (slot == targets.size()) {
            targets.add(new TextureTarget("Bloomberto shader " + id, width, height, false));
        }
        TextureTarget target = targets.get(slot);
        if (target.width != width || target.height != height) {
            target.resize(width, height);
        }
        return target;
    }

    /** The loaded program, creating it if needed; {@code null} if it failed to load. */
    Loaded instance() {
        com.dougg865.bloomberto.impl.render.ClientReloads.ensureRegistered();
        if (loaded == null && !failed) {
            try {
                loaded = load();
            } catch (IOException | RuntimeException e) {
                failed = true;
                Bloomberto.LOGGER.error("Could not load shader {}", id, e);
            }
        }
        return loaded;
    }

    private Loaded load() throws IOException {
        Identifier jsonId = Identifier.withDefaultNamespace("shaders/core/" + programName() + ".json");
        JsonObject json;
        try (Reader reader = resource(jsonId).openAsReader()) {
            json = JsonParser.parseReader(reader).getAsJsonObject();
        }
        String vertex = read(Identifier.parse(json.get("vertex").getAsString()), ".vsh");
        String fragment = read(Identifier.parse(json.get("fragment").getAsString()), ".fsh");
        LegacyGlsl.Program program = LegacyGlsl.translateAll(List.of(vertex, fragment), List.of("ModelViewMat", "ProjMat"));
        Map<String, float[]> defaults = new HashMap<>();
        if (json.has("uniforms")) {
            for (JsonElement e : json.getAsJsonArray("uniforms")) {
                JsonObject u = e.getAsJsonObject();
                if (u.has("values")) {
                    JsonArray values = u.getAsJsonArray("values");
                    float[] v = new float[Math.min(16, values.size())];
                    for (int i = 0; i < v.length; i++) {
                        v[i] = values.get(i).getAsFloat();
                    }
                    defaults.put(u.get("name").getAsString(), v);
                }
            }
        }
        String base = "shader/g" + GENERATION.get() + "/" + id.getNamespace() + "/" + id.getPath();
        RenderPipeline.Builder builder = RenderPipeline.builder()
                .withLocation(Bloomberto.id("bloomberto", base))
                .withVertexShader(Bloomberto.id("bloomberto", base + "/vertex"))
                .withFragmentShader(Bloomberto.id("bloomberto", base + "/fragment"))
                .withUniform(LegacyGlsl.BLOCK, UniformType.UNIFORM_BUFFER)
                .withVertexFormat(format, VertexFormat.Mode.QUADS)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withCull(false);
        BlendFunction blend = blend(json);
        if (blend != null) {
            builder.withBlend(blend);
        }
        for (String sampler : program.samplers()) {
            builder.withSampler(sampler);
        }
        GpuBuffer ubo = RenderSystem.getDevice().createBuffer(() -> "Bloomberto shader " + id,
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE, program.blockSize());
        return new Loaded(builder.build(), program, defaults, ubo);
    }

    private static BlendFunction blend(JsonObject json) {
        if (!json.has("blend")) {
            return null;
        }
        JsonObject b = json.getAsJsonObject("blend");
        String src = b.has("srcrgb") ? b.get("srcrgb").getAsString().toLowerCase(java.util.Locale.ROOT) : "one";
        String dst = b.has("dstrgb") ? b.get("dstrgb").getAsString().toLowerCase(java.util.Locale.ROOT) : "zero";
        if (dst.equals("zero")) {
            return null;
        }
        if (src.equals("one") && dst.equals("one")) {
            return BlendFunction.ADDITIVE;
        }
        if (src.equals("srcalpha") && dst.equals("one")) {
            return BlendFunction.LIGHTNING;
        }
        return BlendFunction.TRANSLUCENT;
    }

    private static String read(Identifier shader, String extension) throws IOException {
        try (Reader reader = resource(shader.withPath(p -> "shaders/core/" + p + extension)).openAsReader()) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int n;
            while ((n = reader.read(buf)) > 0) {
                sb.append(buf, 0, n);
            }
            return sb.toString();
        }
    }

    private static Resource resource(Identifier id) throws IOException {
        return Minecraft.getInstance().getResourceManager().getResource(id)
                .orElseThrow(() -> new IOException("Missing resource " + id));
    }

    /**
     * The program JSON of {@code mymod:glow} is the file {@code assets/minecraft/shaders/core/mymod_glow.json}. Its
     * {@code vertex} and {@code fragment} entries may point to your own namespace ({@code "vertex": "mymod:glow"} is
     * {@code assets/mymod/shaders/core/glow.vsh}).
     */
    String programName() {
        return id.getNamespace() + "_" + id.getPath().replace('/', '_');
    }

    private void close(Loaded l) {
        l.ubo.close();
    }

    /** Drops the GPU program so it is rebuilt from the (possibly changed) resources on next use. */
    void invalidate() {
        if (loaded != null) {
            close(loaded);
            loaded = null;
        }
        targets.forEach(RenderTarget::destroyBuffers);
        targets.clear();
        failed = false;
        GENERATION.incrementAndGet();
    }
}
