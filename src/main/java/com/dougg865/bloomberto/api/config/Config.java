package com.dougg865.bloomberto.api.config;

import com.dougg865.bloomberto.Bloomberto;
import com.dougg865.bloomberto.impl.config.Toml;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A loaded config file. Create one with {@link ConfigBuilder}. */
public final class Config {
    private final String modId;
    private final Path file;
    private final List<ConfigValue<?>> values;

    Config(String modId, Path file, List<ConfigValue<?>> values) {
        this.modId = modId;
        this.file = file;
        this.values = List.copyOf(values);
    }

    public String modId() {
        return modId;
    }

    public Path file() {
        return file;
    }

    public List<ConfigValue<?>> values() {
        return values;
    }

    /**
     * Reads the file. Missing files are created with the defaults; invalid or out-of-range values are
     * corrected and the file is rewritten; an unreadable file is kept as {@code <name>.bak} and replaced.
     */
    public void load() {
        if (!Files.exists(file)) {
            save();
            return;
        }
        Map<String, Object> parsed;
        try {
            parsed = Toml.parse(Files.readString(file, StandardCharsets.UTF_8));
        } catch (Toml.ParseException | IOException e) {
            Bloomberto.LOGGER.error("Could not read config {} ({}); using defaults and keeping a backup", file, e.getMessage());
            backup();
            values.forEach(ConfigValue::reset);
            save();
            return;
        }
        boolean rewrite = false;
        for (ConfigValue<?> value : values) {
            Object raw = parsed.get(value.path());
            if (raw == null) {
                value.load(null);
                rewrite = true; // new option: add it to the file
            } else if (!value.load(raw)) {
                Bloomberto.LOGGER.warn("Invalid value for '{}' in {}; using the default", value.path(), file.getFileName());
                rewrite = true;
            } else if (!value.encode().equals(Toml.format(raw))) {
                rewrite = true; // value was corrected (e.g. clamped)
            }
        }
        if (rewrite) {
            save();
        }
    }

    /** Writes the file with comments, grouped by section. */
    public void save() {
        StringBuilder sb = new StringBuilder();
        // Top-level keys (empty section name) must come first: in TOML anything after a [table] header belongs to it.
        Map<String, List<ConfigValue<?>>> bySection = new LinkedHashMap<>();
        bySection.put("", new ArrayList<>());
        for (ConfigValue<?> value : values) {
            String path = value.path();
            int dot = path.lastIndexOf('.');
            bySection.computeIfAbsent(dot < 0 ? "" : path.substring(0, dot), k -> new ArrayList<>()).add(value);
        }
        boolean first = true;
        for (Map.Entry<String, List<ConfigValue<?>>> section : bySection.entrySet()) {
            if (section.getValue().isEmpty()) {
                continue;
            }
            if (!first) {
                sb.append('\n');
            }
            first = false;
            if (!section.getKey().isEmpty()) {
                sb.append('[').append(section.getKey()).append("]\n");
            }
            for (ConfigValue<?> value : section.getValue()) {
                writeEntry(sb, value);
            }
        }
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, sb.toString(), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Bloomberto.LOGGER.error("Could not save config {}", file, e);
        }
    }

    private static void writeEntry(StringBuilder sb, ConfigValue<?> value) {
        if (!value.comment().isEmpty()) {
            for (String line : value.comment().split("\\R")) {
                sb.append("# ").append(line).append('\n');
            }
        }
        String constraints = value.type().describe();
        if (!constraints.isEmpty()) {
            sb.append("# ").append(constraints).append('\n');
        }
        sb.append("# Default: ").append(value.encodeDefault()).append('\n');
        if (value.isSynced()) {
            sb.append("# Synced: the server's value is used while connected to a server\n");
        }
        String key = value.path().substring(value.path().lastIndexOf('.') + 1);
        sb.append(key).append(" = ").append(value.encode()).append('\n');
    }

    private void backup() {
        try {
            Files.copy(file, file.resolveSibling(file.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Bloomberto.LOGGER.warn("Could not back up {}", file, e);
        }
    }

    /** The synced options as {@code path -> encoded value}. */
    public Map<String, String> syncedValues() {
        Map<String, String> result = new LinkedHashMap<>();
        for (ConfigValue<?> value : values) {
            if (value.isSynced()) {
                result.put(value.path(), value.encode());
            }
        }
        return result;
    }

    /** Applies values received from the server; unknown or non-synced paths are ignored. */
    public void applyServerValues(Map<String, String> encoded) {
        for (ConfigValue<?> value : values) {
            String text = encoded.get(value.path());
            if (value.isSynced() && text != null) {
                value.applyServerValue(text);
            }
        }
    }

    public void clearServerValues() {
        values.forEach(ConfigValue::clearServerOverride);
    }
}
