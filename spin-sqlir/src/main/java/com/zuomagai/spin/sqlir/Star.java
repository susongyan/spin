package com.zuomagai.spin.sqlir;

public record Star(QName qualifier, Meta meta) implements Expr {
    public Star {
        meta = meta == null ? Meta.empty() : meta;
    }
}
