package com.zuomagai.spin.sqlir;

import java.util.List;
import java.util.Objects;

public record InList(Expr expr, List<Expr> list, boolean not, Meta meta) implements Expr {
    public InList {
        Objects.requireNonNull(expr, "expr");
        list = list == null ? List.of() : List.copyOf(list);
        meta = meta == null ? Meta.empty() : meta;
    }
}
