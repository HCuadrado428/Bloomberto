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

    /** @throws IOException if the bytes are not a supported image */
    public static Decoded decode(byte[] data, int maxPixels) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
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
