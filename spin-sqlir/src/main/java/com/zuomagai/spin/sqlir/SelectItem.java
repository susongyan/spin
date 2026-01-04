package com.zuomagai.spin.sqlir;

import java.util.Objects;

public record SelectItem(Expr expr, String alias, Meta meta) implements SqlNode {
    public SelectItem {
        Objects.requireNonNull(expr, "expr");
        meta = meta == null ? Meta.empty() : meta;
    }
}
