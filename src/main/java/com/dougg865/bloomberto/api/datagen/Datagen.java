package com.dougg865.bloomberto.api.datagen;

import com.dougg865.bloomberto.platform.Services;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Where mods register what to generate. Run it in a development environment with {@code /bloomberto datagen <modid>}
 * (needs permission level 2); files are written to {@code <game dir>/bloomberto_datagen/<modid>}, or to the folder given by
 * {@code -Dbloomberto.datagen.out=<folder>}, ready to be copied into {@code src/main/resources}.
 *
 * <pre>{@code
 * Datagen.register("mymod", gen -> {
 *     gen.lang("en_us").add("item.mymod.ruby", "Ruby");
 *     gen.itemModel("ruby");
 * });
 * }</pre>
 */
public final class Datagen {
    private static final Map<String, Consumer<DataGenerator>> PROVIDERS = new ConcurrentHashMap<>();

    private Datagen() {
    }

    public static void register(String modId, Consumer<DataGenerator> provider) {
        if (PROVIDERS.putIfAbsent(modId, provider) != null) {
            throw new IllegalStateException("Datagen already registered for " + modId);
        }
    }

    public static List<String> registeredMods() {
        return PROVIDERS.keySet().stream().sorted().toList();
    }

    /** The folder {@link #run} writes to. */
    public static Path outputFor(String modId) {
        String override = System.getProperty("bloomberto.datagen.out");
        Path base = override != null ? Path.of(override) : Services.PLATFORM.getGameDir().resolve("bloomberto_datagen");
        return base.resolve(modId);
    }

    /**
     * Runs a mod's registered generator.
     *
     * @return the files that were written
     * @throws IllegalArgumentException if the mod registered nothing
     */
    public static List<String> run(String modId, Path output) throws IOException {
        Consumer<DataGenerator> provider = PROVIDERS.get(modId);
        if (provider == null) {
            throw new IllegalArgumentException("No datagen registered for " + modId + "; registered: " + registeredMods());
        }
        DataGenerator generator = DataGenerator.create(modId, output);
        provider.accept(generator);
        return generator.generate();
    }
}
