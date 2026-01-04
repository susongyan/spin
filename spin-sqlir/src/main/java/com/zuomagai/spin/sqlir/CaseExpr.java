package com.zuomagai.spin.sqlir;

import java.util.List;

public record CaseExpr(Expr value, List<WhenThen> items, Expr elseExpr, Meta meta) implements Expr {
    public CaseExpr {
        items = items == null ? List.of() : List.copyOf(items);
        meta = meta == null ? Meta.empty() : meta;
    }
}
