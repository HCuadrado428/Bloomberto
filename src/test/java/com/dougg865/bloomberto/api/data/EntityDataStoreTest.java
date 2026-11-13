package com.dougg865.bloomberto.api.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dougg865.bloomberto.Bloomberto;
import com.dougg865.bloomberto.impl.data.EntityDataStore;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;

class EntityDataStoreTest {
    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();

    @Test
    void setGetRemoveClear() {
        EntityDataStore store = new EntityDataStore();
        assertNull(store.get(a, "k"));
        store.set(a, "k", IntTag.valueOf(5));
        store.set(a, "k2", StringTag.valueOf("x"));
        store.set(b, "k", IntTag.valueOf(7));
        assertEquals(IntTag.valueOf(5), store.get(a, "k"));
        assertEquals(2, store.entityCount());
        assertTrue(store.remove(a, "k"));
        assertFalse(store.remove(a, "k"));
        assertNull(store.get(a, "k"));
        assertTrue(store.clear(a));
        assertFalse(store.clear(a));
        assertEquals(1, store.entityCount());
    }

    @Test
    void removingTheLastKeyDropsTheEntity() {
        EntityDataStore store = new EntityDataStore();
        store.set(a, "k", IntTag.valueOf(1));
        store.remove(a, "k");
        assertEquals(0, store.entityCount());
    }

    @Test
    void survivesSerialization() {
        EntityDataStore store = new EntityDataStore();
        store.set(a, "mymod:mana", IntTag.valueOf(42));
        store.set(b, "mymod:name", StringTag.valueOf("Doug"));
        EntityDataStore loaded = EntityDataStore.fromTag(store.toTag());
        assertEquals(IntTag.valueOf(42), loaded.get(a, "mymod:mana"));
        assertEquals(StringTag.valueOf("Doug"), loaded.get(b, "mymod:name"));
    }

    @Test
    void serializedFormIsACopy() {
        EntityDataStore store = new EntityDataStore();
        store.set(a, "k", IntTag.valueOf(1));
        CompoundTag tag = store.toTag();
        store.set(a, "k", IntTag.valueOf(2));
        assertEquals(IntTag.valueOf(1), EntityDataStore.fromTag(tag).get(a, "k"));
    }

    @Test
    void loadingIgnoresGarbage() {
        CompoundTag root = new CompoundTag();
        root.put("not-a-uuid", new CompoundTag());
        root.putInt(a.toString(), 5);
        root.put(b.toString(), new CompoundTag());
        assertEquals(0, EntityDataStore.fromTag(root).entityCount());
    }

    @Test
    void serializersRoundTripAndRejectWrongTags() {
        assertEquals(9, DataSerializer.INT.read(DataSerializer.INT.write(9)));
        assertEquals(2.5, DataSerializer.DOUBLE.read(DataSerializer.DOUBLE.write(2.5)));
        assertEquals(true, DataSerializer.BOOLEAN.read(DataSerializer.BOOLEAN.write(true)));
        assertEquals("hi", DataSerializer.STRING.read(DataSerializer.STRING.write("hi")));
        assertNull(DataSerializer.INT.read(StringTag.valueOf("5")));
        assertNull(DataSerializer.STRING.read(IntTag.valueOf(5)));
    }

    @Test
    void keysAreRegisteredOnceAndHaveFreshDefaults() {
        DataKey<Integer> key = DataKey.ofInt(Bloomberto.id("test", "unique_key"), 3).synced();
        assertEquals(3, key.defaultValue());
        assertTrue(key.isSynced());
        assertEquals(key, DataKey.byId(key.id()));
        assertThrows(IllegalStateException.class, () -> DataKey.ofInt(key.id(), 1));
        DataKey<StringBuilder> mutable = DataKey.of(Bloomberto.id("test", "mutable_key"),
                new DataSerializer<>() {
                    @Override
                    public net.minecraft.nbt.Tag write(StringBuilder v) {
                        return StringTag.valueOf(v.toString());
                    }

                    @Override
                    public StringBuilder read(net.minecraft.nbt.Tag t) {
                        return new StringBuilder(t.getAsString());
                    }
                }, StringBuilder::new);
        assertTrue(mutable.defaultValue() != mutable.defaultValue());
    }
}
