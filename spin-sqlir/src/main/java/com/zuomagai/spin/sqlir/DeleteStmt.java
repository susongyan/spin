package com.zuomagai.spin.sqlir;

import java.util.Objects;

public record DeleteStmt(TableSource table, Expr where, Meta meta) implements Statement {
    public DeleteStmt {
        Objects.requireNonNull(table, "table");
        meta = meta == null ? Meta.empty() : meta;
    }
}
