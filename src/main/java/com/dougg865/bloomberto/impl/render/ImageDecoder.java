package com.dougg865.bloomberto.impl.render;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

/** Decodes PNG, JPEG, GIF and BMP into plain ARGB pixels. Pure Java, safe to call from any thread. */
public final class ImageDecoder {
    /** A decoded image; {@code argb} has {@code width * height} entries in row-major order. */
    public record Decoded(int width, int height, int[] argb) {
    }

    private ImageDecoder() {
    }

    /**
     * The size is checked from the file header before any pixel is decoded, so a tiny file that claims a huge picture (a
     * "decompression bomb") is rejected without allocating it.
     *
     * @throws IOException if the bytes are not a supported image, or the image is larger than {@code maxPixels}
     */
    public static Decoded decode(byte[] data, int maxPixels) throws IOException {
        BufferedImage image;
        try (javax.imageio.stream.ImageInputStream in = new javax.imageio.stream.MemoryCacheImageInputStream(
                new ByteArrayInputStream(data))) {
            java.util.Iterator<javax.imageio.ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                throw new IOException("Unsupported or corrupt image");
            }
            javax.imageio.ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true);
                long declared = (long) reader.getWidth(0) * reader.getHeight(0);
                if (declared <= 0 || declared > maxPixels) {
                    throw new IOException("Image too large: " + reader.getWidth(0) + "x" + reader.getHeight(0));
                }
                image = reader.read(0);
            } catch (RuntimeException e) {
                throw new IOException("Unsupported or corrupt image: " + e.getMessage(), e);
            } finally {
                reader.dispose();
            }
        }
        if (image == null) {
            throw new IOException("Unsupported or corrupt image");
        }
        int w = image.getWidth();
        int h = image.getHeight();
        if ((long) w * h > maxPixels) {
            throw new IOException("Image too large: " + w + "x" + h);
        }
        int[] pixels = image.getRGB(0, 0, w, h, null, 0, w);
        return new Decoded(w, h, pixels);
    }

    /** Converts {@code 0xAARRGGBB} to the {@code 0xAABBGGRR} layout NativeImage uses. */
    public static int argbToAbgr(int argb) {
        return (argb & 0xFF00FF00) | (argb >> 16 & 0xFF) | (argb & 0xFF) << 16;
    }
}
