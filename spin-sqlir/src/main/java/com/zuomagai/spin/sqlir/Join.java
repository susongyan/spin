package com.zuomagai.spin.sqlir;

import java.util.List;
import java.util.Objects;

public record Join(TableSource left, JoinType type, TableSource right, Expr condition,
                   List<Expr> using, boolean natural, Meta meta) implements TableSource {
    public Join {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(right, "right");
        using = using == null ? List.of() : List.copyOf(using);
        meta = meta == null ? Meta.empty() : meta;
    }
}
