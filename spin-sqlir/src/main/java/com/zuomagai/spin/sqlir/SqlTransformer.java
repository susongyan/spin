package com.zuomagai.spin.sqlir;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class SqlTransformer {
    public SqlNode transform(SqlNode node) {
        if (node == null) {
            return null;
        }
        if (node instanceof QueryStmt stmt) {
            return transformQueryStmt(stmt);
        }
        if (node instanceof InsertStmt stmt) {
            return transformInsertStmt(stmt);
        }
        if (node instanceof UpdateStmt stmt) {
            return transformUpdateStmt(stmt);
        }
        if (node instanceof DeleteStmt stmt) {
            return transformDeleteStmt(stmt);
        }
        if (node instanceof SelectBody body) {
            return transformSelectBody(body);
        }
        if (node instanceof SetOpBody body) {
            return transformSetOpBody(body);
        }
        if (node instanceof NamedTable table) {
            return transformNamedTable(table);
        }
        if (node instanceof DerivedTable table) {
            return transformDerivedTable(table);
        }
        if (node instanceof Join join) {
            return transformJoin(join);
        }
        if (node instanceof Id id) {
            return transformId(id);
        }
        if (node instanceof Binary binary) {
            return transformBinary(binary);
        }
        if (node instanceof Unary unary) {
            return transformUnary(unary);
        }
        if (node instanceof FuncCall call) {
            return transformFuncCall(call);
        }
        if (node instanceof Param param) {
            return transformParam(param);
        }
        if (node instanceof InList inList) {
            return transformInList(inList);
        }
        if (node instanceof Exists exists) {
            return transformExists(exists);
        }
        if (node instanceof CaseExpr expr) {
            return transformCaseExpr(expr);
        }
        if (node instanceof Literal literal) {
            return transformLiteral(literal);
        }
        if (node instanceof Star star) {
            return transformStar(star);
        }
        if (node instanceof SelectItem item) {
            return transformSelectItem(item);
        }
        if (node instanceof GroupBy groupBy) {
            return transformGroupBy(groupBy);
        }
        if (node instanceof OrderBy orderBy) {
            return transformOrderBy(orderBy);
        }
        if (node instanceof OrderByItem item) {
            return transformOrderByItem(item);
        }
        if (node instanceof LimitOffset limitOffset) {
            return transformLimitOffset(limitOffset);
        }
        if (node instanceof UpdateSet updateSet) {
            return transformUpdateSet(updateSet);
        }
        if (node instanceof WhenThen whenThen) {
            return transformWhenThen(whenThen);
        }
        throw new IllegalStateException("Unknown SqlNode type: " + node.getClass().getName());
    }

    public QueryStmt transformQueryStmt(QueryStmt stmt) {
        QueryBody body = transformQueryBody(stmt.body());
        OrderBy orderBy = transformOrderBy(stmt.orderBy());
        LimitOffset limitOffset = transformLimitOffset(stmt.limitOffset());
        if (body == stmt.body() && orderBy == stmt.orderBy() && limitOffset == stmt.limitOffset()) {
            return stmt;
        }
        return new QueryStmt(body, orderBy, limitOffset, stmt.meta());
    }

    public InsertStmt transformInsertStmt(InsertStmt stmt) {
        TableSource table = transformTableSource(stmt.table());
        List<Expr> columns = transformList(stmt.columns(), this::transformExpr);
        List<List<Expr>> valuesList = transformExprRows(stmt.valuesList());
        QueryStmt query = stmt.query() == null ? null : transformQueryStmt(stmt.query());
        if (table == stmt.table()
                && columns == stmt.columns()
                && valuesList == stmt.valuesList()
                && query == stmt.query()) {
            return stmt;
        }
        return new InsertStmt(table, columns, valuesList, query, stmt.meta());
    }

    public UpdateStmt transformUpdateStmt(UpdateStmt stmt) {
        TableSource table = transformTableSource(stmt.table());
        List<UpdateSet> setItems = transformList(stmt.setItems(), this::transformUpdateSet);
        Expr where = transformExpr(stmt.where());
        if (table == stmt.table() && setItems == stmt.setItems() && where == stmt.where()) {
            return stmt;
        }
        return new UpdateStmt(table, setItems, where, stmt.meta());
    }

    public DeleteStmt transformDeleteStmt(DeleteStmt stmt) {
        TableSource table = transformTableSource(stmt.table());
        Expr where = transformExpr(stmt.where());
        if (table == stmt.table() && where == stmt.where()) {
            return stmt;
        }
        return new DeleteStmt(table, where, stmt.meta());
    }

    public SelectBody transformSelectBody(SelectBody body) {
        List<SelectItem> items = transformList(body.selectItems(), this::transformSelectItem);
        TableSource from = transformTableSource(body.from());
        Expr where = transformExpr(body.where());
        GroupBy groupBy = transformGroupBy(body.groupBy());
        if (items == body.selectItems() && from == body.from()
                && where == body.where() && groupBy == body.groupBy()) {
            return body;
        }
        return new SelectBody(body.distinct(), items, from, where, groupBy, body.meta());
    }

    public SetOpBody transformSetOpBody(SetOpBody body) {
        QueryBody left = transformQueryBody(body.left());
        QueryBody right = transformQueryBody(body.right());
        if (left == body.left() && right == body.right()) {
            return body;
        }
        return new SetOpBody(left, body.op(), right, body.meta());
    }

    public NamedTable transformNamedTable(NamedTable table) {
        return table;
    }

    public DerivedTable transformDerivedTable(DerivedTable table) {
        QueryStmt subquery = transformQueryStmt(table.subquery());
        if (subquery == table.subquery()) {
            return table;
        }
        return new DerivedTable(subquery, table.alias(), table.meta());
    }

    public Join transformJoin(Join join) {
        TableSource left = transformTableSource(join.left());
        TableSource right = transformTableSource(join.right());
        Expr condition = transformExpr(join.condition());
        List<Expr> using = transformList(join.using(), this::transformExpr);
        if (left == join.left() && right == join.right()
                && condition == join.condition() && using == join.using()) {
            return join;
        }
        return new Join(left, join.type(), right, condition, using, join.natural(), join.meta());
    }

    public Id transformId(Id id) {
        return id;
    }

    public Binary transformBinary(Binary binary) {
        Expr left = transformExpr(binary.left());
        Expr right = transformExpr(binary.right());
        if (left == binary.left() && right == binary.right()) {
            return binary;
        }
        return new Binary(left, binary.operator(), right, binary.meta());
    }

    public Unary transformUnary(Unary unary) {
        Expr expr = transformExpr(unary.expr());
        if (expr == unary.expr()) {
            return unary;
        }
        return new Unary(unary.operator(), expr, unary.meta());
    }

    public FuncCall transformFuncCall(FuncCall call) {
        List<Expr> args = transformList(call.args(), this::transformExpr);
        if (args == call.args()) {
            return call;
        }
        return new FuncCall(call.name(), args, call.distinct(), call.meta());
    }

    public Param transformParam(Param param) {
        return param;
    }

    public InList transformInList(InList inList) {
        Expr expr = transformExpr(inList.expr());
        List<Expr> list = transformList(inList.list(), this::transformExpr);
        if (expr == inList.expr() && list == inList.list()) {
            return inList;
        }
        return new InList(expr, list, inList.not(), inList.meta());
    }

    public Exists transformExists(Exists exists) {
        QueryStmt subquery = transformQueryStmt(exists.subquery());
        if (subquery == exists.subquery()) {
            return exists;
        }
        return new Exists(subquery, exists.not(), exists.meta());
    }

    public CaseExpr transformCaseExpr(CaseExpr expr) {
        Expr value = transformExpr(expr.value());
        List<WhenThen> items = transformList(expr.items(), this::transformWhenThen);
        Expr elseExpr = transformExpr(expr.elseExpr());
        if (value == expr.value() && items == expr.items() && elseExpr == expr.elseExpr()) {
            return expr;
        }
        return new CaseExpr(value, items, elseExpr, expr.meta());
    }

    public Literal transformLiteral(Literal literal) {
        return literal;
    }

    public Star transformStar(Star star) {
        return star;
    }

    public SelectItem transformSelectItem(SelectItem item) {
        Expr expr = transformExpr(item.expr());
        if (expr == item.expr()) {
            return item;
        }
        return new SelectItem(expr, item.alias(), item.meta());
    }

    public GroupBy transformGroupBy(GroupBy groupBy) {
        if (groupBy == null) {
            return null;
        }
        List<Expr> items = transformList(groupBy.items(), this::transformExpr);
        Expr having = transformExpr(groupBy.having());
        if (items == groupBy.items() && having == groupBy.having()) {
            return groupBy;
        }
        return new GroupBy(items, having, groupBy.meta());
    }

    public OrderBy transformOrderBy(OrderBy orderBy) {
        if (orderBy == null) {
            return null;
        }
        List<OrderByItem> items = transformList(orderBy.items(), this::transformOrderByItem);
        if (items == orderBy.items()) {
            return orderBy;
        }
        return new OrderBy(items, orderBy.meta());
    }

    public OrderByItem transformOrderByItem(OrderByItem item) {
        Expr expr = transformExpr(item.expr());
        if (expr == item.expr()) {
            return item;
        }
        return new OrderByItem(expr, item.direction(), item.nullsOrder(), item.meta());
    }

    public LimitOffset transformLimitOffset(LimitOffset limitOffset) {
        if (limitOffset == null) {
            return null;
        }
        Expr limit = transformExpr(limitOffset.limit());
        Expr offset = transformExpr(limitOffset.offset());
        if (limit == limitOffset.limit() && offset == limitOffset.offset()) {
            return limitOffset;
        }
        return new LimitOffset(limit, offset, limitOffset.meta());
    }

    public UpdateSet transformUpdateSet(UpdateSet updateSet) {
        Expr target = transformExpr(updateSet.target());
        Expr value = transformExpr(updateSet.value());
        if (target == updateSet.target() && value == updateSet.value()) {
            return updateSet;
        }
        return new UpdateSet(target, value, updateSet.meta());
    }

    public WhenThen transformWhenThen(WhenThen whenThen) {
        Expr when = transformExpr(whenThen.when());
        Expr then = transformExpr(whenThen.then());
        if (when == whenThen.when() && then == whenThen.then()) {
            return whenThen;
        }
        return new WhenThen(when, then, whenThen.meta());
    }

    public QueryBody transformQueryBody(QueryBody body) {
        if (body == null) {
            return null;
        }
        if (body instanceof SelectBody selectBody) {
            return transformSelectBody(selectBody);
        }
        if (body instanceof SetOpBody setOpBody) {
            return transformSetOpBody(setOpBody);
        }
        throw new IllegalStateException("Unknown QueryBody type: " + body.getClass().getName());
    }

    public TableSource transformTableSource(TableSource source) {
        if (source == null) {
            return null;
        }
        if (source instanceof NamedTable table) {
            return transformNamedTable(table);
        }
        if (source instanceof DerivedTable table) {
            return transformDerivedTable(table);
        }
        if (source instanceof Join join) {
            return transformJoin(join);
        }
        throw new IllegalStateException("Unknown TableSource type: " + source.getClass().getName());
    }

    public Expr transformExpr(Expr expr) {
        if (expr == null) {
            return null;
        }
        if (expr instanceof Id id) {
            return transformId(id);
        }
        if (expr instanceof Binary binary) {
            return transformBinary(binary);
        }
        if (expr instanceof Unary unary) {
            return transformUnary(unary);
        }
        if (expr instanceof FuncCall call) {
            return transformFuncCall(call);
        }
        if (expr instanceof Param param) {
            return transformParam(param);
        }
        if (expr instanceof InList inList) {
            return transformInList(inList);
        }
        if (expr instanceof Exists exists) {
            return transformExists(exists);
        }
        if (expr instanceof CaseExpr caseExpr) {
            return transformCaseExpr(caseExpr);
        }
        if (expr instanceof Literal literal) {
            return transformLiteral(literal);
        }
        if (expr instanceof Star star) {
            return transformStar(star);
        }
        throw new IllegalStateException("Unknown Expr type: " + expr.getClass().getName());
    }

    protected <T extends SqlNode> List<T> transformList(List<T> list, Function<T, T> transformer) {
        if (list == null || list.isEmpty()) {
            return list == null ? List.of() : list;
        }
        boolean changed = false;
        List<T> out = new ArrayList<>(list.size());
        for (T item : list) {
            T next = item == null ? null : transformer.apply(item);
            if (next != item) {
                changed = true;
            }
            out.add(next);
        }
        if (!changed) {
            return list;
        }
        return List.copyOf(out);
    }

    protected List<List<Expr>> transformExprRows(List<List<Expr>> rows) {
        if (rows == null || rows.isEmpty()) {
            return rows == null ? List.of() : rows;
        }
        boolean changed = false;
        List<List<Expr>> out = new ArrayList<>(rows.size());
        for (List<Expr> row : rows) {
            List<Expr> nextRow = transformList(row, this::transformExpr);
            if (nextRow != row) {
                changed = true;
            }
            out.add(nextRow);
        }
        if (!changed) {
            return rows;
        }
        return List.copyOf(out);
    }
}
