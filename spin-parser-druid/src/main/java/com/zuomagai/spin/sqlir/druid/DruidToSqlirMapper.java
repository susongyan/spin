package com.zuomagai.spin.sqlir.druid;

import com.alibaba.druid.sql.ast.SQLCommentHint;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLHint;
import com.alibaba.druid.sql.ast.SQLLimit;
import com.alibaba.druid.sql.ast.SQLOrderBy;
import com.alibaba.druid.sql.ast.SQLOrderingSpecification;
import com.alibaba.druid.sql.ast.expr.SQLAggregateExpr;
import com.alibaba.druid.sql.ast.expr.SQLAllColumnExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLBooleanExpr;
import com.alibaba.druid.sql.ast.expr.SQLCaseExpr;
import com.alibaba.druid.sql.ast.expr.SQLCharExpr;
import com.alibaba.druid.sql.ast.expr.SQLDateExpr;
import com.alibaba.druid.sql.ast.expr.SQLDateTimeExpr;
import com.alibaba.druid.sql.ast.expr.SQLExistsExpr;
import com.alibaba.druid.sql.ast.expr.SQLHexExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLInListExpr;
import com.alibaba.druid.sql.ast.expr.SQLIntegerExpr;
import com.alibaba.druid.sql.ast.expr.SQLMethodInvokeExpr;
import com.alibaba.druid.sql.ast.expr.SQLNCharExpr;
import com.alibaba.druid.sql.ast.expr.SQLNullExpr;
import com.alibaba.druid.sql.ast.expr.SQLNumberExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.expr.SQLUnaryExpr;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.ast.expr.SQLTimeExpr;
import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLJoinTableSource;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLSelectGroupByClause;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectOrderByItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectQuery;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLSubqueryTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.ast.statement.SQLUnionQuery;
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLUnionOperator;
import com.zuomagai.spin.sqlir.Binary;
import com.zuomagai.spin.sqlir.CaseExpr;
import com.zuomagai.spin.sqlir.DeleteStmt;
import com.zuomagai.spin.sqlir.DerivedTable;
import com.zuomagai.spin.sqlir.Exists;
import com.zuomagai.spin.sqlir.Expr;
import com.zuomagai.spin.sqlir.FuncCall;
import com.zuomagai.spin.sqlir.GroupBy;
import com.zuomagai.spin.sqlir.Id;
import com.zuomagai.spin.sqlir.InsertStmt;
import com.zuomagai.spin.sqlir.InList;
import com.zuomagai.spin.sqlir.Join;
import com.zuomagai.spin.sqlir.JoinType;
import com.zuomagai.spin.sqlir.LimitOffset;
import com.zuomagai.spin.sqlir.Literal;
import com.zuomagai.spin.sqlir.LiteralType;
import com.zuomagai.spin.sqlir.Meta;
import com.zuomagai.spin.sqlir.NamedTable;
import com.zuomagai.spin.sqlir.NullsOrder;
import com.zuomagai.spin.sqlir.OrderBy;
import com.zuomagai.spin.sqlir.OrderByItem;
import com.zuomagai.spin.sqlir.OrderDirection;
import com.zuomagai.spin.sqlir.Param;
import com.zuomagai.spin.sqlir.ParenExpr;
import com.zuomagai.spin.sqlir.QName;
import com.zuomagai.spin.sqlir.QueryBody;
import com.zuomagai.spin.sqlir.QueryStmt;
import com.zuomagai.spin.sqlir.SelectBody;
import com.zuomagai.spin.sqlir.SelectItem;
import com.zuomagai.spin.sqlir.SetOp;
import com.zuomagai.spin.sqlir.SetOpBody;
import com.zuomagai.spin.sqlir.Star;
import com.zuomagai.spin.sqlir.Statement;
import com.zuomagai.spin.sqlir.TableSource;
import com.zuomagai.spin.sqlir.Unary;
import com.zuomagai.spin.sqlir.UpdateSet;
import com.zuomagai.spin.sqlir.UpdateStmt;
import com.zuomagai.spin.sqlir.WhenThen;

import java.util.ArrayList;
import java.util.List;

