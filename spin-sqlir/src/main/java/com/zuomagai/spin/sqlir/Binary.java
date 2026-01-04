package com.zuomagai.spin.sqlir;

import java.util.Objects;

public record Binary(Expr left, String operator, Expr right, Meta meta) implements Expr {
    public Binary {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(operator, "operator");
        Objects.requireNonNull(right, "right");
        meta = meta == null ? Meta.empty() : meta;
    }
}
