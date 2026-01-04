package com.zuomagai.spin.sqlir;

import java.util.Objects;

public record UpdateSet(Expr target, Expr value, Meta meta) implements SqlNode {
    public UpdateSet {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(value, "value");
        meta = meta == null ? Meta.empty() : meta;
    }
}
