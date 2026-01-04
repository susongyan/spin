package com.zuomagai.spin.sqlir;

import java.util.List;

public record GroupBy(List<Expr> items, Expr having, Meta meta) implements SqlNode {
    public GroupBy {
        items = items == null ? List.of() : List.copyOf(items);
        meta = meta == null ? Meta.empty() : meta;
    }
}
