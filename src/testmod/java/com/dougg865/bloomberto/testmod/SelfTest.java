package com.dougg865.bloomberto.testmod;

import com.dougg865.bloomberto.Bloomberto;
import com.dougg865.bloomberto.api.BloombertoApi;
import com.dougg865.bloomberto.api.animation.Easing;
import com.dougg865.bloomberto.api.animation.Tween;
import com.dougg865.bloomberto.api.animation.TweenManager;
import com.dougg865.bloomberto.api.animation.model.AnimationParser;
import com.dougg865.bloomberto.api.animation.model.BbModelParser;
import com.dougg865.bloomberto.api.animation.model.GeoParser;
import com.dougg865.bloomberto.api.animation.molang.Molang;
import com.dougg865.bloomberto.api.animation.molang.MolangContext;
import com.dougg865.bloomberto.api.config.Config;
import com.dougg865.bloomberto.api.config.ConfigBuilder;
import com.dougg865.bloomberto.api.config.ConfigValue;
import com.dougg865.bloomberto.api.data.DataKey;
import com.dougg865.bloomberto.api.data.EntityData;
import com.dougg865.bloomberto.api.datagen.Datagen;
import com.dougg865.bloomberto.api.effects.Particles;
import com.dougg865.bloomberto.api.event.BloombertoEvents;
import com.dougg865.bloomberto.api.network.PacketType;
import com.dougg865.bloomberto.api.scheduler.Scheduler;
import com.dougg865.bloomberto.api.scheduler.Schedulers;
import io.netty.buffer.Unpooled;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.IntTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Automatic checks of every Bloomberto feature on a real game, enabled with {@code -Dbloomberto.selfTest=server|client|all}.
 * Each check logs {@code [selftest] PASS <name>} or {@code [selftest] FAIL <name>: <why>}, and the run ends with
 * {@code [selftest] <side> done: N passed, M failed}. With {@code -Dbloomberto.selfTest.exit=false} the game keeps running.
 * This class covers the server side (it also runs on the integrated server of a singleplayer world).
 */
public final class SelfTest {
    /** A key used only by the self-test, on a pig that is created and removed again. */
    public static final DataKey<Integer> PROBE = DataKey.ofInt(Bloomberto.id(TestMod.MOD_ID, "selftest_probe"), 0);

    private static final String MODE = System.getProperty("bloomberto.selfTest", "");
    /** Counts per side: the integrated server and the client run in the same game, on different threads. */
    private static final Map<String, int[]> COUNTS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<String, List<String>> FAILURES = new java.util.concurrent.ConcurrentHashMap<>();

    private static String side() {
        return Thread.currentThread().getName().contains("Server") ? "server" : "client";
    }
    private static boolean started;
    private static int serverTicks;
    private static int levelLoads;

    private SelfTest() {
    }

    public static boolean server() {
        return MODE.equals("server") || MODE.equals("all");
    }

    public static boolean client() {
        return MODE.equals("client") || MODE.equals("all");
    }

    public static boolean exitWhenDone() {
        return !"false".equals(System.getProperty("bloomberto.selfTest.exit"));
    }

    @FunctionalInterface
    public interface Check {
        void run() throws Exception;
    }

    /** Runs {@code check}; any exception or {@link AssertionError} counts as a failure. */
    public static void check(String name, Check check) {
        try {
            check.run();
            synchronized (COUNTS) {
                COUNTS.computeIfAbsent(side(), k -> new int[2])[0]++;
            }
            TestMod.LOGGER.info("[selftest] PASS {}", name);
        } catch (Throwable e) {
            synchronized (COUNTS) {
                COUNTS.computeIfAbsent(side(), k -> new int[2])[1]++;
                FAILURES.computeIfAbsent(side(), k -> new ArrayList<>()).add(name);
            }
            TestMod.LOGGER.error("[selftest] FAIL {}: {}", name, e.toString(), e);
        }
    }

    public static void expect(boolean ok, String what) {
        if (!ok) {
            throw new AssertionError(what);
        }
    }

    public static void summary(String side) {
        int[] c;
        List<String> failures;
        synchronized (COUNTS) {
            c = COUNTS.getOrDefault(side, new int[2]).clone();
            failures = List.copyOf(FAILURES.getOrDefault(side, List.of()));
        }
        TestMod.LOGGER.info("[selftest] {} done: {} passed, {} failed{}", side, c[0], c[1], failures.isEmpty() ? "" : " " + failures);
    }

