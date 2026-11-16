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

    /**
     * Deletes cached files older than {@code maxAge}, then the oldest ones until the cache is at most {@code maxBytes}.
     * Leftover {@code .tmp} files of interrupted writes are removed too.
     *
     * @return how many files were deleted
     */
    public int prune(Duration maxAge, long maxBytes) {
        if (!Files.isDirectory(dir)) {
            return 0;
        }
        record Item(Path path, long size, Instant modified) {
        }
        java.util.List<Item> items = new java.util.ArrayList<>();
        int deleted = 0;
        try (java.util.stream.Stream<Path> files = Files.list(dir)) {
            for (Path p : (Iterable<Path>) files::iterator) {
                String name = p.getFileName().toString();
                if (!name.endsWith(".bin") && !name.endsWith(".tmp")) {
                    continue; // only touch files this cache wrote
                }
                try {
                    Instant modified = Files.getLastModifiedTime(p).toInstant();
                    if (name.endsWith(".tmp") || modified.plus(maxAge).isBefore(Instant.now())) {
                        Files.deleteIfExists(p);
                        deleted++;
                    } else {
                        items.add(new Item(p, Files.size(p), modified));
                    }
                } catch (IOException ignored) {
                    // a file that vanished or cannot be read is simply skipped
                }
            }
        } catch (IOException e) {
            return deleted;
        }
        long total = items.stream().mapToLong(Item::size).sum();
        items.sort(java.util.Comparator.comparing(Item::modified));
        for (Item item : items) {
            if (total <= maxBytes) {
                break;
            }
            try {
                Files.deleteIfExists(item.path());
                total -= item.size();
                deleted++;
            } catch (IOException ignored) {
                // try the next one
            }
        }
        return deleted;
    }

    public void delete(String url) {
        try {
            Files.deleteIfExists(file(url));
        } catch (IOException ignored) {
            // nothing to do
        }
    }
}
