package com.dougg865.bloomberto.api.render;

import java.util.concurrent.CompletableFuture;
import net.minecraft.resources.ResourceLocation;

/**
 * A texture that is loaded in the background. Draw with {@link #location()}: it is the placeholder (or the
 * "missing texture") while loading or after a failure, and the real texture once it is ready.
 * Call {@link #close()} when the texture is no longer needed to free its GPU memory.
 */
public final class TextureHandle implements AutoCloseable {
    public enum State { LOADING, READY, FAILED, CLOSED }

    private final ResourceLocation id;
    private final ResourceLocation placeholder;
    private final CompletableFuture<TextureHandle> done = new CompletableFuture<>();
    private volatile State state = State.LOADING;
    private volatile Throwable error;
    private volatile int width;
    private volatile int height;
    private volatile Runnable releaser = () -> { };

    TextureHandle(ResourceLocation id, ResourceLocation placeholder) {
        this.id = id;
        this.placeholder = placeholder;
    }

    /** The texture to draw right now. */
    public ResourceLocation location() {
        return state == State.READY ? id : placeholder;
    }

    /** The id the real texture is registered under. */
    public ResourceLocation id() {
        return id;
    }

    public State state() {
        return state;
    }

    public boolean isReady() {
        return state == State.READY;
    }

    /** Why loading failed; {@code null} unless {@link #state()} is {@link State#FAILED}. */
    public Throwable error() {
        return error;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /** Completes (never exceptionally) when loading has finished, successfully or not. */
    public CompletableFuture<TextureHandle> whenDone() {
        return done;
    }

    synchronized void ready(int width, int height, Runnable releaser) {
        if (state == State.CLOSED) {
            releaser.run();
            return;
        }
        this.width = width;
        this.height = height;
        this.releaser = releaser;
        this.state = State.READY;
        done.complete(this);
    }

    synchronized void failed(Throwable error) {
        if (state == State.CLOSED) {
            return;
        }
        this.error = error;
        this.state = State.FAILED;
        done.complete(this);
    }

    @Override
    public synchronized void close() {
        if (state == State.CLOSED) {
            return;
        }
        state = State.CLOSED;
        releaser.run();
        done.complete(this);
    }
}
