package com.dougg865.bloomberto.api.render;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dougg865.bloomberto.impl.render.UniformStore;
import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

class UniformStoreTest {
    private static final class Recorder implements UniformStore.Target {
        final List<String> log = new ArrayList<>();
        Matrix4f lastMatrix;
        float[] lastFloats;

        @Override
        public void setFloats(String name, float... values) {
            lastFloats = values;
            log.add(name + "=f" + values.length);
        }

        @Override
        public void setInt(String name, int value) {
            log.add(name + "=i" + value);
        }

        @Override
        public void setMatrix(String name, Matrix4f matrix) {
            lastMatrix = matrix;
            log.add(name + "=m");
        }
    }

    @Test
    void appliesTypedValuesInOrder() {
        UniformStore store = new UniformStore();
        store.setFloat("A", 1f);
        store.setVec2("B", 1f, 2f);
        store.setVec3("C", 1f, 2f, 3f);
        store.setVec4("D", 1f, 2f, 3f, 4f);
        store.setInt("E", 7);
        store.setMatrix("F", new Matrix4f().identity());
        Recorder r = new Recorder();
        store.applyTo(r);
        assertEquals(List.of("A=f1", "B=f2", "C=f3", "D=f4", "E=i7", "F=m"), r.log);
        assertEquals(6, store.size());
    }

    @Test
    void settingAgainReplacesTheValueAndKeepsItsPosition() {
        UniformStore store = new UniformStore();
        store.setFloat("A", 1f);
        store.setInt("B", 2);
        store.setVec2("A", 5f, 6f);
        Recorder r = new Recorder();
        store.applyTo(r);
        assertEquals(List.of("A=f2", "B=i2"), r.log);
        assertArrayEquals(new float[] {5f, 6f}, new float[] {5f, 6f});
        assertTrue(store.has("A"));
        assertFalse(store.has("Z"));
    }

    @Test
    void matricesAreCopiedAtSetTime() {
        UniformStore store = new UniformStore();
        Matrix4f m = new Matrix4f().identity();
        store.setMatrix("M", m);
        m.scale(5f);
        Recorder r = new Recorder();
        store.applyTo(r);
        assertEquals(new Matrix4f().identity(), r.lastMatrix);
    }

    @Test
    void valuesCanBeAppliedRepeatedly() {
        UniformStore store = new UniformStore();
        store.setFloat("A", 3f);
        Recorder r = new Recorder();
        store.applyTo(r);
        store.applyTo(r);
        assertEquals(2, r.log.size());
        assertEquals(3f, r.lastFloats[0]);
    }
}
