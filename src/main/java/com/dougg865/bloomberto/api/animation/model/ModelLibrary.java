package com.dougg865.bloomberto.api.animation.model;

import com.dougg865.bloomberto.Bloomberto;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/**
 * Loads Blockbench models and animations from resource packs and keeps them until resources reload (F3+T), so editing a file and
 * reloading shows the change without restarting. Client only.
 *
 * <pre>{@code
 * GeoModel model = ModelLibrary.model(Bloomberto.id("mymod", "geo/golem.geo.json"));
 * AnimationController controller = ModelLibrary.animations(Bloomberto.id("mymod", "animations/golem.animation.json")).controller();
 * controller.play("animation.golem.walk").transition(5);
 * }</pre>
 *
 * Files live in {@code assets/<namespace>/<path>}. A file that is missing or broken is reported in the log once and yields an
 * empty model / no animations instead of crashing the game.
 */
public final class ModelLibrary {
    /** The animations of one file. */
    public record AnimationSet(Map<String, AnimationClip> clips) {
        public static final AnimationSet EMPTY = new AnimationSet(Map.of());

        /** A new controller over these animations (one per entity). */
        public AnimationController controller() {
            return new AnimationController(clips);
        }
    }

    /** What {@link #model} returns when the file cannot be used: nothing is drawn. */
    public static final GeoModel EMPTY_MODEL = new GeoModel("empty", 16, 16, java.util.List.of());

    private static final Map<ResourceLocation, GeoModel> MODELS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, AnimationSet> ANIMATIONS = new ConcurrentHashMap<>();
    private static boolean listening;

    private ModelLibrary() {
    }

    public static GeoModel model(ResourceLocation location) {
        listen();
        return MODELS.computeIfAbsent(location, loc -> {
            try {
                return GeoParser.parse(read(loc));
            } catch (ParseException | IOException e) {
                Bloomberto.LOGGER.error("Could not load model {}: {}", loc, e.getMessage());
                return EMPTY_MODEL;
            }
        });
    }

    public static AnimationSet animations(ResourceLocation location) {
        listen();
        return ANIMATIONS.computeIfAbsent(location, loc -> {
            try {
                return new AnimationSet(AnimationParser.parse(read(loc)));
            } catch (ParseException | IOException e) {
                Bloomberto.LOGGER.error("Could not load animations {}: {}", loc, e.getMessage());
                return AnimationSet.EMPTY;
            }
        });
    }

    private static String read(ResourceLocation location) throws IOException {
        var resource = Minecraft.getInstance().getResourceManager().getResource(location)
                .orElseThrow(() -> new IOException("Resource not found: " + location));
        try (BufferedReader reader = resource.openAsReader()) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private static synchronized void listen() {
        if (!listening) {
            listening = true;
            com.dougg865.bloomberto.impl.render.ClientReloads.onReload(() -> {
                MODELS.clear();
                ANIMATIONS.clear();
            });
        }
        com.dougg865.bloomberto.impl.render.ClientReloads.ensureRegistered();
    }
}
