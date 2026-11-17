package com.dougg865.bloomberto.api.render;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

/**
 * A framebuffer of your own to render into, then sample as a texture (minimaps, portals, previews, custom post effects).
 * Client only, render thread only. Close it when you are done.
 *
 * <pre>{@code
 * OffscreenTarget target = new OffscreenTarget(256, 256);
 * target.bind();            // render types drawn now go into the target
 * // ... draw ...
 * target.unbind();          // back to the screen
 * GpuTextureView view = target.colorTextureView();
 * }</pre>
 *
 * On Minecraft 1.21.11 {@link #bind()} redirects what is drawn through render types ({@code MultiBufferSource}); render
 * passes you create yourself take {@link #colorTextureView()} as their output directly.
 */
public final class OffscreenTarget implements AutoCloseable {
    private final RenderTarget target;
    private GpuTextureView previousColor;
    private GpuTextureView previousDepth;
    private boolean bound;

    public OffscreenTarget(int width, int height) {
        RenderSystem.assertOnRenderThread();
        this.target = new TextureTarget("Bloomberto offscreen", width, height, true);
        clear();
    }

    public int width() {
        return target.width;
    }

    public int height() {
        return target.height;
    }

    /** Directs render-type drawing into this target. */
    public void bind() {
        if (!bound) {
            previousColor = RenderSystem.outputColorTextureOverride;
            previousDepth = RenderSystem.outputDepthTextureOverride;
            bound = true;
        }
        RenderSystem.outputColorTextureOverride = target.getColorTextureView();
        RenderSystem.outputDepthTextureOverride = target.getDepthTextureView();
    }

    /** Directs drawing back to where it went before {@link #bind()}. */
    public void unbind() {
        if (bound) {
            RenderSystem.outputColorTextureOverride = previousColor;
            RenderSystem.outputDepthTextureOverride = previousDepth;
            bound = false;
        }
    }

    /** Empties the target to transparent black. */
    public void clear() {
        GpuTexture color = target.getColorTexture();
        GpuTexture depth = target.getDepthTexture();
        if (color != null && depth != null) {
            RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(color, 0, depth, 1.0);
        }
    }

    public void resize(int width, int height) {
        target.resize(width, height);
        clear();
    }

    /** The OpenGL id of the color texture, for sampling in a raw OpenGL shader. */
    public int colorTextureId() {
        return target.getColorTexture() instanceof GlTexture gl ? gl.glId() : 0;
    }

    /** The color texture, for binding to a render pass ({@code renderPass.bindTexture(...)}). */
    public GpuTextureView colorTextureView() {
        return target.getColorTextureView();
    }

    /** The underlying vanilla target, for APIs that take one. */
    public RenderTarget handle() {
        return target;
    }

    @Override
    public void close() {
        unbind();
        target.destroyBuffers();
    }
}
