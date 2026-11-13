package com.dougg865.bloomberto.api.render;

import com.dougg865.bloomberto.Bloomberto;
import com.dougg865.bloomberto.api.color.Color;
import com.dougg865.bloomberto.impl.render.UniformStore;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * A core shader you wrote ({@code assets/<ns>/shaders/core/<name>.json} plus {@code .vsh}/{@code .fsh}).
 * Uniforms are set with typed methods at any time; they are applied the next time the program is {@link #use() used}.
 * The GPU program is created when first needed and rebuilt after every resource reload.
 *
 * <pre>{@code
 * static final ShaderProgram GLOW = Shaders.create(Bloomberto.id("mymod", "glow"), DefaultVertexFormat.POSITION_COLOR);
 * ...
 * GLOW.setFloat("Intensity", 0.5f);
 * if (GLOW.use()) { ... draw with the buffer builder ... }
 * }</pre>
 */
public final class ShaderProgram {
    private final ResourceLocation id;
    private final VertexFormat format;
    private final UniformStore uniforms = new UniformStore();
    private ShaderInstance instance;
    private boolean failed;

    ShaderProgram(ResourceLocation id, VertexFormat format) {
        this.id = id;
        this.format = format;
    }

    public ResourceLocation id() {
        return id;
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
     * Makes this the active shader for the next draw call and uploads the uniforms. Render thread only.
     *
     * @return {@code false} if the shader could not be loaded (the reason is in the log); don't draw then
     */
    public boolean use() {
        ShaderInstance shader = instance();
        if (shader == null) {
            return false;
        }
        uniforms.applyTo(new UniformStore.Target() {
            @Override
            public void setFloats(String name, float... v) {
                var u = shader.safeGetUniform(name);
                switch (v.length) {
                    case 1 -> u.set(v[0]);
                    case 2 -> u.set(v[0], v[1]);
                    case 3 -> u.set(v[0], v[1], v[2]);
                    default -> u.set(v[0], v[1], v[2], v[3]);
                }
            }

            @Override
            public void setInt(String name, int value) {
                shader.safeGetUniform(name).set(value);
            }

            @Override
            public void setMatrix(String name, Matrix4f matrix) {
                shader.safeGetUniform(name).set(matrix);
            }
        });
        RenderSystem.setShader(() -> shader);
        return true;
    }

    /** The loaded GPU program, creating it if needed; {@code null} if it failed to load. */
    ShaderInstance instance() {
        if (instance == null && !failed) {
            try {
                instance = new ShaderInstance(Minecraft.getInstance().getResourceManager(), id.toString(), format);
            } catch (IOException | RuntimeException e) {
                failed = true;
                Bloomberto.LOGGER.error("Could not load shader {}", id, e);
            }
        }
        return instance;
    }

    /** Drops the GPU program so it is rebuilt from the (possibly changed) resources on next use. */
    void invalidate() {
        if (instance != null) {
            instance.close();
            instance = null;
        }
        failed = false;
    }
}
