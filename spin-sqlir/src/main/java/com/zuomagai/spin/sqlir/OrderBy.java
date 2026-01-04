package com.zuomagai.spin.sqlir;

import java.util.List;

public record OrderBy(List<OrderByItem> items, Meta meta) implements SqlNode {
    public OrderBy {
        items = items == null ? List.of() : List.copyOf(items);
        meta = meta == null ? Meta.empty() : meta;
    }
}
