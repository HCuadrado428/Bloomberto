package com.dougg865.bloomberto.impl.render;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

/** An on-disk cache of downloaded files, keyed by the SHA-256 of their URL. */
public final class TextureCache {
    private final Path dir;

    public TextureCache(Path dir) {
        this.dir = dir;
    }

    /** The file name used for a URL: 64 hex characters. */
    public static String keyFor(String url) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(url.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private Path file(String url) {
        return dir.resolve(keyFor(url) + ".bin");
    }

    /** @return the cached bytes, or {@code null} if missing or older than {@code maxAge} */
    public byte[] read(String url, Duration maxAge) {
        Path file = file(url);
        try {
            if (!Files.isRegularFile(file)) {
                return null;
            }
            Instant modified = Files.getLastModifiedTime(file).toInstant();
            if (maxAge != null && modified.plus(maxAge).isBefore(Instant.now())) {
                return null;
            }
            return Files.readAllBytes(file);
        } catch (IOException e) {
            return null;
        }
    }

    public void write(String url, byte[] data) {
        try {
            Files.createDirectories(dir);
            Path tmp = dir.resolve(keyFor(url) + ".tmp");
            Files.write(tmp, data);
            Files.move(tmp, file(url), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // the cache is an optimization; failing to write it is not an error
        }
    }

    public void delete(String url) {
        try {
            Files.deleteIfExists(file(url));
        } catch (IOException ignored) {
            // nothing to do
        }
    }
}