public final class DruidToSqlirMapper {
    public Statement mapStatement(SQLStatement statement) {
        if (statement instanceof SQLSelectStatement selectStatement) {
            return mapSelect(selectStatement.getSelect());
        }
        if (statement instanceof SQLInsertStatement insertStatement) {
            return mapInsert(insertStatement);
        }
        if (statement instanceof SQLUpdateStatement updateStatement) {
            return mapUpdate(updateStatement);
        }
        if (statement instanceof SQLDeleteStatement deleteStatement) {
            return mapDelete(deleteStatement);
        }
        throw new IllegalArgumentException("Unsupported SQLStatement: " + statement);
    }

    public QueryStmt mapSelect(SQLSelect select) {
        QueryBody body = mapQueryBody(select.getQuery());
        OrderBy orderBy = extractOrderBy(select);
        LimitOffset limitOffset = extractLimitOffset(select);
        Meta meta = metaWithHints(select.getHints());
        return new QueryStmt(body, orderBy, limitOffset, meta);
    }

    private InsertStmt mapInsert(SQLInsertStatement statement) {
        TableSource table = mapTableSource(statement.getTableSource());
        List<Expr> columns = mapExprList(statement.getColumns());
        List<List<Expr>> valuesList = mapValuesList(statement.getValuesList());
        QueryStmt query = statement.getQuery() == null ? null : mapSelect(statement.getQuery());
        Meta meta = metaWithCommentHint(statement.getHint());
        return new InsertStmt(table, columns, valuesList, query, meta);
    }

    private UpdateStmt mapUpdate(SQLUpdateStatement statement) {
        TableSource table = mapTableSource(statement.getTableSource());
        List<UpdateSet> setItems = new ArrayList<>();
        for (SQLUpdateSetItem item : statement.getItems()) {
            setItems.add(new UpdateSet(mapExpr(item.getColumn()), mapExpr(item.getValue()), Meta.empty()));
        }
        Expr where = mapExpr(statement.getWhere());
        return new UpdateStmt(table, setItems, where, Meta.empty());
    }

    private DeleteStmt mapDelete(SQLDeleteStatement statement) {
        TableSource table = mapTableSource(statement.getTableSource());
        Expr where = mapExpr(statement.getWhere());
        return new DeleteStmt(table, where, Meta.empty());
    }

    private QueryBody mapQueryBody(SQLSelectQuery query) {
        if (query instanceof SQLSelectQueryBlock queryBlock) {
            return mapSelectBody(queryBlock);
        }
        if (query instanceof SQLUnionQuery unionQuery) {
            return mapSetOpBody(unionQuery);
        }
        throw new IllegalArgumentException("Unsupported SQLSelectQuery: " + query);
    }

    private SelectBody mapSelectBody(SQLSelectQueryBlock queryBlock) {
        List<SelectItem> items = new ArrayList<>();
        for (SQLSelectItem item : queryBlock.getSelectList()) {
            items.add(new SelectItem(mapExpr(item.getExpr()), item.getAlias(), Meta.empty()));
        }
        TableSource from = mapTableSource(queryBlock.getFrom());
        Expr where = mapExpr(queryBlock.getWhere());
        GroupBy groupBy = mapGroupBy(queryBlock.getGroupBy());
        Meta meta = metaWithHints(queryBlock.getHints());
        return new SelectBody(queryBlock.isDistinct(), items, from, where, groupBy, meta);
    }

    private SetOpBody mapSetOpBody(SQLUnionQuery unionQuery) {
        QueryBody left = mapQueryBody(unionQuery.getLeft());
        QueryBody right = mapQueryBody(unionQuery.getRight());
        SetOp op = mapSetOp(unionQuery.getOperator());
        return new SetOpBody(left, op, right, Meta.empty());
    }

    private SetOp mapSetOp(SQLUnionOperator operator) {
        if (operator == null) {
            return SetOp.UNION;
        }
        return SetOp.valueOf(operator.name());
    }

    private GroupBy mapGroupBy(SQLSelectGroupByClause groupByClause) {
        if (groupByClause == null) {
            return null;
        }
        List<Expr> items = mapExprList(groupByClause.getItems());
        Expr having = mapExpr(groupByClause.getHaving());
        Meta meta = metaWithCommentHint(groupByClause.getHint());
        return new GroupBy(items, having, meta);
    }

