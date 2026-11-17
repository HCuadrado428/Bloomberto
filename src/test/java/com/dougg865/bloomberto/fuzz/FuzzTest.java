package com.dougg865.bloomberto.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.dougg865.bloomberto.api.animation.model.AnimationParser;
import com.dougg865.bloomberto.api.animation.model.BbModelParser;
import com.dougg865.bloomberto.api.animation.model.GeoParser;
import com.dougg865.bloomberto.api.animation.model.ModelGeometry;
import com.dougg865.bloomberto.api.animation.model.ParseException;
import com.dougg865.bloomberto.api.animation.model.Pose;
import com.dougg865.bloomberto.api.animation.molang.Molang;
import com.dougg865.bloomberto.api.animation.molang.MolangContext;
import com.dougg865.bloomberto.api.animation.molang.MolangException;
import com.dougg865.bloomberto.impl.config.Toml;
import com.dougg865.bloomberto.impl.render.ImageDecoder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Throws thousands of random and corrupted inputs at everything that reads data from files, resource packs or the network.
 * The rule: bad input may only ever produce the documented checked exception, never a crash (RuntimeException or Error), a
 * hang or a silently broken result. The seed is fixed so a failure can be reproduced; change it to explore more cases.
 */
class FuzzTest {
    private static final long SEED = 20261117L;

