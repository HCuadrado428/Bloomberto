package com.dougg865.bloomberto.api.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dougg865.bloomberto.impl.config.Toml;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TomlTest {
    @Test
    void parsesTablesKeysAndComments() throws Exception {
        Map<String, Object> m = Toml.parse("""
                # top comment
                top = 1

                [general]
                enabled = true   # trailing comment
                name = "Bloom # not a comment"
                ratio = 0.5
                count = -42

                [general.nested]
                list = ["a", "b",]
                """);
        assertEquals(1L, m.get("top"));
        assertEquals(true, m.get("general.enabled"));
        assertEquals("Bloom # not a comment", m.get("general.name"));
        assertEquals(0.5, m.get("general.ratio"));
        assertEquals(-42L, m.get("general.count"));
        assertEquals(List.of("a", "b"), m.get("general.nested.list"));
    }

    @Test
    void stringEscapesRoundTrip() throws Exception {
        String original = "line1\nline2\t\"quoted\" back\\slash é \u0001";
        assertEquals(original, Toml.parseValue(Toml.format(original)));
    }

    @Test
    void numbersAndLists() throws Exception {
        assertEquals(1_000L, Toml.parseValue("1_000"));
        assertEquals(1.5e3, Toml.parseValue("1.5e3"));
        assertEquals(List.of(1L, 2L, 3L), Toml.parseValue("[1, 2, 3]"));
        assertEquals(List.of(), Toml.parseValue("[]"));
        assertEquals("3.0", Toml.format(3.0));
        assertEquals("[\"x\", \"y\"]", Toml.format(List.of("x", "y")));
    }

    @Test
    void reportsErrorsWithLineNumbers() {
        Toml.ParseException e = assertThrows(Toml.ParseException.class, () -> Toml.parse("a = 1\nbroken line\n"));
        assertEquals("line 2: expected 'key = value'", e.getMessage());
        assertThrows(Toml.ParseException.class, () -> Toml.parse("a = \"unterminated"));
        assertThrows(Toml.ParseException.class, () -> Toml.parse("a = 12abc"));
        assertThrows(Toml.ParseException.class, () -> Toml.parse("[open\na = 1"));
        assertThrows(Toml.ParseException.class, () -> Toml.parse("a = 1 junk"));
        assertThrows(Toml.ParseException.class, () -> Toml.parse("a = [1, 2"));
        assertThrows(Toml.ParseException.class, () -> Toml.parse("a = 99999999999999999999"));
    }

    @Test
    void rejectsNonFiniteDoubles() {
        assertThrows(IllegalArgumentException.class, () -> Toml.format(Double.NaN));
    }
}
