package com.dougg865.bloomberto.impl.render;

import java.util.LinkedHashMap;
import java.util.Map;
import org.joml.Matrix4f;

/** The typed values set on a shader program, kept until they can be applied to the real GPU program. */
public final class UniformStore {
    /** Receives the stored values; the real implementation writes them to a {@code ShaderInstance}. */
    public interface Target {
        void setFloats(String name, float... values);

        void setInt(String name, int value);

        void setMatrix(String name, Matrix4f matrix);
    }

    private sealed interface Value permits Floats, Int, Matrix {
        void apply(String name, Target target);
    }

    private record Floats(float[] values) implements Value {
        @Override
        public void apply(String name, Target target) {
            target.setFloats(name, values);
        }
    }

    private record Int(int value) implements Value {
        @Override
        public void apply(String name, Target target) {
            target.setInt(name, value);
        }
    }

    private record Matrix(Matrix4f matrix) implements Value {
        @Override
        public void apply(String name, Target target) {
            target.setMatrix(name, matrix);
        }
    }

    private final Map<String, Value> values = new LinkedHashMap<>();

    public synchronized void setFloat(String name, float v) {
        values.put(name, new Floats(new float[] {v}));
    }

    public synchronized void setVec2(String name, float x, float y) {
        values.put(name, new Floats(new float[] {x, y}));
    }

    public synchronized void setVec3(String name, float x, float y, float z) {
        values.put(name, new Floats(new float[] {x, y, z}));
    }

    public synchronized void setVec4(String name, float x, float y, float z, float w) {
        values.put(name, new Floats(new float[] {x, y, z, w}));
    }

    public synchronized void setInt(String name, int v) {
        values.put(name, new Int(v));
    }

    public synchronized void setMatrix(String name, Matrix4f m) {
        values.put(name, new Matrix(new Matrix4f(m)));
    }

    public synchronized boolean has(String name) {
        return values.containsKey(name);
    }

    public synchronized int size() {
        return values.size();
    }

    /** Writes every stored value to {@code target}, in the order they were first set. */
    public synchronized void applyTo(Target target) {
        values.forEach((name, value) -> value.apply(name, target));
    }
}
