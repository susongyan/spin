package com.zuomagai.spin.sqlir;

public interface SqlVisitor<R> {
    R visitQueryStmt(QueryStmt stmt);

    R visitInsertStmt(InsertStmt stmt);

    R visitUpdateStmt(UpdateStmt stmt);

    R visitDeleteStmt(DeleteStmt stmt);

    R visitSelectBody(SelectBody body);

    R visitSetOpBody(SetOpBody body);

    R visitNamedTable(NamedTable table);

    R visitDerivedTable(DerivedTable table);

    R visitJoin(Join join);

    R visitId(Id id);

    R visitBinary(Binary binary);

    R visitUnary(Unary unary);

    R visitParenExpr(ParenExpr expr);

    R visitFuncCall(FuncCall call);

    R visitParam(Param param);

    R visitInList(InList inList);

    R visitExists(Exists exists);

    R visitCaseExpr(CaseExpr expr);

    R visitLiteral(Literal literal);

    R visitStar(Star star);

    R visitSelectItem(SelectItem item);

    R visitGroupBy(GroupBy groupBy);

    R visitOrderBy(OrderBy orderBy);

    R visitOrderByItem(OrderByItem item);

    R visitLimitOffset(LimitOffset limitOffset);

    R visitUpdateSet(UpdateSet updateSet);

    R visitWhenThen(WhenThen whenThen);
}
