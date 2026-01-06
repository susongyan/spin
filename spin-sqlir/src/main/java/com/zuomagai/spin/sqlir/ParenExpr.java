package com.zuomagai.spin.sqlir;

import java.util.Objects;

public record ParenExpr(Expr expr, Meta meta) implements Expr {
    public ParenExpr {
        Objects.requireNonNull(expr, "expr");
        meta = meta == null ? Meta.empty() : meta;
    }
}
