package com.dougg865.bloomberto.api.data;

import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/** Converts a value to and from NBT. {@link #read} returns {@code null} for malformed data. */
public interface DataSerializer<T> {
    Tag write(T value);

    T read(Tag tag);

    DataSerializer<Integer> INT = new DataSerializer<>() {
        @Override
        public Tag write(Integer value) {
            return IntTag.valueOf(value);
        }

        @Override
        public Integer read(Tag tag) {
            return tag instanceof NumericTag n ? n.getAsInt() : null;
        }
    };

    DataSerializer<Double> DOUBLE = new DataSerializer<>() {
        @Override
        public Tag write(Double value) {
            return DoubleTag.valueOf(value);
        }

        @Override
        public Double read(Tag tag) {
            return tag instanceof NumericTag n ? n.getAsDouble() : null;
        }
    };

    DataSerializer<Boolean> BOOLEAN = new DataSerializer<>() {
        @Override
        public Tag write(Boolean value) {
            return ByteTag.valueOf(value);
        }

        @Override
        public Boolean read(Tag tag) {
            return tag instanceof NumericTag n ? n.getAsByte() != 0 : null;
        }
    };

    DataSerializer<String> STRING = new DataSerializer<>() {
        @Override
        public Tag write(String value) {
            return StringTag.valueOf(value);
        }

        @Override
        public String read(Tag tag) {
            return tag instanceof StringTag s ? s.getAsString() : null;
        }
    };
}