    /** Hooks the server checks; called from {@link TestMod#init()}. */
    static void register() {
        if (!server()) {
            return;
        }
        BloombertoEvents.SERVER_TICK.register(server -> serverTicks++);
        BloombertoEvents.LEVEL_LOAD.register(level -> {
            levelLoads++;
            if (!started && level.dimension() == Level.OVERWORLD) {
                started = true;
                Schedulers.server().runLater(20, () -> runServerChecks(level));
            }
        });
        BloombertoEvents.SERVER_STOPPED.register(server -> TestMod.LOGGER.info("[selftest] SERVER_STOPPED fired"));
    }

    private static void runServerChecks(ServerLevel level) {
        MinecraftServer server = level.getServer();
        check("api level", () -> expect(BloombertoApi.level() >= 2, "level " + BloombertoApi.level()));
        check("events fired", () -> expect(serverTicks > 0 && levelLoads >= 1, "ticks " + serverTicks + ", loads " + levelLoads));
        check("registry entries", () -> {
            expect(BuiltInRegistries.ITEM.containsKey(TestMod.EXAMPLE_ITEM.id()), "example_item missing");
            expect(BuiltInRegistries.BLOCK.containsKey(TestMod.TOTEM_BLOCK.id()), "totem missing");
            expect(BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(TestMod.TOTEM_BE.id()), "totem block entity missing");
            expect(TestMod.EXAMPLE_ITEM.get() != null, "example_item not created");
        });
        check("packet codecs round-trip", () -> {
            roundTrip(TestMod.Greeting.TYPE, new TestMod.Greeting("héllo ✓"));
            roundTrip(com.dougg865.bloomberto.impl.config.ConfigSync.Packet.TYPE,
                    new com.dougg865.bloomberto.impl.config.ConfigSync.Packet("mod", Map.of("a.b", "1", "c", "\"x\"")));
            roundTrip(com.dougg865.bloomberto.impl.data.DataSync.Packet.TYPE,
                    new com.dougg865.bloomberto.impl.data.DataSync.Packet(java.util.UUID.randomUUID(), Bloomberto.id("k"), IntTag.valueOf(7)));
            roundTrip(com.dougg865.bloomberto.impl.data.DataSync.Packet.TYPE,
                    new com.dougg865.bloomberto.impl.data.DataSync.Packet(java.util.UUID.randomUUID(), Bloomberto.id("k"), null));
            roundTrip(com.dougg865.bloomberto.impl.network.VersionCheck.Packet.TYPE,
                    new com.dougg865.bloomberto.impl.network.VersionCheck.Packet(BloombertoApi.level()));
        });
        check("config file round-trip", () -> {
            Path dir = Files.createTempDirectory("bloomberto-selftest");
            ConfigBuilder b = ConfigBuilder.create("selftest");
            b.push("section");
            ConfigValue<Integer> n = b.defineInt("n", 5, 1, 10, "a number");
            b.pop();
            Path file = dir.resolve("selftest.toml");
            Config c = b.build(file);
            expect(Files.exists(file), "file not created");
            Files.writeString(file, Files.readString(file).replace("n = 5", "n = 99"));
            c.load();
            expect(n.get() == 10, "out-of-range value not clamped: " + n.get());
            Files.writeString(file, "this is [ not toml");
            c.load();
            expect(n.get() == 5, "broken file did not fall back to the default");
            expect(Files.exists(dir.resolve("selftest.toml.bak")), "no backup of the broken file");
        });
        check("datagen writes files", () -> {
            Path dir = Files.createTempDirectory("bloomberto-datagen");
            List<String> written = Datagen.run(TestMod.MOD_ID, dir);
            expect(written.size() >= 8, "only " + written.size() + " files");
            for (String f : written) {
                com.google.gson.JsonParser.parseString(Files.readString(dir.resolve(f)));
            }
        });
        check("blockbench files in the jar parse", () -> {
            String bb = resource("/assets/bloomberto_testmod/models/totem.bbmodel");
            expect(!BbModelParser.parseModel(bb).bones().isEmpty(), "bbmodel has no bones");
            expect(BbModelParser.parseAnimations(bb).containsKey("animation.totem.sway"), "bbmodel misses sway");
            expect(!GeoParser.parse(resource("/assets/bloomberto_testmod/geo/totem.geo.json")).bones().isEmpty(), "geo has no bones");
            expect(AnimationParser.parse(resource("/assets/bloomberto_testmod/animations/totem.animation.json")).size() == 3,
                    "animation file");
        });
        check("molang", () -> expect(Math.abs(Molang.compile("math.sin(q.anim_time * 90) * 2")
                .eval(new MolangContext().set("query.anim_time", 1)) - 2) < 1e-9, "wrong value"));
        check("particles from the server", () -> Particles.of(net.minecraft.core.particles.ParticleTypes.FLAME)
                .sphere(spawnPos(level).getCenter(), 1, 8).spawn(level));

        // Entity data: set on a real entity, read back, then removed with the entity. Since 1.21.9 the spawn chunks are
        // not always loaded, so the chunk is loaded first: entities in unloaded chunks are never tracked by the level.
        BlockPos spawn = spawnPos(level);
        level.setChunkForced(spawn.getX() >> 4, spawn.getZ() >> 4, true);
        AtomicInteger parts = new AtomicInteger(2); // the entity part and the scheduler part; then the summary
        Runnable partDone = () -> {
            if (parts.decrementAndGet() == 0) {
                summary("server");
                if (exitWhenDone() && server.isDedicatedServer()) {
                    server.halt(false);
                }
            }
        };
        AtomicInteger waited = new AtomicInteger();
        Scheduler.Handle[] waitForChunk = new Scheduler.Handle[1];
        waitForChunk[0] = Schedulers.server().runRepeating(0, 1, () -> {
            boolean loaded = level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(spawn));
            if (!loaded && waited.incrementAndGet() < 400) {
                return;
            }
            waitForChunk[0].cancel();
            //? if >=1.21.11 {
            /*Entity pig = EntityType.PIG.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            *///?} else {
            Entity pig = EntityType.PIG.create(level);
            //?}
            check("entity data on a live entity", () -> {
                expect(loaded, "spawn chunk did not load");
                expect(pig != null, "could not create a pig");
                pig.setPos(spawn.getX() + 0.5, spawn.getY() + 1, spawn.getZ() + 0.5);
                expect(level.addFreshEntity(pig), "pig not added");
                EntityData.set(pig, PROBE, 42);
                expect(EntityData.get(pig, PROBE) == 42, "read back " + EntityData.get(pig, PROBE));
                expect(EntityData.has(pig, PROBE), "has() false");
            });
            if (pig != null) {
                pig.discard();
            }
            Schedulers.server().runLater(2, () -> {
                check("data of a removed entity is deleted", () -> expect(pig != null && !EntityData.has(pig, PROBE), "still stored"));
                level.setChunkForced(spawn.getX() >> 4, spawn.getZ() >> 4, false);
                partDone.run();
            });
        });

