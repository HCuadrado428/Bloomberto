package com.dougg865.bloomberto.api.registry;

import com.dougg865.bloomberto.platform.Services;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Declares content for a mod and registers it the right way for the running loader.
 *
 * <pre>{@code
 * private static final Registrar REG = Registrar.of("mymod");
 * public static final Registered<Block> RUBY_BLOCK = REG.block("ruby_block", () -> new Block(BlockBehaviour.Properties.of()));
 * public static final Registered<Item> RUBY_BLOCK_ITEM = REG.blockItem("ruby_block", RUBY_BLOCK);
 *
 * // in your mod's initializer, once:
 * REG.register();
 * }</pre>
 *
 * <p>Call {@link #register()} from your mod's constructor / initializer (it must run while the mod is loading).
 * Values are created lazily, in the order the registries are filled, so a factory may use {@code get()} of
 * entries of registries that are filled earlier (blocks before items, blocks before block entities, ...).
 */
public final class Registrar {
    /** One declared entry, as seen by the loader code. */
    public static final class Entry<T> {
        private final ResourceKey<? extends Registry<T>> registry;
        private final Registered<T> holder;

        Entry(ResourceKey<? extends Registry<T>> registry, Registered<T> holder) {
            this.registry = registry;
            this.holder = holder;
        }

        public ResourceKey<? extends Registry<T>> registryKey() {
            return registry;
        }

        public ResourceLocation id() {
            return holder.id();
        }

        /** Creates the value (once) and returns it for registration. */
        public T create() {
            return holder.isPresent() ? holder.get() : holder.create();
        }
    }

    private final String modId;
    private final List<Entry<?>> entries = new ArrayList<>();
    private boolean registered;

    private Registrar(String modId) {
        this.modId = modId;
    }

    public static Registrar of(String modId) {
        return new Registrar(modId);
    }

    public String modId() {
        return modId;
    }

    public List<Entry<?>> entries() {
        return List.copyOf(entries);
    }

    /** Declares an entry of any registry. */
    public <T> Registered<T> register(ResourceKey<? extends Registry<T>> registry, String name, Supplier<? extends T> factory) {
        if (registered) {
            throw new IllegalStateException("Registrar of '" + modId + "' was already registered; declare entries before register()");
        }
        ResourceLocation id = com.dougg865.bloomberto.Bloomberto.id(modId, name);
        for (Entry<?> e : entries) {
            if (e.registryKey().equals(registry) && e.id().equals(id)) {
                throw new IllegalStateException("Duplicate registry entry " + id + " in " + registry.location());
            }
        }
        Registered<T> holder = new Registered<>(id, factory);
        entries.add(new Entry<>(registry, holder));
        return holder;
    }

    public Registered<Item> item(String name, Supplier<? extends Item> factory) {
        return register(Registries.ITEM, name, factory);
    }

    public Registered<Block> block(String name, Supplier<? extends Block> factory) {
        return register(Registries.BLOCK, name, factory);
    }

    /** Registers a plain {@link BlockItem} for a block declared with {@link #block}, under the same name. */
    public Registered<Item> blockItem(String name, Registered<? extends Block> block, Supplier<Item.Properties> properties) {
        return item(name, () -> new BlockItem(block.get(), properties.get()));
    }

    public Registered<Item> blockItem(String name, Registered<? extends Block> block) {
        return blockItem(name, block, Item.Properties::new);
    }

    public <E extends net.minecraft.world.entity.Entity> Registered<EntityType<?>> entityType(
            String name, Supplier<EntityType<E>> factory) {
        return register(Registries.ENTITY_TYPE, name, factory);
    }

    public <B extends net.minecraft.world.level.block.entity.BlockEntity> Registered<BlockEntityType<?>> blockEntityType(
            String name, Supplier<BlockEntityType<B>> factory) {
        return register(Registries.BLOCK_ENTITY_TYPE, name, factory);
    }

    /** Registers a sound event whose id is the entry's own id. */
    public Registered<SoundEvent> sound(String name) {
        ResourceLocation id = com.dougg865.bloomberto.Bloomberto.id(modId, name);
        return register(Registries.SOUND_EVENT, name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public Registered<MobEffect> mobEffect(String name, Supplier<? extends MobEffect> factory) {
        return register(Registries.MOB_EFFECT, name, factory);
    }

    public Registered<Attribute> attribute(String name, Supplier<? extends Attribute> factory) {
        return register(Registries.ATTRIBUTE, name, factory);
    }

    /**
     * Registers a creative tab.
     *
     * @param title the tab title
     * @param icon  the icon item (created lazily)
     * @param items the items shown in the tab, as holders declared in this registrar
     */
    public Registered<CreativeModeTab> creativeTab(String name, Component title, Supplier<net.minecraft.world.item.ItemStack> icon,
                                                   List<? extends Supplier<? extends Item>> items) {
        return register(Registries.CREATIVE_MODE_TAB, name, () -> Services.REGISTRY.creativeTabBuilder()
                .title(title)
                .icon(icon)
                .displayItems((parameters, output) -> items.forEach(item -> output.accept(item.get())))
                .build());
    }

    /** Hands the declared entries to the loader. Call once, from the mod initializer. */
    public void register() {
        if (registered) {
            throw new IllegalStateException("Registrar of '" + modId + "' was already registered");
        }
        registered = true;
        Services.REGISTRY.register(this);
    }
}
