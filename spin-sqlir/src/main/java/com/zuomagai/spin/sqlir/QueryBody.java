package com.zuomagai.spin.sqlir;

public sealed interface QueryBody extends SqlNode
        permits SelectBody, SetOpBody {
}
