package com.dougg865.bloomberto.api.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;

/**
 * A framebuffer of your own to render into, then sample as a texture (minimaps, portals, previews, custom post effects).
 * Client only, render thread only. Close it when you are done.
 *
 * <pre>{@code
 * OffscreenTarget target = new OffscreenTarget(256, 256);
 * target.bind();            // everything drawn now goes into the target
 * // ... draw ...
 * target.unbind();          // back to the screen
 * int textureId = target.colorTextureId();
 * }</pre>
 */
public final class OffscreenTarget implements AutoCloseable {
    private final RenderTarget target;

    public OffscreenTarget(int width, int height) {
        RenderSystem.assertOnRenderThread();
        this.target = new TextureTarget(width, height, true, Minecraft.ON_OSX);
        this.target.setClearColor(0f, 0f, 0f, 0f);
        this.target.clear(Minecraft.ON_OSX);
    }

    public int width() {
        return target.width;
    }

    public int height() {
        return target.height;
    }

    /** Directs drawing into this target (and sets the viewport to its size). */
    public void bind() {
        target.bindWrite(true);
    }

    /** Directs drawing back to the screen. */
    public void unbind() {
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
    }

    /** Empties the target to transparent black. */
    public void clear() {
        target.clear(Minecraft.ON_OSX);
    }

    public void resize(int width, int height) {
        target.resize(width, height, Minecraft.ON_OSX);
    }

    /** The OpenGL id of the color texture, for sampling in a shader. */
    public int colorTextureId() {
        return target.getColorTextureId();
    }

    /** The underlying vanilla target, for APIs that take one. */
    public RenderTarget handle() {
        return target;
    }

    @Override
    public void close() {
        target.destroyBuffers();
    }
}
