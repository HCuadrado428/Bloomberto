package com.dougg865.bloomberto.api.animation.model;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A map whose keys are compared by identity and held weakly: an entry disappears once nothing else references its key.
 * Used to cache data per model object without the cost of record {@code equals}/{@code hashCode} and without leaking old
 * models after a resource reload. Not thread-safe; wrap it with {@code Collections.synchronizedMap}.
 */
final class IdentityWeakMap<K, V> extends AbstractMap<K, V> {
    private static final class Key<K> extends WeakReference<K> {
        private final int hash;

        Key(K key, ReferenceQueue<K> queue) {
            super(key, queue);
            this.hash = System.identityHashCode(key);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Key<?> other)) {
                return false;
            }
            Object a = get();
            return a != null && a == other.get();
        }
    }

    /** A lookup key that is not registered with the queue. */

    private final Map<Key<K>, V> map = new HashMap<>();
    private final ReferenceQueue<K> queue = new ReferenceQueue<>();

    private void purge() {
        for (Object ref; (ref = queue.poll()) != null; ) {
            map.remove(ref);
        }
    }

    @Override
    public V get(Object key) {
        purge();
        if (key == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Key<K> probe = new Key<>((K) key, null);
        V value = map.get(probe);
        probe.clear();
        return value;
    }

    @Override
    public V put(K key, V value) {
        purge();
        return map.put(new Key<>(key, queue), value);
    }

    @Override
    public int size() {
        purge();
        return map.size();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException("not needed");
    }
}