    private OrderBy extractOrderBy(SQLSelect select) {
        SQLSelectQuery query = select.getQuery();
        SQLOrderBy orderBy = null;
        if (query instanceof SQLSelectQueryBlock queryBlock && queryBlock.getOrderBy() != null) {
            orderBy = queryBlock.getOrderBy();
        } else if (query instanceof SQLUnionQuery unionQuery && unionQuery.getOrderBy() != null) {
            orderBy = unionQuery.getOrderBy();
        } else if (select.getOrderBy() != null) {
            orderBy = select.getOrderBy();
        }
        return orderBy == null ? null : mapOrderBy(orderBy);
    }

    private OrderBy mapOrderBy(SQLOrderBy orderBy) {
        List<OrderByItem> items = new ArrayList<>();
        for (SQLSelectOrderByItem item : orderBy.getItems()) {
            OrderDirection direction = mapOrderDirection(item.getType());
            NullsOrder nullsOrder = mapNullsOrder(item.getNullsOrderType());
            items.add(new OrderByItem(mapExpr(item.getExpr()), direction, nullsOrder, Meta.empty()));
        }
        return new OrderBy(items, Meta.empty());
    }

    private OrderDirection mapOrderDirection(SQLOrderingSpecification type) {
        if (type == null) {
            return OrderDirection.UNSPECIFIED;
        }
        return type == SQLOrderingSpecification.ASC ? OrderDirection.ASC : OrderDirection.DESC;
    }

    private NullsOrder mapNullsOrder(SQLSelectOrderByItem.NullsOrderType type) {
        if (type == null) {
            return null;
        }
        return type == SQLSelectOrderByItem.NullsOrderType.NullsFirst ? NullsOrder.FIRST : NullsOrder.LAST;
    }

    private LimitOffset extractLimitOffset(SQLSelect select) {
        SQLSelectQuery query = select.getQuery();
        LimitOffset limitOffset = extractLimitOffset(query);
        if (limitOffset != null) {
            return limitOffset;
        }
        if (select.getLimit() != null) {
            return mapLimitOffset(select.getLimit());
        }
        if (select.getOffset() != null || select.getRowCount() != null) {
            return new LimitOffset(mapExpr(select.getRowCount()), mapExpr(select.getOffset()), Meta.empty());
        }
        return null;
    }

    private LimitOffset extractLimitOffset(SQLSelectQuery query) {
        if (query instanceof SQLSelectQueryBlock queryBlock) {
            if (queryBlock.getLimit() != null) {
                return mapLimitOffset(queryBlock.getLimit());
            }
            if (queryBlock.getOffset() != null || queryBlock.getFirst() != null) {
                return new LimitOffset(mapExpr(queryBlock.getFirst()), mapExpr(queryBlock.getOffset()), Meta.empty());
            }
        } else if (query instanceof SQLUnionQuery unionQuery) {
            if (unionQuery.getLimit() != null) {
                return mapLimitOffset(unionQuery.getLimit());
            }
        }
        return null;
    }

    private LimitOffset mapLimitOffset(SQLLimit limit) {
        if (limit == null) {
            return null;
        }
        return new LimitOffset(mapExpr(limit.getRowCount()), mapExpr(limit.getOffset()), Meta.empty());
    }

    private TableSource mapTableSource(SQLTableSource tableSource) {
        if (tableSource == null) {
            return null;
        }
        if (tableSource instanceof SQLExprTableSource exprTableSource) {
            return mapNamedTable(exprTableSource);
        }
        if (tableSource instanceof SQLSubqueryTableSource subqueryTableSource) {
            return mapDerivedTable(subqueryTableSource);
        }
        if (tableSource instanceof SQLJoinTableSource joinTableSource) {
            return mapJoin(joinTableSource);
        }
        throw new IllegalArgumentException("Unsupported SQLTableSource: " + tableSource);
    }

    private NamedTable mapNamedTable(SQLExprTableSource tableSource) {
        QName name = mapQName(tableSource.getExpr());
        Meta meta = metaWithHints(tableSource.getHints());
        return new NamedTable(name, tableSource.getAlias(), meta);
    }

    private DerivedTable mapDerivedTable(SQLSubqueryTableSource tableSource) {
        QueryStmt subquery = mapSelect(tableSource.getSelect());
        return new DerivedTable(subquery, tableSource.getAlias(), Meta.empty());
    }

