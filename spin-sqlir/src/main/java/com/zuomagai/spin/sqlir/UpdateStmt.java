package com.zuomagai.spin.sqlir;

import java.util.List;
import java.util.Objects;

public record UpdateStmt(TableSource table, List<UpdateSet> setItems, Expr where, Meta meta)
        implements Statement {
    public UpdateStmt {
        Objects.requireNonNull(table, "table");
        setItems = setItems == null ? List.of() : List.copyOf(setItems);
        meta = meta == null ? Meta.empty() : meta;
    }
}
