package com.zuomagai.spin.sqlir;

public sealed interface Statement extends SqlNode
        permits QueryStmt, InsertStmt, UpdateStmt, DeleteStmt {
}
