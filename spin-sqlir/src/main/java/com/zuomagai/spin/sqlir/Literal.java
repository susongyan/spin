package com.zuomagai.spin.sqlir;

import java.util.Objects;

public record Literal(LiteralType type, Object value, Meta meta) implements Expr {
    public Literal {
        Objects.requireNonNull(type, "type");
        meta = meta == null ? Meta.empty() : meta;
    }
}
