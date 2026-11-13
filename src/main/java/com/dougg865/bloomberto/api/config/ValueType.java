package com.dougg865.bloomberto.api.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** How a config value is validated and converted to and from its TOML form. */
public interface ValueType<T> {
    /**
     * Converts a parsed TOML value to {@code T}.
     *
     * @return the (possibly corrected, e.g. clamped) value, or {@code null} if it cannot be used
     */
    T fromToml(Object raw);

    /** Converts to a TOML-compatible value ({@code Boolean}, {@code Long}, {@code Double}, {@code String} or {@code List}). */
    Object toToml(T value);

    /** Human readable constraints, written into the file as a comment; may be empty. */
    String describe();

    ValueType<Boolean> BOOLEAN = new ValueType<>() {
        @Override
        public Boolean fromToml(Object raw) {
            return raw instanceof Boolean b ? b : null;
        }

        @Override
        public Object toToml(Boolean value) {
            return value;
        }

        @Override
        public String describe() {
            return "true or false";
        }
    };

    ValueType<String> STRING = new ValueType<>() {
        @Override
        public String fromToml(Object raw) {
            return raw instanceof String s ? s : null;
        }

        @Override
        public Object toToml(String value) {
            return value;
        }

        @Override
        public String describe() {
            return "";
        }
    };

    static ValueType<Integer> intRange(int min, int max) {
        return new ValueType<>() {
            @Override
            public Integer fromToml(Object raw) {
                if (!(raw instanceof Long l)) {
                    return null;
                }
                return (int) Math.max(min, Math.min(max, l));
            }

            @Override
            public Object toToml(Integer value) {
                return (long) value;
            }

            @Override
            public String describe() {
                return "Range: " + min + " ~ " + max;
            }
        };
    }

    static ValueType<Double> doubleRange(double min, double max) {
        return new ValueType<>() {
            @Override
            public Double fromToml(Object raw) {
                double v;
                if (raw instanceof Double d) {
                    v = d;
                } else if (raw instanceof Long l) {
                    v = l;
                } else {
                    return null;
                }
                if (Double.isNaN(v)) {
                    return null;
                }
                return Math.max(min, Math.min(max, v));
            }

            @Override
            public Object toToml(Double value) {
                return value;
            }

            @Override
            public String describe() {
                return "Range: " + min + " ~ " + max;
            }
        };
    }

    static <E extends Enum<E>> ValueType<E> enumOf(Class<E> type) {
        return new ValueType<>() {
            @Override
            public E fromToml(Object raw) {
                if (!(raw instanceof String s)) {
                    return null;
                }
                for (E constant : type.getEnumConstants()) {
                    if (constant.name().equalsIgnoreCase(s.strip())) {
                        return constant;
                    }
                }
                return null;
            }

            @Override
            public Object toToml(E value) {
                return value.name().toLowerCase(Locale.ROOT);
            }

            @Override
            public String describe() {
                StringBuilder sb = new StringBuilder("Allowed: ");
                E[] constants = type.getEnumConstants();
                for (int i = 0; i < constants.length; i++) {
                    sb.append(i > 0 ? ", " : "").append(constants[i].name().toLowerCase(Locale.ROOT));
                }
                return sb.toString();
            }
        };
    }

    ValueType<List<String>> STRING_LIST = new ValueType<>() {
        @Override
        public List<String> fromToml(Object raw) {
            if (!(raw instanceof List<?> list)) {
                return null;
            }
            List<String> result = new ArrayList<>();
            for (Object o : list) {
                if (!(o instanceof String s)) {
                    return null;
                }
                result.add(s);
            }
            return List.copyOf(result);
        }

        @Override
        public Object toToml(List<String> value) {
            return value;
        }

        @Override
        public String describe() {
            return "List of strings";
        }
    };
}
