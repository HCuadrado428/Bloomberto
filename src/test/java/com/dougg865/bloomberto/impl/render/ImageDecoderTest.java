package com.dougg865.bloomberto.impl.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ImageDecoderTest {
    private static void chunk(DataOutputStream out, String type, byte[] data) throws IOException {
        out.writeInt(data.length);
        byte[] t = type.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        out.write(t);
        out.write(data);
        CRC32 crc = new CRC32();
        crc.update(t);
        crc.update(data);
        out.writeInt((int) crc.getValue());
    }

    /** A valid PNG header that claims a picture of {@code w x h} pixels, with no real pixel data. */
    private static byte[] pngClaiming(int w, int h) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.write(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'});
        ByteArrayOutputStream ihdr = new ByteArrayOutputStream();
        DataOutputStream d = new DataOutputStream(ihdr);
        d.writeInt(w);
        d.writeInt(h);
        d.write(new byte[] {8, 6, 0, 0, 0}); // 8-bit RGBA
        chunk(out, "IHDR", ihdr.toByteArray());
        chunk(out, "IDAT", new byte[] {0x78, (byte) 0x9c, 0x03, 0x00, 0x00, 0x00, 0x00, 0x01});
        chunk(out, "IEND", new byte[0]);
        return bytes.toByteArray();
    }

    @Test
    void hugeDeclaredSizeIsRejectedBeforeDecoding() throws Exception {
        byte[] bomb = pngClaiming(30000, 30000); // would need 3.6 GB if decoded
        assertTrue(bomb.length < 200);
        IOException e = assertThrows(IOException.class, () -> ImageDecoder.decode(bomb, 4096 * 4096));
        assertTrue(e.getMessage().contains("too large"), e.getMessage());
    }

    @Test
    void normalImageDecodes() throws Exception {
        BufferedImage img = new BufferedImage(3, 2, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(1, 1, 0xFF112233);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        ImageDecoder.Decoded d = ImageDecoder.decode(out.toByteArray(), 100);
        assertEquals(3, d.width());
        assertEquals(2, d.height());
        assertEquals(0xFF112233, d.argb()[1 * 3 + 1]);
    }

    @Test
    void garbageIsAnIOException() {
        assertThrows(IOException.class, () -> ImageDecoder.decode(new byte[] {1, 2, 3, 4}, 100));
        assertThrows(IOException.class, () -> ImageDecoder.decode(new byte[0], 100));
    }
}