    private Join mapJoin(SQLJoinTableSource tableSource) {
        TableSource left = mapTableSource(tableSource.getLeft());
        TableSource right = mapTableSource(tableSource.getRight());
        JoinType type = mapJoinType(tableSource.getJoinType());
        Expr condition = mapExpr(tableSource.getCondition());
        List<Expr> using = mapExprList(tableSource.getUsing());
        return new Join(left, type, right, condition, using, tableSource.isNatural(), Meta.empty());
    }

    private JoinType mapJoinType(SQLJoinTableSource.JoinType joinType) {
        if (joinType == null) {
            return JoinType.JOIN;
        }
        return JoinType.valueOf(joinType.name());
    }

    private Expr mapExpr(SQLExpr expr) {
        if (expr == null) {
            return null;
        }
        if (expr instanceof SQLIdentifierExpr identifierExpr) {
            return new Id(mapQName(identifierExpr), Meta.empty());
        }
        if (expr instanceof SQLPropertyExpr propertyExpr) {
            if ("*".equals(propertyExpr.getName())) {
                QName qualifier = propertyExpr.getOwner() == null ? null : mapQName(propertyExpr.getOwner());
                return new Star(qualifier, Meta.empty());
            }
            return new Id(mapQName(propertyExpr), Meta.empty());
        }
        if (expr instanceof SQLAllColumnExpr) {
            return new Star(null, Meta.empty());
        }
        if (expr instanceof SQLBinaryOpExpr binaryOpExpr) {
            String operator = binaryOpExpr.getOperator().getName();
            Binary binary = new Binary(mapExpr(binaryOpExpr.getLeft()), operator,
                    mapExpr(binaryOpExpr.getRight()), Meta.empty());
            if (binaryOpExpr.isParenthesized()) {
                return new ParenExpr(binary, Meta.empty());
            }
            return binary;
        }
        if (expr instanceof SQLUnaryExpr unaryExpr) {
            String operator = unaryExpr.getOperator().name;
            return new Unary(operator, mapExpr(unaryExpr.getExpr()), Meta.empty());
        }
        if (expr instanceof SQLAggregateExpr aggregateExpr) {
            return mapFuncCall(aggregateExpr, aggregateExpr.isDistinct());
        }
        if (expr instanceof SQLMethodInvokeExpr methodInvokeExpr) {
            return mapFuncCall(methodInvokeExpr, false);
        }
        if (expr instanceof SQLVariantRefExpr variantRefExpr) {
            return new Param(variantRefExpr.getName(), variantRefExpr.getIndex(), Meta.empty());
        }
        if (expr instanceof SQLInListExpr inListExpr) {
            return new InList(mapExpr(inListExpr.getExpr()), mapExprList(inListExpr.getTargetList()),
                    inListExpr.isNot(), Meta.empty());
        }
        if (expr instanceof SQLExistsExpr existsExpr) {
            return new Exists(mapSelect(existsExpr.getSubQuery()), existsExpr.isNot(), Meta.empty());
        }
        if (expr instanceof SQLCaseExpr caseExpr) {
            List<WhenThen> items = new ArrayList<>();
            for (SQLCaseExpr.Item item : caseExpr.getItems()) {
                items.add(new WhenThen(mapExpr(item.getConditionExpr()), mapExpr(item.getValueExpr()), Meta.empty()));
            }
            return new CaseExpr(mapExpr(caseExpr.getValueExpr()), items, mapExpr(caseExpr.getElseExpr()), Meta.empty());
        }
        if (expr instanceof SQLCharExpr charExpr) {
            return new Literal(LiteralType.STRING, charExpr.getText(), Meta.empty());
        }
        if (expr instanceof SQLNCharExpr ncharExpr) {
            return new Literal(LiteralType.STRING, ncharExpr.getText(), Meta.empty());
        }
        if (expr instanceof SQLNumberExpr numberExpr) {
            return new Literal(LiteralType.NUMBER, numberExpr.getNumber(), Meta.empty());
        }
        if (expr instanceof SQLIntegerExpr integerExpr) {
            return new Literal(LiteralType.NUMBER, integerExpr.getNumber(), Meta.empty());
        }
        if (expr instanceof SQLBooleanExpr booleanExpr) {
            return new Literal(LiteralType.BOOLEAN, booleanExpr.getBooleanValue(), Meta.empty());
        }
        if (expr instanceof SQLNullExpr) {
            return new Literal(LiteralType.NULL, null, Meta.empty());
        }
        if (expr instanceof SQLHexExpr hexExpr) {
            return new Literal(LiteralType.HEX, hexExpr.getHex(), Meta.empty());
        }
        if (expr instanceof SQLDateExpr dateExpr) {
            return new Literal(LiteralType.DATE, dateExpr.getLiteral(), Meta.empty());
        }
        if (expr instanceof SQLTimeExpr timeExpr) {
            return new Literal(LiteralType.TIME, timeExpr.getValue(), Meta.empty());
        }
        if (expr instanceof SQLDateTimeExpr dateTimeExpr) {
            return new Literal(LiteralType.DATETIME, dateTimeExpr.getValue(), Meta.empty());
        }
        throw new IllegalArgumentException("Unsupported SQLExpr: " + expr.getClass().getName());
    }

