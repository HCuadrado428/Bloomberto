package com.dougg865.bloomberto.impl.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DownloadSafetyTest {
    @Test
    void localAndPrivateAddressesAreRecognized() throws Exception {
        for (String a : new String[] {"127.0.0.1", "10.1.2.3", "172.16.0.1", "192.168.1.1", "169.254.169.254", "100.64.0.1",
                "0.0.0.0", "::1", "fd00::1", "fe80::1", "224.0.0.1"}) {
            assertTrue(ImageDownloader.isLocal(InetAddress.getByName(a)), a);
        }
        for (String a : new String[] {"8.8.8.8", "1.1.1.1", "93.184.216.34", "2606:4700:4700::1111"}) {
            assertFalse(ImageDownloader.isLocal(InetAddress.getByName(a)), a);
        }
    }

    @Test
    void localDownloadsCanBeRefused() {
        IOException e = assertThrows(IOException.class,
                () -> ImageDownloader.download("http://127.0.0.1:9/x.png", 1000, Duration.ofSeconds(2), false));
        assertTrue(e.getMessage().contains("local or private"), e.getMessage());
    }

    @Test
    void cacheIsPrunedByAgeAndSize(@TempDir Path dir) throws Exception {
        TextureCache cache = new TextureCache(dir);
        cache.write("old", new byte[10]);
        cache.write("a", new byte[100]);
        cache.write("b", new byte[100]);
        Files.writeString(dir.resolve("unrelated.txt"), "keep me");
        Files.writeString(dir.resolve("leftover.tmp"), "x");
        Path old = dir.resolve(TextureCache.keyFor("old") + ".bin");
        Files.setLastModifiedTime(old, FileTime.from(Instant.now().minus(Duration.ofDays(60))));
        Path a = dir.resolve(TextureCache.keyFor("a") + ".bin");
        Files.setLastModifiedTime(a, FileTime.from(Instant.now().minus(Duration.ofDays(1))));
        int deleted = cache.prune(Duration.ofDays(30), 150);
        assertTrue(deleted >= 3, "deleted " + deleted);
        assertFalse(Files.exists(old));
        assertFalse(Files.exists(a)); // oldest one goes first when over the size limit
        assertTrue(Files.exists(dir.resolve(TextureCache.keyFor("b") + ".bin")));
        assertTrue(Files.exists(dir.resolve("unrelated.txt")));
        assertFalse(Files.exists(dir.resolve("leftover.tmp")));
    }
}
