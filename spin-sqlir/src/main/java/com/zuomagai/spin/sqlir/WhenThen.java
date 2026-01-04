package com.zuomagai.spin.sqlir;

import java.util.Objects;

public record WhenThen(Expr when, Expr then, Meta meta) implements SqlNode {
    public WhenThen {
        Objects.requireNonNull(when, "when");
        Objects.requireNonNull(then, "then");
        meta = meta == null ? Meta.empty() : meta;
    }
}
