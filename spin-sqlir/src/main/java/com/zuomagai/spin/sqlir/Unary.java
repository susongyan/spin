package com.zuomagai.spin.sqlir;

import java.util.Objects;

public record Unary(String operator, Expr expr, Meta meta) implements Expr {
    public Unary {
        Objects.requireNonNull(operator, "operator");
        Objects.requireNonNull(expr, "expr");
        meta = meta == null ? Meta.empty() : meta;
    }
}