        // Scheduler and tweens over the next ticks.
        AtomicInteger later = new AtomicInteger();
        AtomicInteger repeating = new AtomicInteger();
        Schedulers.server().runLater(3, later::incrementAndGet);
        Scheduler.Handle rep = Schedulers.server().runRepeating(0, 1, repeating::incrementAndGet);
        Schedulers.server().runLater(2, () -> {
            throw new IllegalStateException("selftest: a failing task (expected in the log)");
        });
        TweenManager tweens = new TweenManager();
        float[] tweenValue = {0f};
        Tween.of(0f, 1f).duration(5).ease(Easing.OUT_CUBIC).onUpdate(v -> tweenValue[0] = v).start(tweens);
        BloombertoEvents.SERVER_TICK.register(s -> tweens.tick());

        Schedulers.server().runLater(10, () -> {
            rep.cancel();
            check("scheduler (incl. a failing task in between)", () -> {
                expect(later.get() == 1, "runLater ran " + later.get() + " times");
                expect(repeating.get() >= 9, "repeating ran " + repeating.get() + " times");
            });
            check("tween reached its end", () -> expect(tweenValue[0] == 1f, "value " + tweenValue[0]));
            partDone.run();
        });
    }

    private static <T> void roundTrip(PacketType<T> type, T packet) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        type.codec().encode(packet, buf);
        T back = type.codec().decode(buf);
        expect(back.equals(packet), type.id() + ": " + packet + " came back as " + back);
        expect(buf.readableBytes() == 0, type.id() + ": " + buf.readableBytes() + " bytes left over");
        // a truncated packet must fail cleanly (an exception), never hang or return garbage silently
        FriendlyByteBuf full = new FriendlyByteBuf(Unpooled.buffer());
        type.codec().encode(packet, full);
        if (full.readableBytes() > 1) {
            byte[] bytes = new byte[full.readableBytes()];
            full.readBytes(bytes);
            FriendlyByteBuf cut = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes, 0, bytes.length - 1));
            try {
                T partial = type.codec().decode(cut);
                expect(!partial.equals(packet), type.id() + ": a truncated packet decoded to the original");
            } catch (RuntimeException expected) {
                // fine: rejected
            }
        }
    }

    private static String resource(String path) throws Exception {
        try (InputStream in = SelfTest.class.getResourceAsStream(path)) {
            expect(in != null, "missing resource " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static BlockPos spawnPos(net.minecraft.world.level.Level level) {
        //? if >=1.21.11 {
        /*return level.getRespawnData().pos();
        *///?} else {
        return level.getSharedSpawnPos();
        //?}
    }
}
