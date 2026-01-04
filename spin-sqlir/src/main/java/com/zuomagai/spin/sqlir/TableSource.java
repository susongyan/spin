package com.zuomagai.spin.sqlir;

public sealed interface TableSource extends SqlNode
        permits NamedTable, DerivedTable, Join {
}