    private FuncCall mapFuncCall(SQLMethodInvokeExpr methodInvokeExpr, boolean distinct) {
        String name = qualifyMethodName(methodInvokeExpr);
        List<Expr> args = mapExprList(methodInvokeExpr.getArguments());
        return new FuncCall(name, args, distinct, Meta.empty());
    }

    private String qualifyMethodName(SQLMethodInvokeExpr methodInvokeExpr) {
        String name = methodInvokeExpr.getMethodName();
        SQLExpr owner = methodInvokeExpr.getOwner();
        QName ownerName = tryMapQName(owner);
        if (ownerName == null) {
            return name;
        }
        return String.join(".", ownerName.parts()) + "." + name;
    }

    private List<Expr> mapExprList(List<? extends SQLExpr> exprs) {
        if (exprs == null || exprs.isEmpty()) {
            return List.of();
        }
        List<Expr> results = new ArrayList<>(exprs.size());
        for (SQLExpr expr : exprs) {
            results.add(mapExpr(expr));
        }
        return results;
    }

    private List<List<Expr>> mapValuesList(List<SQLInsertStatement.ValuesClause> valuesList) {
        if (valuesList == null || valuesList.isEmpty()) {
            return List.of();
        }
        List<List<Expr>> rows = new ArrayList<>(valuesList.size());
        for (SQLInsertStatement.ValuesClause valuesClause : valuesList) {
            rows.add(mapExprList(valuesClause.getValues()));
        }
        return rows;
    }

    private QName mapQName(SQLExpr expr) {
        QName name = tryMapQName(expr);
        if (name == null) {
            throw new IllegalArgumentException("Unsupported name expression: " + expr);
        }
        return name;
    }

    private QName tryMapQName(SQLExpr expr) {
        if (expr == null) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        if (!collectQNameParts(expr, parts)) {
            return null;
        }
        return new QName(parts);
    }

    private boolean collectQNameParts(SQLExpr expr, List<String> parts) {
        if (expr instanceof SQLIdentifierExpr identifierExpr) {
            parts.add(identifierExpr.getName());
            return true;
        }
        if (expr instanceof SQLPropertyExpr propertyExpr) {
            SQLExpr owner = propertyExpr.getOwner();
            if (owner != null && !collectQNameParts(owner, parts)) {
                return false;
            }
            parts.add(propertyExpr.getName());
            return true;
        }
        return false;
    }

    private Meta metaWithHints(List<? extends SQLHint> hints) {
        if (hints == null || hints.isEmpty()) {
            return Meta.empty();
        }
        List<String> texts = new ArrayList<>(hints.size());
        for (SQLHint hint : hints) {
            if (hint instanceof SQLCommentHint commentHint && commentHint.getText() != null) {
                texts.add(commentHint.getText());
            } else {
                texts.add(hint.toString());
            }
        }
        if (texts.isEmpty()) {
            return Meta.empty();
        }
        return Meta.of("hints", List.copyOf(texts));
    }

    private Meta metaWithCommentHint(SQLCommentHint hint) {
        if (hint == null) {
            return Meta.empty();
        }
        String text = hint.getText();
        return Meta.of("hint", text == null ? hint.toString() : text);
    }
}
