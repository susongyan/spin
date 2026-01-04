package com.zuomagai.spin.sqlir;

import java.util.Objects;

public record Id(QName name, Meta meta) implements Expr {
    public Id {
        Objects.requireNonNull(name, "name");
        meta = meta == null ? Meta.empty() : meta;
    }
}
