package com.dougg865.bloomberto.api.render;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dougg865.bloomberto.Bloomberto;
import com.dougg865.bloomberto.impl.render.ImageDecoder;
import com.dougg865.bloomberto.impl.render.ImageDownloader;
import com.dougg865.bloomberto.impl.render.TextureCache;
import com.sun.net.httpserver.HttpServer;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TextureLogicTest {
    private HttpServer server;
    private final AtomicInteger hits = new AtomicInteger();
    private static final ResourceLocation ID = Bloomberto.id("test", "tex");
    private static final ResourceLocation PLACEHOLDER = Bloomberto.id("test", "loading");

    private static byte[] image(String format, int w, int h, int argb) throws IOException {
        BufferedImage img = new BufferedImage(w, h, format.equals("png") ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, argb);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, format, out);
        return out.toByteArray();
    }

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ok.png", ex -> {
            hits.incrementAndGet();
            byte[] body = image("png", 4, 2, 0x80FF0000);
            ex.sendResponseHeaders(200, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        server.createContext("/missing", ex -> {
            ex.sendResponseHeaders(404, -1);
            ex.close();
        });
        server.createContext("/big", ex -> {
            byte[] body = new byte[5000];
            ex.sendResponseHeaders(200, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private String url(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    @Test
    void downloadsAndEnforcesLimits() throws IOException {
        byte[] data = ImageDownloader.download(url("/ok.png"), 100_000, Duration.ofSeconds(5));
        assertTrue(data.length > 0);
        assertThrows(IOException.class, () -> ImageDownloader.download(url("/missing"), 1000, Duration.ofSeconds(5)));
        assertThrows(IOException.class, () -> ImageDownloader.download(url("/big"), 1000, Duration.ofSeconds(5)));
        assertThrows(IOException.class, () -> ImageDownloader.download("file:///etc/passwd", 1000, Duration.ofSeconds(5)));
        assertThrows(IOException.class, () -> ImageDownloader.download("not a url", 1000, Duration.ofSeconds(5)));
    }

    @Test
    void decodesPngAndJpeg() throws IOException {
        ImageDecoder.Decoded png = ImageDecoder.decode(image("png", 3, 2, 0x80FF0000), 1000);
        assertEquals(3, png.width());
        assertEquals(2, png.height());
        assertEquals(0x80FF0000, png.argb()[0]);
        ImageDecoder.Decoded jpg = ImageDecoder.decode(image("jpg", 8, 8, 0x00FF00), 1000);
        assertEquals(8, jpg.width());
        assertEquals(64, jpg.argb().length);
    }

    @Test
    void rejectsGarbageAndHugeImages() throws IOException {
        assertThrows(IOException.class, () -> ImageDecoder.decode(new byte[] {1, 2, 3, 4}, 1000));
        assertThrows(IOException.class, () -> ImageDecoder.decode(image("png", 100, 100, 0xFFFFFFFF), 50));
    }

    @Test
    void convertsArgbToAbgr() {
        assertEquals(0xFF0000FF, ImageDecoder.argbToAbgr(0xFFFF0000)); // opaque red -> ABGR red
        assertEquals(0x80FF0000, ImageDecoder.argbToAbgr(0x800000FF)); // alpha and green are kept
        assertEquals(0x1200FF00, ImageDecoder.argbToAbgr(0x1200FF00));
    }

    @Test
    void cacheRoundTripsAndExpires(@TempDir Path dir) throws IOException {
        TextureCache cache = new TextureCache(dir.resolve("sub"));
        assertEquals(64, TextureCache.keyFor("https://x/y.png").length());
        assertEquals(TextureCache.keyFor("a"), TextureCache.keyFor("a"));
        assertNull(cache.read("a", Duration.ofDays(1)));
        cache.write("a", new byte[] {1, 2, 3});
        assertArrayEquals(new byte[] {1, 2, 3}, cache.read("a", Duration.ofDays(1)));
        assertNull(cache.read("a", Duration.ofMillis(-1)), "older than the ttl");
        cache.delete("a");
        assertNull(cache.read("a", Duration.ofDays(1)));
    }

    private static final TextureLoader.Uploader FAKE = (id, image) -> () -> { };

    @Test
    void loaderMovesThroughStatesAndUsesPlaceholder() throws IOException {
        byte[] png = image("png", 5, 3, 0xFF00FF00);
        java.util.List<Runnable> mainQueue = new java.util.ArrayList<>();
        AtomicInteger released = new AtomicInteger();
        TextureHandle handle = TextureLoader.load(ID, PLACEHOLDER, () -> png, 1000, Runnable::run, mainQueue::add,
                (id, img) -> released::incrementAndGet);
        assertEquals(TextureHandle.State.LOADING, handle.state());
        assertEquals(PLACEHOLDER, handle.location());
        assertEquals(1, mainQueue.size(), "the upload is deferred to the main thread");
        mainQueue.forEach(Runnable::run);
        assertTrue(handle.isReady());
        assertEquals(ID, handle.location());
        assertEquals(5, handle.width());
        assertEquals(3, handle.height());
        assertTrue(handle.whenDone().isDone());
        handle.close();
        assertEquals(TextureHandle.State.CLOSED, handle.state());
        assertEquals(1, released.get());
        handle.close();
        assertEquals(1, released.get(), "closing twice releases once");
    }

    @Test
    void loaderReportsFailures() {
        TextureHandle handle = TextureLoader.load(ID, PLACEHOLDER, () -> {
            throw new IOException("boom");
        }, 1000, Runnable::run, Runnable::run, FAKE);
        assertEquals(TextureHandle.State.FAILED, handle.state());
        assertEquals("boom", handle.error().getMessage());
        assertEquals(PLACEHOLDER, handle.location());
        assertTrue(handle.whenDone().isDone());
        TextureHandle bad = TextureLoader.load(ID, PLACEHOLDER, () -> new byte[] {9, 9}, 1000, Runnable::run, Runnable::run, FAKE);
        assertEquals(TextureHandle.State.FAILED, bad.state());
    }

    @Test
    void closingBeforeTheUploadReleasesTheTexture() throws IOException {
        byte[] png = image("png", 2, 2, 0xFFFFFFFF);
        java.util.List<Runnable> mainQueue = new java.util.ArrayList<>();
        AtomicInteger released = new AtomicInteger();
        TextureHandle handle = TextureLoader.load(ID, PLACEHOLDER, () -> png, 1000, Runnable::run, mainQueue::add,
                (id, img) -> released::incrementAndGet);
        handle.close();
        mainQueue.forEach(Runnable::run);
        assertEquals(TextureHandle.State.CLOSED, handle.state());
        assertEquals(1, released.get(), "a texture uploaded after close() is freed right away");
    }

    @Test
    void fullUrlPipelineWithCache(@TempDir Path dir) throws Exception {
        TextureCache cache = new TextureCache(dir);
        String url = url("/ok.png");
        TextureLoader.Source source = () -> {
            byte[] cached = cache.read(url, Duration.ofDays(1));
            if (cached != null) {
                return cached;
            }
            byte[] data = ImageDownloader.download(url, 100_000, Duration.ofSeconds(5));
            cache.write(url, data);
            return data;
        };
        TextureHandle a = TextureLoader.load(ID, PLACEHOLDER, source, 1000, Runnable::run, Runnable::run, FAKE);
        TextureHandle b = TextureLoader.load(ID, PLACEHOLDER, source, 1000, Runnable::run, Runnable::run, FAKE);
        assertTrue(a.isReady() && b.isReady());
        assertEquals(1, hits.get(), "the second load comes from the disk cache");
        assertNotNull(Files.list(dir).findFirst().orElse(null));
        assertFalse(a.isReady() && a.error() != null);
    }
}
