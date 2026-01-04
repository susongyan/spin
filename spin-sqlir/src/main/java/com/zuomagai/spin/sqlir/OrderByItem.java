package com.zuomagai.spin.sqlir;

import java.util.Objects;

public record OrderByItem(Expr expr, OrderDirection direction, NullsOrder nullsOrder, Meta meta)
        implements SqlNode {
    public OrderByItem {
        Objects.requireNonNull(expr, "expr");
        direction = direction == null ? OrderDirection.UNSPECIFIED : direction;
        meta = meta == null ? Meta.empty() : meta;
    }
}
