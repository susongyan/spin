package com.zuomagai.spin.sqlir;

public record LimitOffset(Expr limit, Expr offset, Meta meta) implements SqlNode {
    public LimitOffset {
        meta = meta == null ? Meta.empty() : meta;
    }
}
