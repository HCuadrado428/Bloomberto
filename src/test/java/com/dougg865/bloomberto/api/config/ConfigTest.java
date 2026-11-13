package com.dougg865.bloomberto.api.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigTest {
    enum Mode { FAST, SLOW }

    @TempDir
    Path dir;

    private record Options(Config config, ConfigValue<Boolean> enabled, ConfigValue<Integer> count,
                           ConfigValue<Double> ratio, ConfigValue<String> name, ConfigValue<Mode> mode,
                           ConfigValue<List<String>> list) { }

    private Options build(Path file) {
        ConfigBuilder b = ConfigBuilder.create("test");
        b.push("general");
        ConfigValue<Boolean> enabled = b.defineBoolean("enabled", true, "Turns it on");
        ConfigValue<Integer> count = b.defineInt("count", 5, 1, 10, "How many").synced();
        ConfigValue<Double> ratio = b.defineDouble("ratio", 0.5, 0.0, 1.0, "");
        b.pop();
        ConfigValue<String> name = b.defineString("name", "hello", "Display name");
        ConfigValue<Mode> mode = b.defineEnum("mode", Mode.FAST, "");
        ConfigValue<List<String>> list = b.defineStringList("list", List.of("a"), "");
        return new Options(b.build(file), enabled, count, ratio, name, mode, list);
    }

    @Test
    void createsFileWithDefaultsAndComments() throws IOException {
        Path file = dir.resolve("test.toml");
        Options o = build(file);
        assertTrue(Files.exists(file));
        assertEquals(5, o.count().get());
        String text = Files.readString(file);
        assertTrue(text.contains("[general]"));
        assertTrue(text.contains("# Turns it on"));
        assertTrue(text.contains("# Range: 1 ~ 10"));
        assertTrue(text.contains("# Default: 5"));
        assertTrue(text.contains("count = 5"));
        assertTrue(text.contains("# Allowed: fast, slow"));
        assertTrue(text.contains("mode = \"fast\""));
    }

    @Test
    void savedValuesAreLoadedBack() {
        Path file = dir.resolve("test.toml");
        Options a = build(file);
        a.enabled().set(false);
        a.count().set(9);
        a.ratio().set(0.25);
        a.name().set("with \"quotes\"");
        a.mode().set(Mode.SLOW);
        a.list().set(List.of("x", "y"));
        a.config().save();

        Options b = build(file);
        assertFalse(b.enabled().get());
        assertEquals(9, b.count().get());
        assertEquals(0.25, b.ratio().get());
        assertEquals("with \"quotes\"", b.name().get());
        assertEquals(Mode.SLOW, b.mode().get());
        assertEquals(List.of("x", "y"), b.list().get());
    }

    @Test
    void outOfRangeValuesAreClampedAndFileIsFixed() throws IOException {
        Path file = dir.resolve("test.toml");
        Files.writeString(file, "[general]\ncount = 500\nratio = -3.0\n");
        Options o = build(file);
        assertEquals(10, o.count().get());
        assertEquals(0.0, o.ratio().get());
        String text = Files.readString(file);
        assertTrue(text.contains("count = 10"));
        assertTrue(text.contains("name = \"hello\""), "missing options are added to the file");
    }

    @Test
    void wrongTypesFallBackToDefaults() throws IOException {
        Path file = dir.resolve("test.toml");
        Files.writeString(file, "[general]\nenabled = \"yes\"\ncount = 2.5\n\nmode = \"medium\"\n");
        Options o = build(file);
        assertTrue(o.enabled().get());
        assertEquals(5, o.count().get());
        assertEquals(Mode.FAST, o.mode().get());
    }

    @Test
    void enumParsingIgnoresCase() throws IOException {
        Path file = dir.resolve("test.toml");
        Files.writeString(file, "mode = \"SLOW\"\n");
        assertEquals(Mode.SLOW, build(file).mode().get());
    }

    @Test
    void brokenFileIsBackedUpAndReplaced() throws IOException {
        Path file = dir.resolve("test.toml");
        Files.writeString(file, "this is not toml\n", StandardCharsets.UTF_8);
        Options o = build(file);
        assertEquals(5, o.count().get());
        assertEquals("this is not toml\n", Files.readString(dir.resolve("test.toml.bak")));
        assertTrue(Files.readString(file).contains("count = 5"));
    }

    @Test
    void setCorrectsOutOfRangeValues() {
        Options o = build(dir.resolve("test.toml"));
        o.count().set(99);
        assertEquals(10, o.count().get());
        o.count().reset();
        assertEquals(5, o.count().get());
    }

    @Test
    void serverValuesOverrideOnlySyncedOptionsAndCanBeCleared() {
        Options o = build(dir.resolve("test.toml"));
        assertEquals(Map.of("general.count", "5"), o.config().syncedValues());
        o.count().set(3);
        o.config().applyServerValues(Map.of("general.count", "8", "general.enabled", "false", "general.ratio", "0.9"));
        assertEquals(8, o.count().get());
        assertTrue(o.enabled().get(), "non-synced options ignore the server");
        assertEquals(3, o.count().localValue());
        o.config().clearServerValues();
        assertEquals(3, o.count().get());
    }

    @Test
    void invalidServerValuesAreIgnoredAndClamped() {
        Options o = build(dir.resolve("test.toml"));
        o.config().applyServerValues(Map.of("general.count", "not a number"));
        assertEquals(5, o.count().get());
        o.config().applyServerValues(Map.of("general.count", "1000"));
        assertEquals(10, o.count().get());
    }

    @Test
    void builderRejectsMistakes() {
        ConfigBuilder b = ConfigBuilder.create("x");
        b.defineInt("a", 1, 0, 2, "");
        assertThrows(IllegalStateException.class, () -> b.defineInt("a", 1, 0, 2, ""));
        assertThrows(IllegalArgumentException.class, () -> b.defineInt("b", 50, 0, 2, ""));
        assertThrows(IllegalArgumentException.class, () -> b.defineBoolean("bad.key", true, ""));
        assertThrows(IllegalArgumentException.class, () -> b.push("a.b"));
        assertThrows(IllegalStateException.class, b::pop);
        b.push("open");
        assertThrows(IllegalStateException.class, () -> b.build(Path.of("unused.toml")));
    }

    @Test
    void syncPacketRoundTrips() {
        var buf = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        var original = new com.dougg865.bloomberto.impl.config.ConfigSync.Packet("test",
                new java.util.LinkedHashMap<>(Map.of("general.count", "7")));
        com.dougg865.bloomberto.impl.config.ConfigSync.Packet.TYPE.codec().encode(original, buf);
        assertEquals(original, com.dougg865.bloomberto.impl.config.ConfigSync.Packet.TYPE.codec().decode(buf));
    }
}
