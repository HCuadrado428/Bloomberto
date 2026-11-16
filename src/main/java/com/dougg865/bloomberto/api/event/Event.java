package com.dougg865.bloomberto.api.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * A simple event: register listeners of type {@code L}; the platform code fires it by calling
 * {@link #invoke(Consumer)}. A listener that throws does not stop the others; the error is logged (repeats
 * of the same listener are logged at 1, 2, 4, 8, ... failures so the log does not fill up).
 */
public final class Event<L> {
    private final String name;
    private final List<L> listeners = new CopyOnWriteArrayList<>();
    private final java.util.Map<Object, Integer> failures = java.util.Collections.synchronizedMap(new java.util.IdentityHashMap<>());

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
            } catch (RuntimeException | LinkageError e) {
                // a listener that fails every tick or frame would otherwise write thousands of stack traces per minute
                int count = failures.merge(listener, 1, Integer::sum);
                if (Integer.bitCount(count) == 1) {
                    com.dougg865.bloomberto.Bloomberto.LOGGER.error("Listener of event '{}' failed ({} time(s) so far; repeats "
                            + "are logged at 1, 2, 4, 8, ...)", name, count, e);
                }
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
