package com.zuomagai.spin.sqlir;

import java.util.Objects;

public record QueryStmt(QueryBody body, OrderBy orderBy, LimitOffset limitOffset, Meta meta)
        implements Statement {
    public QueryStmt {
        Objects.requireNonNull(body, "body");
        meta = meta == null ? Meta.empty() : meta;
    }
}
