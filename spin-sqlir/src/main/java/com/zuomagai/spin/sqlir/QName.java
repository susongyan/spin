package com.zuomagai.spin.sqlir;

import java.util.List;

public record QName(List<String> parts) {
    public QName {
        parts = parts == null ? List.of() : List.copyOf(parts);
    }

    public String last() {
        if (parts.isEmpty()) {
            return "";
        }
        return parts.get(parts.size() - 1);
    }
}
