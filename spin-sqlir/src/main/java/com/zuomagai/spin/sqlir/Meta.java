package com.zuomagai.spin.sqlir;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public record Meta(Map<String, Object> values) {
    public Meta {
        values = values == null ? Map.of() : Map.copyOf(values);
    }

    public static Meta empty() {
        return new Meta(Map.of());
    }

    public static Meta of(String key, Object value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        return new Meta(Map.of(key, value));
    }

    public Meta with(String key, Object value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        if (values.containsKey(key) && Objects.equals(values.get(key), value)) {
            return this;
        }
        Map<String, Object> next = new HashMap<>(values);
        next.put(key, value);
        return new Meta(next);
    }

    public Object get(String key) {
        return values.get(key);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }
}
