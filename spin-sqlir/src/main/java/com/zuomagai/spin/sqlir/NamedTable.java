package com.zuomagai.spin.sqlir;

import java.util.Objects;

public record NamedTable(QName name, String alias, Meta meta) implements TableSource {
    public NamedTable {
        Objects.requireNonNull(name, "name");
        meta = meta == null ? Meta.empty() : meta;
    }
}
