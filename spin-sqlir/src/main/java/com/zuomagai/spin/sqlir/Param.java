package com.zuomagai.spin.sqlir;

public record Param(String name, Integer index, Meta meta) implements Expr {
    public Param {
        meta = meta == null ? Meta.empty() : meta;
    }
}
