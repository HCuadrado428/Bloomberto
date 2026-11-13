package com.dougg865.bloomberto.api.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * A simple event: register listeners of type {@code L}; the platform code fires it by calling
 * {@link #invoke(Consumer)}. A listener that throws does not stop the others; the error is logged.
 */
public final class Event<L> {
    private final String name;
    private final List<L> listeners = new CopyOnWriteArrayList<>();

    public Event(String name) {
        this.name = name;
    }

    public void register(L listener) {
        listeners.add(listener);
    }

    public void unregister(L listener) {
        listeners.remove(listener);
    }

    /** Calls {@code caller} for every registered listener, in registration order. */
    public void invoke(Consumer<L> caller) {
        for (L listener : listeners) {
            try {
                caller.accept(listener);
            } catch (RuntimeException e) {
                com.dougg865.bloomberto.Bloomberto.LOGGER.error("Listener of event '{}' failed", name, e);
            }
        }
    }

    public int listenerCount() {
        return listeners.size();
    }

    @Override
    public String toString() {
        return "Event[" + name + ", " + listeners.size() + " listeners]";
    }
}
