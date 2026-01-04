package com.zuomagai.spin.sqlir;

import java.util.List;

public record SelectBody(boolean distinct, List<SelectItem> selectItems, TableSource from,
                         Expr where, GroupBy groupBy, Meta meta) implements QueryBody {
    public SelectBody {
        selectItems = selectItems == null ? List.of() : List.copyOf(selectItems);
        meta = meta == null ? Meta.empty() : meta;
    }
}
