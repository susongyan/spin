package com.zuomagai.spin.sqlir;

import java.util.List;
import java.util.Objects;

public record FuncCall(String name, List<Expr> args, boolean distinct, Meta meta) implements Expr {
    public FuncCall {
        Objects.requireNonNull(name, "name");
        args = args == null ? List.of() : List.copyOf(args);
        meta = meta == null ? Meta.empty() : meta;
    }
}
