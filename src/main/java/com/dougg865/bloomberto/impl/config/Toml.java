package com.dougg865.bloomberto.impl.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A small TOML subset: {@code [table]} headers, {@code key = value} lines, {@code #} comments and
 * single-line values (booleans, integers, floats, basic strings and arrays of those).
 * Parsed values are {@code Boolean}, {@code Long}, {@code Double}, {@code String} or {@code List<Object>}.
 */
public final class Toml {
    /** Thrown for malformed input; the message includes the line number. */
    public static final class ParseException extends Exception {
        public ParseException(int line, String message) {
            super("line " + line + ": " + message);
        }
    }

    private Toml() {
    }

    /** Parses a document into a flat map keyed by the dotted path ({@code table.key}). */
    public static Map<String, Object> parse(String text) throws ParseException {
        Map<String, Object> result = new LinkedHashMap<>();
        String table = "";
        String[] lines = text.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].strip();
            int number = i + 1;
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("[")) {
                int end = line.indexOf(']');
                if (end < 0) {
                    throw new ParseException(number, "unterminated table header");
                }
                String rest = line.substring(end + 1).strip();
                if (!rest.isEmpty() && !rest.startsWith("#")) {
                    throw new ParseException(number, "unexpected text after table header");
                }
                table = line.substring(1, end).strip();
                if (table.isEmpty()) {
                    throw new ParseException(number, "empty table name");
                }
                continue;
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                throw new ParseException(number, "expected 'key = value'");
            }
            String key = line.substring(0, eq).strip();
            if (key.isEmpty()) {
                throw new ParseException(number, "empty key");
            }
            Cursor cursor = new Cursor(line, eq + 1, number);
            Object value = cursor.readValue();
            cursor.expectEnd();
            result.put(table.isEmpty() ? key : table + "." + key, value);
        }
        return result;
    }

    /** Parses a single value, as produced by {@link #format(Object)}. */
    public static Object parseValue(String text) throws ParseException {
        Cursor cursor = new Cursor(text, 0, 1);
        Object value = cursor.readValue();
        cursor.expectEnd();
        return value;
    }

    /** Formats a value ({@code Boolean}, {@code Number}, {@code String} or {@code List}) as TOML. */
    public static String format(Object value) {
        if (value instanceof String s) {
            return quote(s);
        }
        if (value instanceof Double d) {
            if (d.isNaN() || d.isInfinite()) {
                throw new IllegalArgumentException("TOML cannot store " + d);
            }
            String text = Double.toString(d);
            return text.contains(".") || text.contains("E") ? text : text + ".0";
        }
        if (value instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(format(list.get(i)));
            }
            return sb.append(']').toString();
        }
        return String.valueOf(value);
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }

    private static final class Cursor {
        private final String text;
        private final int line;
        private int pos;

        Cursor(String text, int pos, int line) {
            this.text = text;
            this.pos = pos;
            this.line = line;
        }

        private void skipSpaces() {
            while (pos < text.length() && (text.charAt(pos) == ' ' || text.charAt(pos) == '\t')) {
                pos++;
            }
        }

        void expectEnd() throws ParseException {
            skipSpaces();
            if (pos < text.length() && text.charAt(pos) != '#') {
                throw new ParseException(line, "unexpected text after value");
            }
        }

        Object readValue() throws ParseException {
            skipSpaces();
            if (pos >= text.length()) {
                throw new ParseException(line, "missing value");
            }
            char c = text.charAt(pos);
            if (c == '"') {
                return readString();
            }
            if (c == '[') {
                return readArray();
            }
            int start = pos;
            while (pos < text.length() && ",]# \t".indexOf(text.charAt(pos)) < 0) {
                pos++;
            }
            String token = text.substring(start, pos);
            return readScalar(token);
        }

        private Object readScalar(String token) throws ParseException {
            if (token.equals("true")) {
                return Boolean.TRUE;
            }
            if (token.equals("false")) {
                return Boolean.FALSE;
            }
            try {
                String clean = token.replace("_", "");
                if (clean.matches("[+-]?\\d+")) {
                    return Long.parseLong(clean);
                }
                if (clean.matches("[+-]?(\\d+\\.\\d*|\\.\\d+|\\d+)([eE][+-]?\\d+)?")) {
                    return Double.parseDouble(clean);
                }
            } catch (NumberFormatException e) {
                throw new ParseException(line, "number out of range: " + token);
            }
            throw new ParseException(line, "invalid value: " + token);
        }

        private String readString() throws ParseException {
            StringBuilder sb = new StringBuilder();
            pos++; // opening quote
            while (pos < text.length()) {
                char c = text.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                if (pos >= text.length()) {
                    break;
                }
                char e = text.charAt(pos++);
                switch (e) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case 'u' -> {
                        if (pos + 4 > text.length()) {
                            throw new ParseException(line, "bad \\u escape");
                        }
                        try {
                            sb.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
                        } catch (NumberFormatException ex) {
                            throw new ParseException(line, "bad \\u escape");
                        }
                        pos += 4;
                    }
                    default -> throw new ParseException(line, "unknown escape \\" + e);
                }
            }
            throw new ParseException(line, "unterminated string");
        }

        private List<Object> readArray() throws ParseException {
            List<Object> list = new ArrayList<>();
            pos++; // [
            while (true) {
                skipSpaces();
                if (pos >= text.length()) {
                    throw new ParseException(line, "unterminated array");
                }
                if (text.charAt(pos) == ']') {
                    pos++;
                    return list;
                }
                list.add(readValue());
                skipSpaces();
                if (pos < text.length() && text.charAt(pos) == ',') {
                    pos++;
                } else if (pos < text.length() && text.charAt(pos) != ']') {
                    throw new ParseException(line, "expected ',' or ']' in array");
                }
            }
        }
    }
}
