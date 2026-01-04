package com.zuomagai.spin.sqlir;

import java.util.Objects;

public record SetOpBody(QueryBody left, SetOp op, QueryBody right, Meta meta) implements QueryBody {
    public SetOpBody {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(op, "op");
        Objects.requireNonNull(right, "right");
        meta = meta == null ? Meta.empty() : meta;
    }
}
