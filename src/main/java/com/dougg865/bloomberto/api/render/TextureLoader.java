package com.dougg865.bloomberto.api.render;

import com.dougg865.bloomberto.impl.render.ImageDecoder;
import java.io.IOException;
import java.util.concurrent.Executor;
import net.minecraft.resources.ResourceLocation;

/** Loads bytes, decodes them off the main thread and hands the image to an uploader on the main thread. */
final class TextureLoader {
    @FunctionalInterface
    interface Source {
        byte[] read() throws IOException;
    }

    /** Registers the image as a texture; returns what releases it. Always called on the main thread. */
    @FunctionalInterface
    interface Uploader {
        Runnable upload(ResourceLocation id, ImageDecoder.Decoded image);
    }

    private TextureLoader() {
    }

    static TextureHandle load(ResourceLocation id, ResourceLocation placeholder, Source source, int maxPixels,
                              Executor worker, Executor mainThread, Uploader uploader) {
        TextureHandle handle = new TextureHandle(id, placeholder);
        worker.execute(() -> {
            try {
                ImageDecoder.Decoded image = ImageDecoder.decode(source.read(), maxPixels);
                mainThread.execute(() -> {
                    try {
                        Runnable releaser = uploader.upload(id, image);
                        handle.ready(image.width(), image.height(), releaser);
                    } catch (RuntimeException e) {
                        handle.failed(e);
                    }
                });
            } catch (IOException | RuntimeException e) {
                handle.failed(e);
            }
        });
        return handle;
    }
}
