package com.dougg865.bloomberto.api.registry;

import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;

/** A registry entry declared with {@link Registrar}; its value exists once registration has happened. */
public final class Registered<T> implements Supplier<T> {
    private final ResourceLocation id;
    private final Supplier<? extends T> factory;
    private volatile T value;

    Registered(ResourceLocation id, Supplier<? extends T> factory) {
        this.id = id;
        this.factory = factory;
    }

    public ResourceLocation id() {
        return id;
    }

    /** @return {@code true} once the value has been created and registered */
    public boolean isPresent() {
        return value != null;
    }

    /** @throws IllegalStateException if called before the registry was filled */
    @Override
    public T get() {
        T v = value;
        if (v == null) {
            throw new IllegalStateException("Registry entry " + id + " is not registered yet");
        }
        return v;
    }

    /** Creates the value; called by the loader code when its registry is ready. */
    T create() {
        T v = factory.get();
        if (v == null) {
            throw new IllegalStateException("Factory of " + id + " returned null");
        }
        this.value = v;
        return v;
    }
}
