package com.dougg865.bloomberto.api.render;

import com.dougg865.bloomberto.Bloomberto;
import com.dougg865.bloomberto.impl.render.ImageDecoder;
import com.dougg865.bloomberto.impl.render.ImageDownloader;
import com.dougg865.bloomberto.impl.render.TextureCache;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

/**
 * Textures created at runtime from a URL, a file, raw bytes or a {@code NativeImage}. Client only.
 *
 * <pre>{@code
 * TextureHandle avatar = Textures.fromUrl(Bloomberto.id("mymod", "avatar"), "https://example.com/a.png")
 *         .placeholder(Bloomberto.id("mymod", "textures/gui/loading.png"))
 *         .cache(true)
 *         .load();
 * // every frame: draw with avatar.location()
 * }</pre>
 *
 * Supported formats: PNG, JPEG, GIF and BMP.
 */
public final class Textures {
    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();
    private static final ExecutorService WORKERS = Executors.newFixedThreadPool(3, r -> {
        Thread t = new Thread(r, "Bloomberto-textures-" + THREAD_COUNTER.incrementAndGet());
        t.setDaemon(true);
        return t;
    });
    private static final Map<ResourceLocation, TextureHandle> ACTIVE = new ConcurrentHashMap<>();

    private Textures() {
    }

    public static Request fromUrl(ResourceLocation id, String url) {
        return new Request(id, url, null);
    }

    public static Request fromFile(ResourceLocation id, Path file) {
        return new Request(id, null, () -> Files.readAllBytes(file));
    }

    public static Request fromBytes(ResourceLocation id, byte[] data) {
        byte[] copy = data.clone();
        return new Request(id, null, () -> copy);
    }

    /**
     * Registers an already decoded image. Must be called on the render thread; the texture takes ownership of the
     * image. The returned handle is ready immediately.
     */
    public static TextureHandle fromImage(ResourceLocation id, NativeImage image) {
        closeExisting(id);
        TextureHandle handle = new TextureHandle(id, id);
        Minecraft mc = Minecraft.getInstance();
        mc.getTextureManager().register(id, new DynamicTexture(image));
        handle.ready(image.getWidth(), image.getHeight(), () -> mc.getTextureManager().release(id));
        ACTIVE.put(id, handle);
        return handle;
    }

    /** How many textures created through this class are currently alive (loading or ready). */
    public static int activeCount() {
        return ACTIVE.size();
    }

    private static void closeExisting(ResourceLocation id) {
        TextureHandle old = ACTIVE.remove(id);
        if (old != null) {
            old.close();
        }
    }

    /** What to load and how; finish with {@link #load()}. */
    public static final class Request {
        private final ResourceLocation id;
        private final String url;
        private final TextureLoader.Source directSource;
        private ResourceLocation placeholder = MissingTextureAtlasSprite.getLocation();
        private boolean cache = true;
        private Duration cacheTtl = Duration.ofDays(7);
        private int maxBytes = 8 * 1024 * 1024;
        private int maxPixels = 4096 * 4096;
        private Duration timeout = Duration.ofSeconds(20);

        private Request(ResourceLocation id, String url, TextureLoader.Source directSource) {
            this.id = id;
            this.url = url;
            this.directSource = directSource;
        }

        /** What {@link TextureHandle#location()} returns until the texture is ready. */
        public Request placeholder(ResourceLocation placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        /** Keep downloaded files on disk and reuse them (URLs only). Default {@code true}. */
        public Request cache(boolean cache) {
            this.cache = cache;
            return this;
        }

        /** How long a cached file is trusted. Default 7 days. */
        public Request cacheTtl(Duration ttl) {
            this.cacheTtl = ttl;
            return this;
        }

        /** Largest download accepted, in bytes. Default 8 MiB. */
        public Request maxBytes(int maxBytes) {
            this.maxBytes = maxBytes;
            return this;
        }

        /** Largest image accepted, in pixels. Default 4096x4096. */
        public Request maxPixels(int maxPixels) {
            this.maxPixels = maxPixels;
            return this;
        }

        public Request timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        /** Starts loading in the background. Call from the render thread. */
        public TextureHandle load() {
            closeExisting(id);
            TextureLoader.Source source = directSource != null ? directSource : urlSource();
            TextureHandle handle = TextureLoader.load(id, placeholder, source, maxPixels, WORKERS,
                    Minecraft.getInstance(), Textures::upload);
            ACTIVE.put(id, handle);
            return handle;
        }

        private TextureLoader.Source urlSource() {
            return () -> {
                TextureCache diskCache = cache ? new TextureCache(cacheDir()) : null;
                if (diskCache != null) {
                    byte[] cached = diskCache.read(url, cacheTtl);
                    if (cached != null) {
                        try {
                            ImageDecoder.decode(cached, maxPixels);
                            return cached;
                        } catch (IOException corrupt) {
                            diskCache.delete(url);
                        }
                    }
                }
                byte[] data = ImageDownloader.download(url, maxBytes, timeout);
                if (diskCache != null) {
                    diskCache.write(url, data);
                }
                return data;
            };
        }
    }

    private static Path cacheDir() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("bloomberto").resolve("cache").resolve("textures");
    }

    private static Runnable upload(ResourceLocation id, ImageDecoder.Decoded image) {
        NativeImage native0 = new NativeImage(image.width(), image.height(), false);
        for (int y = 0; y < image.height(); y++) {
            for (int x = 0; x < image.width(); x++) {
                native0.setPixelRGBA(x, y, ImageDecoder.argbToAbgr(image.argb()[y * image.width() + x]));
            }
        }
        var textures = Minecraft.getInstance().getTextureManager();
        textures.register(id, new DynamicTexture(native0));
        return () -> textures.release(id);
    }
}
