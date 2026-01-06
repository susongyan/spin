package com.zuomagai.spin.sqlir;

public sealed interface Expr extends SqlNode
        permits Id, Binary, Unary, ParenExpr, FuncCall, Param, InList, Exists, CaseExpr, Literal, Star {
}
