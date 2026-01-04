package com.zuomagai.spin.sqlir;

import java.util.Objects;

public record DerivedTable(QueryStmt subquery, String alias, Meta meta) implements TableSource {
    public DerivedTable {
        Objects.requireNonNull(subquery, "subquery");
        meta = meta == null ? Meta.empty() : meta;
    }
}
