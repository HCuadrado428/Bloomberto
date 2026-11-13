package com.dougg865.bloomberto.impl.registry;

import com.dougg865.bloomberto.Bloomberto;
import com.dougg865.bloomberto.api.registry.Registered;
import com.dougg865.bloomberto.api.registry.Registrar;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Content registered only with {@code -Dbloomberto.debugEvents=true}, to check {@link Registrar} on a real game. */
public final class DebugContent {
    private DebugContent() {
    }

    public static void register() {
        Registrar reg = Registrar.of(Bloomberto.MOD_ID);
        Registered<Item> item = reg.item("debug_item", () -> new Item(new Item.Properties()));
        Registered<Block> block = reg.block("debug_block", () -> new Block(BlockBehaviour.Properties.of()));
        Registered<Item> blockItem = reg.blockItem("debug_block", block);
        reg.sound("debug_sound");
        reg.mobEffect("debug_effect", () -> new MobEffect(MobEffectCategory.NEUTRAL, 0xFF00FF) { });
        reg.creativeTab("debug_tab", Component.literal("Bloomberto debug"), () -> new ItemStack(item.get()),
                List.of(item, blockItem));
        reg.register();
    }

    /** Logs whether every debug entry ended up in the vanilla registries. */
    public static void verify() {
        boolean ok = has(BuiltInRegistries.ITEM, "debug_item") && has(BuiltInRegistries.BLOCK, "debug_block")
                && has(BuiltInRegistries.ITEM, "debug_block") && has(BuiltInRegistries.SOUND_EVENT, "debug_sound")
                && has(BuiltInRegistries.MOB_EFFECT, "debug_effect") && has(BuiltInRegistries.CREATIVE_MODE_TAB, "debug_tab");
        Bloomberto.LOGGER.info("[debugEvents] registry entries {}", ok ? "ok" : "MISSING");
    }

    private static boolean has(net.minecraft.core.Registry<?> registry, String name) {
        boolean present = registry.containsKey(Bloomberto.id(name));
        if (!present) {
            Bloomberto.LOGGER.error("[debugEvents] missing {} in {}", name, registry.key().location());
        }
        return present;
    }
}
