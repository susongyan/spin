package com.zuomagai.spin.sqlir;

public sealed interface Expr extends SqlNode
        permits Id, Binary, Unary, FuncCall, Param, InList, Exists, CaseExpr, Literal, Star {
}
