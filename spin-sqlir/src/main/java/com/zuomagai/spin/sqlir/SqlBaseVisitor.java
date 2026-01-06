package com.zuomagai.spin.sqlir;

import java.util.List;

public abstract class SqlBaseVisitor<R> implements SqlVisitor<R> {
    protected R defaultResult() {
        return null;
    }

    protected R aggregateResult(R aggregate, R nextResult) {
        return nextResult != null ? nextResult : aggregate;
    }

    protected R visitChild(SqlNode node) {
        return node == null ? defaultResult() : node.accept(this);
    }

    protected R visitChildren(List<? extends SqlNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return defaultResult();
        }
        R result = defaultResult();
        for (SqlNode node : nodes) {
            result = aggregateResult(result, visitChild(node));
        }
        return result;
    }

    @Override
    public R visitQueryStmt(QueryStmt stmt) {
        R result = defaultResult();
        result = aggregateResult(result, visitChild(stmt.body()));
        result = aggregateResult(result, visitChild(stmt.orderBy()));
        result = aggregateResult(result, visitChild(stmt.limitOffset()));
        return result;
    }

    @Override
    public R visitInsertStmt(InsertStmt stmt) {
        R result = defaultResult();
        result = aggregateResult(result, visitChild(stmt.table()));
        for (Expr column : stmt.columns()) {
            result = aggregateResult(result, visitChild(column));
        }
        for (List<Expr> row : stmt.valuesList()) {
            for (Expr expr : row) {
                result = aggregateResult(result, visitChild(expr));
            }
        }
        result = aggregateResult(result, visitChild(stmt.query()));
        return result;
    }

    @Override
    public R visitUpdateStmt(UpdateStmt stmt) {
        R result = defaultResult();
        result = aggregateResult(result, visitChild(stmt.table()));
        result = aggregateResult(result, visitChildren(stmt.setItems()));
        result = aggregateResult(result, visitChild(stmt.where()));
        return result;
    }

    @Override
    public R visitDeleteStmt(DeleteStmt stmt) {
        R result = defaultResult();
        result = aggregateResult(result, visitChild(stmt.table()));
        result = aggregateResult(result, visitChild(stmt.where()));
        return result;
    }

    @Override
    public R visitSelectBody(SelectBody body) {
        R result = defaultResult();
        result = aggregateResult(result, visitChildren(body.selectItems()));
        result = aggregateResult(result, visitChild(body.from()));
        result = aggregateResult(result, visitChild(body.where()));
        result = aggregateResult(result, visitChild(body.groupBy()));
        return result;
    }

    @Override
    public R visitSetOpBody(SetOpBody body) {
        R result = defaultResult();
        result = aggregateResult(result, visitChild(body.left()));
        result = aggregateResult(result, visitChild(body.right()));
        return result;
    }

    @Override
    public R visitNamedTable(NamedTable table) {
        return defaultResult();
    }

    @Override
    public R visitDerivedTable(DerivedTable table) {
        return visitChild(table.subquery());
    }

    @Override
    public R visitJoin(Join join) {
        R result = defaultResult();
        result = aggregateResult(result, visitChild(join.left()));
        result = aggregateResult(result, visitChild(join.right()));
        result = aggregateResult(result, visitChild(join.condition()));
        result = aggregateResult(result, visitChildren(join.using()));
        return result;
    }

    @Override
    public R visitId(Id id) {
        return defaultResult();
    }

    @Override
    public R visitBinary(Binary binary) {
        R result = defaultResult();
        result = aggregateResult(result, visitChild(binary.left()));
        result = aggregateResult(result, visitChild(binary.right()));
        return result;
    }

    @Override
    public R visitUnary(Unary unary) {
        return visitChild(unary.expr());
    }

    @Override
    public R visitParenExpr(ParenExpr expr) {
        return visitChild(expr.expr());
    }

    @Override
    public R visitFuncCall(FuncCall call) {
        return visitChildren(call.args());
    }

    @Override
    public R visitParam(Param param) {
        return defaultResult();
    }

    @Override
    public R visitInList(InList inList) {
        R result = defaultResult();
        result = aggregateResult(result, visitChild(inList.expr()));
        result = aggregateResult(result, visitChildren(inList.list()));
        return result;
    }

    @Override
    public R visitExists(Exists exists) {
        return visitChild(exists.subquery());
    }

    @Override
    public R visitCaseExpr(CaseExpr expr) {
        R result = defaultResult();
        result = aggregateResult(result, visitChild(expr.value()));
        result = aggregateResult(result, visitChildren(expr.items()));
        result = aggregateResult(result, visitChild(expr.elseExpr()));
        return result;
    }

    @Override
    public R visitLiteral(Literal literal) {
        return defaultResult();
    }

    @Override
    public R visitStar(Star star) {
        return defaultResult();
    }

    @Override
    public R visitSelectItem(SelectItem item) {
        return visitChild(item.expr());
    }

    @Override
    public R visitGroupBy(GroupBy groupBy) {
        R result = defaultResult();
        result = aggregateResult(result, visitChildren(groupBy.items()));
        result = aggregateResult(result, visitChild(groupBy.having()));
        return result;
    }

    @Override
    public R visitOrderBy(OrderBy orderBy) {
        return visitChildren(orderBy.items());
    }

    @Override
    public R visitOrderByItem(OrderByItem item) {
        return visitChild(item.expr());
    }

    @Override
    public R visitLimitOffset(LimitOffset limitOffset) {
        R result = defaultResult();
        result = aggregateResult(result, visitChild(limitOffset.limit()));
        result = aggregateResult(result, visitChild(limitOffset.offset()));
        return result;
    }

    @Override
    public R visitUpdateSet(UpdateSet updateSet) {
        R result = defaultResult();
        result = aggregateResult(result, visitChild(updateSet.target()));
        result = aggregateResult(result, visitChild(updateSet.value()));
        return result;
    }

    @Override
    public R visitWhenThen(WhenThen whenThen) {
        R result = defaultResult();
        result = aggregateResult(result, visitChild(whenThen.when()));
        result = aggregateResult(result, visitChild(whenThen.then()));
        return result;
    }
}
