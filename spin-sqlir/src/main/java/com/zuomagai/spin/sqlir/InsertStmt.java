package com.zuomagai.spin.sqlir;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record InsertStmt(TableSource table, List<Expr> columns, List<List<Expr>> valuesList,
                         QueryStmt query, Meta meta) implements Statement {
    public InsertStmt {
        Objects.requireNonNull(table, "table");
        columns = columns == null ? List.of() : List.copyOf(columns);
        if (valuesList == null) {
            valuesList = List.of();
        } else {
            List<List<Expr>> rows = new ArrayList<>(valuesList.size());
            for (List<Expr> row : valuesList) {
                rows.add(row == null ? List.of() : List.copyOf(row));
            }
            valuesList = List.copyOf(rows);
        }
        meta = meta == null ? Meta.empty() : meta;
    }
}
