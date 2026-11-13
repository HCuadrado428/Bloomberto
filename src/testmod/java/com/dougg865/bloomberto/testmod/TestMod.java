package com.dougg865.bloomberto.testmod;

import com.dougg865.bloomberto.Bloomberto;
import com.dougg865.bloomberto.api.animation.Easing;
import com.dougg865.bloomberto.api.animation.Tween;
import com.dougg865.bloomberto.api.animation.TweenManager;
import com.dougg865.bloomberto.api.config.Config;
import com.dougg865.bloomberto.api.config.ConfigBuilder;
import com.dougg865.bloomberto.api.config.ConfigValue;
import com.dougg865.bloomberto.api.data.DataKey;
import com.dougg865.bloomberto.api.datagen.Datagen;
import com.dougg865.bloomberto.api.datagen.Ingredient;
import com.dougg865.bloomberto.api.data.EntityData;
import com.dougg865.bloomberto.api.effects.Particles;
import com.dougg865.bloomberto.api.event.BloombertoEvents;
import com.dougg865.bloomberto.api.network.Network;
import com.dougg865.bloomberto.api.network.PacketCodec;
import com.dougg865.bloomberto.api.network.PacketType;
import com.dougg865.bloomberto.api.registry.Registered;
import com.dougg865.bloomberto.api.registry.Registrar;
import com.dougg865.bloomberto.api.scheduler.Schedulers;
import com.dougg865.bloomberto.platform.Services;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example mod that uses the Bloomberto API: one small example of each feature. Read it next to the docs.
 * Every example logs a line starting with {@code [testmod]} so it can be checked on a real game.
 */
public final class TestMod {
    public static final String MOD_ID = "bloomberto_testmod";
    static final Logger LOGGER = LoggerFactory.getLogger("Bloomberto Testmod");

    // --- Config: a TOML file at config/bloomberto_testmod.toml, one option synced from the server ---
    public static final ConfigValue<Boolean> EFFECTS_ENABLED;
    public static final ConfigValue<Integer> GREETING_REPEAT;
    private static final Config CONFIG;

    static {
        ConfigBuilder builder = ConfigBuilder.create(MOD_ID);
        builder.push("general");
        EFFECTS_ENABLED = builder.defineBoolean("effects_enabled", true, "Show the demo visual effects on the client");
        GREETING_REPEAT = builder.defineInt("greeting_repeat", 1, 1, 5, "How many times the greeting is logged").synced();
        builder.pop();
        CONFIG = builder.build();
    }

    // --- Registry: an item, a block with its item and a creative tab ---
    private static final Registrar REGISTRAR = Registrar.of(MOD_ID);
    public static final Registered<Item> EXAMPLE_ITEM = REGISTRAR.item("example_item", () -> new Item(new Item.Properties()));
    public static final Registered<Block> EXAMPLE_BLOCK = REGISTRAR.block("example_block",
            () -> new Block(BlockBehaviour.Properties.of()));
    public static final Registered<Item> EXAMPLE_BLOCK_ITEM = REGISTRAR.blockItem("example_block", EXAMPLE_BLOCK);
    public static final Registered<?> TAB = REGISTRAR.creativeTab("tab", Component.literal("Bloomberto testmod"),
            () -> new ItemStack(EXAMPLE_ITEM.get()), List.of(EXAMPLE_ITEM, EXAMPLE_BLOCK_ITEM));

    // --- A block entity drawn with a Blockbench model (see TotemBlockEntity and TestModClient) ---
    public static final Registered<Block> TOTEM_BLOCK = REGISTRAR.block("totem", () -> new TotemBlock(BlockBehaviour.Properties.of().noOcclusion()));
    public static final Registered<net.minecraft.world.level.block.entity.BlockEntityType<?>> TOTEM_BE = REGISTRAR.blockEntityType("totem",
            () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(TotemBlockEntity::new, TOTEM_BLOCK.get()).build(null));

