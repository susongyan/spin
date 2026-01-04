package com.zuomagai.spin.sqlir;

import java.util.Objects;

public record Exists(QueryStmt subquery, boolean not, Meta meta) implements Expr {
    public Exists {
        Objects.requireNonNull(subquery, "subquery");
        meta = meta == null ? Meta.empty() : meta;
    }
}