    private static Path testmodAssets() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null && !Files.isDirectory(dir.resolve("src/testmod"))) {
            dir = dir.getParent();
        }
        return dir.resolve("src/testmod/resources/assets/bloomberto_testmod");
    }

    /** Random edits: delete, duplicate, replace with a structural character, or insert a random one. */
    private static String mutate(String s, Random r) {
        StringBuilder sb = new StringBuilder(s);
        int edits = 1 + r.nextInt(6);
        String specials = "{}[]\":,0123456789-.eE truefalsnul\\";
        for (int i = 0; i < edits && sb.length() > 0; i++) {
            int pos = r.nextInt(sb.length());
            switch (r.nextInt(4)) {
                case 0 -> sb.deleteCharAt(pos);
                case 1 -> sb.insert(pos, sb.substring(pos, Math.min(sb.length(), pos + 1 + r.nextInt(20))));
                case 2 -> sb.setCharAt(pos, specials.charAt(r.nextInt(specials.length())));
                default -> sb.insert(pos, (char) (32 + r.nextInt(95)));
            }
        }
        return sb.toString();
    }

    @FunctionalInterface
    interface Parse {
        Object run(String in) throws Exception;
    }

    private static void fuzzParser(String name, String valid, Parse parse, Class<? extends Exception> allowed, int rounds) {
        Random r = new Random(SEED ^ name.hashCode());
        int parsedOk = 0;
        for (int i = 0; i < rounds; i++) {
            String input = mutate(valid, r);
            try {
                Object result = parse.run(input);
                parsedOk++;
                if (result instanceof com.dougg865.bloomberto.api.animation.model.GeoModel model) {
                    // whatever parses must also be drawable
                    ModelGeometry.build(model, new Pose());
                }
            } catch (Exception e) {
                if (!allowed.isInstance(e)) {
                    fail(name + " threw " + e + " for input #" + i + ":\n" + input, e);
                }
            } catch (StackOverflowError | OutOfMemoryError e) {
                fail(name + " crashed with " + e + " for input #" + i);
            }
        }
        // sanity: the mutations must not be so destructive that nothing is ever valid
        assertTrue(parsedOk > 0 || rounds < 50, name + ": no mutated input parsed at all");
    }

    @Test
    void geometryFiles() throws Exception {
        String geo = Files.readString(testmodAssets().resolve("geo/totem.geo.json"));
        fuzzParser("geo", geo, GeoParser::parse, ParseException.class, 4000);
    }

    @Test
    void animationFiles() throws Exception {
        String anim = Files.readString(testmodAssets().resolve("animations/totem.animation.json"));
        fuzzParser("animation", anim, json -> {
            var clips = AnimationParser.parse(json);
            // sampling any parsed clip at any time must work
            for (var clip : clips.values()) {
                for (float t = -1f; t < 6f; t += 0.37f) {
                    clip.sample(t, new MolangContext());
                }
            }
            return clips;
        }, ParseException.class, 4000);
    }

    @Test
    void blockbenchProjects() throws Exception {
        String bb = Files.readString(testmodAssets().resolve("models/totem.bbmodel"));
        fuzzParser("bbmodel", bb, json -> {
            BbModelParser.parseAnimations(json).values().forEach(c -> c.sample(1f, new MolangContext()));
            BbModelParser.embeddedTextures(json);
            return BbModelParser.parseModel(json);
        }, ParseException.class, 4000);
    }

    @Test
    void tomlConfigFiles() {
        String toml = "# comment\ntop = 1\n[general]\nenabled = true\nratio = 0.5\nname = \"a \\\"b\\\" \\u00e9\"\nlist = [\"x\", \"y\", [1, 2]]\n";
        fuzzParser("toml", toml, Toml::parse, Toml.ParseException.class, 6000);
    }

    /** A random, grammatically valid Molang expression. */
    private static String expression(Random r, int depth) {
        if (depth <= 0) {
            return switch (r.nextInt(5)) {
                case 0 -> String.valueOf(r.nextInt(100));
                case 1 -> String.valueOf(r.nextDouble() * 10 - 5);
                case 2 -> "q.anim_time";
                case 3 -> "variable.x";
                default -> r.nextBoolean() ? "true" : "math.pi";
            };
        }
        String a = expression(r, depth - 1);
        String b = expression(r, depth - 1);
        return switch (r.nextInt(9)) {
            case 0 -> a + " + " + b;
            case 1 -> a + " * " + b;
            case 2 -> a + " / " + b;
            case 3 -> "-(" + a + ")";
            case 4 -> "(" + a + " > " + b + ") ? " + a + " : " + b;
            case 5 -> "math.sin(" + a + ")";
            case 6 -> "math.clamp(" + a + ", 0, " + b + ")";
            case 7 -> "!" + a + " || " + b + " && " + a;
            default -> "math.pow(" + a + ", " + b + ")";
        };
    }

    @Test
    void molangExpressions() {
        Random r = new Random(SEED);
        MolangContext ctx = new MolangContext().set("query.anim_time", 1.25);
        int compiled = 0;
        for (int i = 0; i < 20000; i++) {
            String expr = expression(r, r.nextInt(6));
            if (r.nextBoolean()) {
                expr = mutate(expr, r);
            }
            if (r.nextInt(20) == 0) {
                expr = "v.x = " + expr + "; return v.x * 2";
            }
            Molang m;
            try {
                m = Molang.compile(expr);
            } catch (MolangException expected) {
                continue;
            } catch (RuntimeException | StackOverflowError e) {
                fail("compile crashed on: " + expr, e);
                return;
            }
            compiled++;
            try {
                m.eval(ctx); // NaN or infinity is acceptable output; an exception is not
            } catch (RuntimeException | StackOverflowError e) {
                fail("eval crashed on: " + expr, e);
            }
        }
        assertTrue(compiled > 10000, "too few valid expressions generated: " + compiled);
    }

    @Test
    void imagesFromTheNetwork() throws Exception {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(8, 8, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", out);
        byte[] png = out.toByteArray();
        Random r = new Random(SEED);
        for (int i = 0; i < 3000; i++) {
            byte[] data;
            if (r.nextInt(4) == 0) {
                data = new byte[r.nextInt(64)];
                r.nextBytes(data);
            } else {
                data = png.clone();
                for (int k = 0; k < 1 + r.nextInt(8); k++) {
                    data[r.nextInt(data.length)] = (byte) r.nextInt(256);
                }
                if (r.nextBoolean()) {
                    data = java.util.Arrays.copyOf(data, r.nextInt(data.length + 1));
                }
            }
            try {
                ImageDecoder.Decoded d = ImageDecoder.decode(data, 4096);
                assertTrue(d.width() * d.height() <= 4096 && d.argb().length == d.width() * d.height(), "inconsistent image");
            } catch (IOException expected) {
                // rejected cleanly
            } catch (RuntimeException | Error e) {
                fail("image #" + i + " crashed the decoder: " + e, e);
            }
        }
    }
}
