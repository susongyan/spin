package com.zuomagai.spin.sqlir;

public sealed interface SqlNode permits Statement, QueryBody, TableSource, Expr, SelectItem,
        GroupBy, OrderBy, OrderByItem, LimitOffset, UpdateSet, WhenThen {
    Meta meta();

    default <R> R accept(SqlVisitor<R> visitor) {
        if (this instanceof QueryStmt node) {
            return visitor.visitQueryStmt(node);
        }
        if (this instanceof InsertStmt node) {
            return visitor.visitInsertStmt(node);
        }
        if (this instanceof UpdateStmt node) {
            return visitor.visitUpdateStmt(node);
        }
        if (this instanceof DeleteStmt node) {
            return visitor.visitDeleteStmt(node);
        }
        if (this instanceof SelectBody node) {
            return visitor.visitSelectBody(node);
        }
        if (this instanceof SetOpBody node) {
            return visitor.visitSetOpBody(node);
        }
        if (this instanceof NamedTable node) {
            return visitor.visitNamedTable(node);
        }
        if (this instanceof DerivedTable node) {
            return visitor.visitDerivedTable(node);
        }
        if (this instanceof Join node) {
            return visitor.visitJoin(node);
        }
        if (this instanceof Id node) {
            return visitor.visitId(node);
        }
        if (this instanceof Binary node) {
            return visitor.visitBinary(node);
        }
        if (this instanceof Unary node) {
            return visitor.visitUnary(node);
        }
        if (this instanceof FuncCall node) {
            return visitor.visitFuncCall(node);
        }
        if (this instanceof Param node) {
            return visitor.visitParam(node);
        }
        if (this instanceof InList node) {
            return visitor.visitInList(node);
        }
        if (this instanceof Exists node) {
            return visitor.visitExists(node);
        }
        if (this instanceof CaseExpr node) {
            return visitor.visitCaseExpr(node);
        }
        if (this instanceof Literal node) {
            return visitor.visitLiteral(node);
        }
        if (this instanceof Star node) {
            return visitor.visitStar(node);
        }
        if (this instanceof SelectItem node) {
            return visitor.visitSelectItem(node);
        }
        if (this instanceof GroupBy node) {
            return visitor.visitGroupBy(node);
        }
        if (this instanceof OrderBy node) {
            return visitor.visitOrderBy(node);
        }
        if (this instanceof OrderByItem node) {
            return visitor.visitOrderByItem(node);
        }
        if (this instanceof LimitOffset node) {
            return visitor.visitLimitOffset(node);
        }
        if (this instanceof UpdateSet node) {
            return visitor.visitUpdateSet(node);
        }
        if (this instanceof WhenThen node) {
            return visitor.visitWhenThen(node);
        }
        throw new IllegalStateException("Unknown SqlNode type: " + getClass().getName());
    }
}