    // --- Data: a counter saved on each player and synced to their client ---
    public static final DataKey<Integer> JOINS = DataKey.ofInt(Bloomberto.id(MOD_ID, "joins"), 0).synced();

    // --- Network: the server greets the client, the client answers ---
    public record Greeting(String text) {
        public static final PacketType<Greeting> TYPE = PacketType.of(Bloomberto.id(MOD_ID, "greeting"), Greeting.class,
                PacketCodec.of((p, buf) -> buf.writeUtf(p.text()), buf -> new Greeting(buf.readUtf())));
    }

    public record Reply(String text) {
        public static final PacketType<Reply> TYPE = PacketType.of(Bloomberto.id(MOD_ID, "reply"), Reply.class,
                PacketCodec.of((p, buf) -> buf.writeUtf(p.text()), buf -> new Reply(buf.readUtf())));
    }

    private static final TweenManager TWEENS = new TweenManager();

    private TestMod() {
    }

    public static void init() {
        LOGGER.info("[testmod] loaded on {}; id helper -> {}; bloomberto loaded -> {}",
                Services.PLATFORM.getLoader(), Bloomberto.id(MOD_ID, "example"), Services.PLATFORM.isModLoaded(Bloomberto.MOD_ID));
        LOGGER.info("[testmod] config file {} (greeting_repeat={})", CONFIG.file().getFileName(), GREETING_REPEAT.get());

        REGISTRAR.register();

        // Datagen: run "/bloomberto datagen bloomberto_testmod" to write the JSON resources for the items above.
        Datagen.register(MOD_ID, gen -> {
            gen.lang("en_us").add("item.bloomberto_testmod.example_item", "Example Item")
                    .add("block.bloomberto_testmod.example_block", "Example Block");
            gen.itemModel("example_item");
            gen.cubeAll("example_block");
            gen.shaped("example_block", 1, List.of("##", "##"), java.util.Map.of('#', Ingredient.item("example_item")));
            gen.shapeless("example_item", 4, List.of(Ingredient.item("example_block")));
            gen.smelting(Ingredient.item("minecraft:stick"), "example_item", 0.1f, 100);
            gen.dropSelf("example_block");
            gen.itemTag("c:examples").add("example_item");
        });

        Network.registerS2C(Greeting.TYPE, (packet, ctx) -> {
            LOGGER.info("[testmod] client got greeting '{}'; joins seen by the client: {}", packet.text(),
                    EntityData.get(ctx.player(), JOINS));
            Network.sendToServer(new Reply("hi back"));
        });
        Network.registerC2S(Reply.TYPE, (packet, ctx) ->
                LOGGER.info("[testmod] server got reply '{}' from {}", packet.text(), ctx.player().getGameProfile().getName()));

        BloombertoEvents.PLAYER_JOIN.register(player -> {
            // Particles: a ring of flames around the player who just joined (sent from the server).
            Particles.of(net.minecraft.core.particles.ParticleTypes.FLAME).ring(player.position(), 1.5, 24).speed(0.04).spawn(player.level());
            EntityData.set(player, JOINS, EntityData.get(player, JOINS) + 1);
            for (int i = 0; i < GREETING_REPEAT.get(); i++) {
                Network.sendToPlayer(player, new Greeting("welcome " + player.getGameProfile().getName()));
            }
        });

        // Scheduler: runs on the server thread after 2 seconds (40 ticks), once the first level has loaded.
        BloombertoEvents.LEVEL_LOAD.register(level -> {
            if (level.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
                Schedulers.server().runLater(40, () -> LOGGER.info("[testmod] scheduler fired after 40 ticks"));
                // Animation: a tween driven by our own manager, ticked from the server tick.
                Tween.of(0f, 1f).duration(20).ease(Easing.OUT_CUBIC)
                        .onComplete(() -> LOGGER.info("[testmod] tween finished"))
                        .start(TWEENS);
            }
        });
        BloombertoEvents.SERVER_TICK.register(server -> TWEENS.tick());
    }
}
