package com.dougg865.bloomberto.impl.render;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Downloads a file over http(s) with a size limit. Blocking: call it from a worker thread. */
public final class ImageDownloader {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER) // redirects are followed by hand so every hop is checked
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /** Closes streams of downloads that exceed their time budget (three times the request timeout for the whole body). */
    private static final java.util.concurrent.ScheduledExecutorService WATCHDOG =
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "Bloomberto-download-watchdog");
                t.setDaemon(true);
                return t;
            });

    private ImageDownloader() {
    }

    /**
     * @param maxBytes the largest body accepted
     * @throws IOException on network errors, non-2xx status or a body larger than {@code maxBytes}
     */
    public static byte[] download(String url, int maxBytes, Duration timeout) throws IOException {
        return download(url, maxBytes, timeout, true);
    }

    private static final int MAX_REDIRECTS = 5;

    /**
     * @param allowLocal {@code false} refuses hosts that resolve to this computer or the local network (loopback, private,
     *                   link-local, carrier-grade NAT and unique-local IPv6 addresses), also after redirects. Use it for URLs
     *                   that come from players or servers, so they cannot make the game probe the user's network.
     */
    public static byte[] download(String url, int maxBytes, Duration timeout, boolean allowLocal) throws IOException {
        String current = url;
        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            Result r = fetch(current, maxBytes, timeout, allowLocal);
            if (r.data != null) {
                return r.data;
            }
            current = r.redirect;
        }
        throw new IOException("Too many redirects for " + url);
    }

    private record Result(byte[] data, String redirect) {
    }

    /** {@code true} if the address belongs to this computer or a private network. */
    public static boolean isLocal(InetAddress a) {
        if (a.isLoopbackAddress() || a.isSiteLocalAddress() || a.isLinkLocalAddress() || a.isAnyLocalAddress()
                || a.isMulticastAddress()) {
            return true;
        }
        byte[] b = a.getAddress();
        if (b.length == 4) {
            int first = b[0] & 0xFF;
            int second = b[1] & 0xFF;
            return first == 0 || (first == 100 && second >= 64 && second < 128) // carrier-grade NAT
                    || (first == 192 && second == 0 && (b[2] & 0xFF) == 0)        // IETF protocol assignments
                    || first >= 224;                                              // multicast and reserved
        }
        return (b[0] & 0xFE) == 0xFC; // IPv6 unique local fc00::/7
    }

    private static void checkHost(URI uri, boolean allowLocal) throws IOException {
        if (allowLocal) {
            return;
        }
        String host = uri.getHost();
        if (host == null) {
            throw new IOException("URL has no host: " + uri);
        }
        for (InetAddress a : InetAddress.getAllByName(host)) {
            if (isLocal(a)) {
                throw new IOException("Refusing to download from a local or private address: " + host);
            }
        }
    }

    private static Result fetch(String url, int maxBytes, Duration timeout, boolean allowLocal) throws IOException {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid URL: " + url, e);
        }
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IOException("Only http and https URLs are supported: " + url);
        }
        checkHost(uri, allowLocal);
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(timeout).GET().build();
        try {
            HttpResponse<InputStream> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
            int status = response.statusCode();
            if (status / 100 == 3) {
                response.body().close();
                String location = response.headers().firstValue("Location")
                        .orElseThrow(() -> new IOException("Redirect without a Location header from " + url));
                URI next = uri.resolve(location);
                if ("https".equalsIgnoreCase(uri.getScheme()) && "http".equalsIgnoreCase(next.getScheme())) {
                    throw new IOException("Refusing a redirect from https to http: " + next);
                }
                return new Result(null, next.toString());
            }
            InputStream body = response.body();
            java.util.concurrent.ScheduledFuture<?> watchdog = WATCHDOG.schedule(() -> {
                try {
                    body.close(); // unblocks a read() that is stuck on a stalled connection
                } catch (IOException ignored) {
                    // already closed
                }
            }, Math.max(1L, timeout.toMillis() * 3), java.util.concurrent.TimeUnit.MILLISECONDS);
            try (InputStream in = body) {
                if (response.statusCode() / 100 != 2) {
                    throw new IOException("HTTP " + response.statusCode() + " for " + url);
                }
                long declared = response.headers().firstValueAsLong("Content-Length").orElse(-1);
                if (declared > maxBytes) {
                    throw new IOException("File too large (" + declared + " bytes, limit " + maxBytes + ")");
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int total = 0;
                int n;
                while ((n = in.read(buffer)) > 0) {
                    total += n;
                    if (total > maxBytes) {
                        throw new IOException("File too large (limit " + maxBytes + " bytes)");
                    }
                    out.write(buffer, 0, n);
                }
                return new Result(out.toByteArray(), null);
            } finally {
                watchdog.cancel(false);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while downloading " + url, e);
        }
    }
}
