package com.zuomagai.spin.sqlir.emit;

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
import com.zuomagai.spin.sqlir.NamedTable;
import com.zuomagai.spin.sqlir.NullsOrder;
import com.zuomagai.spin.sqlir.OrderBy;
import com.zuomagai.spin.sqlir.OrderByItem;
import com.zuomagai.spin.sqlir.OrderDirection;
import com.zuomagai.spin.sqlir.Param;
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

import java.util.List;

public abstract class AbstractSqlEmitter implements SqlEmitter {
    @Override
    public String emit(Statement statement) {
        StringBuilder sb = new StringBuilder();
        appendStatement(sb, statement);
        return sb.toString();
    }

    protected abstract Dialect dialect();

    protected abstract void appendLimitOffset(StringBuilder sb, LimitOffset limitOffset);

    protected boolean supportsNullsOrder() {
        return false;
    }

    protected String booleanLiteral(boolean value) {
        return value ? "TRUE" : "FALSE";
    }

    protected void appendHexLiteral(StringBuilder sb, String value) {
        sb.append("x'");
        if (value != null) {
            sb.append(value);
        }
        sb.append('\'');
    }

    protected void appendTemporalLiteral(StringBuilder sb, LiteralType type, Object value) {
        appendStringLiteral(sb, value == null ? "" : value.toString());
    }

    protected void appendStatement(StringBuilder sb, Statement statement) {
        if (statement instanceof QueryStmt stmt) {
            appendQueryStmt(sb, stmt);
            return;
        }
        if (statement instanceof InsertStmt stmt) {
            appendInsertStmt(sb, stmt);
            return;
        }
        if (statement instanceof UpdateStmt stmt) {
            appendUpdateStmt(sb, stmt);
            return;
        }
        if (statement instanceof DeleteStmt stmt) {
            appendDeleteStmt(sb, stmt);
            return;
        }
        throw new IllegalArgumentException("Unsupported statement: " + statement);
    }

    protected void appendQueryStmt(StringBuilder sb, QueryStmt stmt) {
        appendQueryBody(sb, stmt.body());
        if (stmt.orderBy() != null && !stmt.orderBy().items().isEmpty()) {
            sb.append(' ');
            appendOrderBy(sb, stmt.orderBy());
        }
        if (stmt.limitOffset() != null && (stmt.limitOffset().limit() != null || stmt.limitOffset().offset() != null)) {
            sb.append(' ');
            appendLimitOffset(sb, stmt.limitOffset());
        }
    }

    protected void appendInsertStmt(StringBuilder sb, InsertStmt stmt) {
        sb.append("INSERT INTO ");
        appendTableSource(sb, stmt.table());
        if (!stmt.columns().isEmpty()) {
            sb.append(" (");
            appendExprList(sb, stmt.columns());
            sb.append(')');
        }
        if (!stmt.valuesList().isEmpty()) {
            sb.append(" VALUES ");
            for (int i = 0; i < stmt.valuesList().size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append('(');
                appendExprList(sb, stmt.valuesList().get(i));
                sb.append(')');
            }
        } else if (stmt.query() != null) {
            sb.append(' ');
            appendQueryStmt(sb, stmt.query());
        }
    }

    protected void appendUpdateStmt(StringBuilder sb, UpdateStmt stmt) {
        sb.append("UPDATE ");
        appendTableSource(sb, stmt.table());
        sb.append(" SET ");
        for (int i = 0; i < stmt.setItems().size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            UpdateSet item = stmt.setItems().get(i);
            appendExpr(sb, item.target());
            sb.append(" = ");
            appendExpr(sb, item.value());
        }
        if (stmt.where() != null) {
            sb.append(" WHERE ");
            appendExpr(sb, stmt.where());
        }
    }

    protected void appendDeleteStmt(StringBuilder sb, DeleteStmt stmt) {
        sb.append("DELETE FROM ");
        appendTableSource(sb, stmt.table());
        if (stmt.where() != null) {
            sb.append(" WHERE ");
            appendExpr(sb, stmt.where());
        }
    }

    protected void appendQueryBody(StringBuilder sb, QueryBody body) {
        if (body instanceof SelectBody selectBody) {
            appendSelectBody(sb, selectBody);
            return;
        }
        if (body instanceof SetOpBody setOpBody) {
            appendSetOpBody(sb, setOpBody);
            return;
        }
        throw new IllegalArgumentException("Unsupported query body: " + body);
    }

    protected void appendSelectBody(StringBuilder sb, SelectBody body) {
        sb.append("SELECT ");
        if (body.distinct()) {
            sb.append("DISTINCT ");
        }
        if (body.selectItems().isEmpty()) {
            sb.append('*');
        } else {
            for (int i = 0; i < body.selectItems().size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                appendSelectItem(sb, body.selectItems().get(i));
            }
        }
        if (body.from() != null) {
            sb.append(" FROM ");
            appendTableSource(sb, body.from());
        }
        if (body.where() != null) {
            sb.append(" WHERE ");
            appendExpr(sb, body.where());
        }
        GroupBy groupBy = body.groupBy();
        if (groupBy != null) {
            if (!groupBy.items().isEmpty()) {
                sb.append(" GROUP BY ");
                appendExprList(sb, groupBy.items());
            }
            if (groupBy.having() != null) {
                sb.append(" HAVING ");
                appendExpr(sb, groupBy.having());
            }
        }
    }

    protected void appendSetOpBody(StringBuilder sb, SetOpBody body) {
        sb.append('(');
        appendQueryBody(sb, body.left());
        sb.append(')');
        sb.append(' ').append(setOpKeyword(body.op())).append(' ');
        sb.append('(');
        appendQueryBody(sb, body.right());
        sb.append(')');
    }

    protected void appendSelectItem(StringBuilder sb, SelectItem item) {
        appendExpr(sb, item.expr());
        if (item.alias() != null && !item.alias().isEmpty()) {
            sb.append(" AS ").append(item.alias());
        }
    }

    protected void appendOrderBy(StringBuilder sb, OrderBy orderBy) {
        if (orderBy.items().isEmpty()) {
            return;
        }
        sb.append("ORDER BY ");
        for (int i = 0; i < orderBy.items().size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            appendOrderByItem(sb, orderBy.items().get(i));
        }
    }

    protected void appendOrderByItem(StringBuilder sb, OrderByItem item) {
        appendExpr(sb, item.expr());
        if (item.direction() != null && item.direction() != OrderDirection.UNSPECIFIED) {
            sb.append(' ').append(item.direction() == OrderDirection.ASC ? "ASC" : "DESC");
        }
        if (supportsNullsOrder() && item.nullsOrder() != null) {
            sb.append(" NULLS ").append(item.nullsOrder() == NullsOrder.FIRST ? "FIRST" : "LAST");
        }
    }

    protected void appendTableSource(StringBuilder sb, TableSource source) {
        if (source instanceof NamedTable table) {
            appendNamedTable(sb, table);
            return;
        }
        if (source instanceof DerivedTable table) {
            appendDerivedTable(sb, table);
            return;
        }
        if (source instanceof Join join) {
            appendJoin(sb, join);
            return;
        }
        throw new IllegalArgumentException("Unsupported table source: " + source);
    }

    protected void appendNamedTable(StringBuilder sb, NamedTable table) {
        appendQName(sb, table.name());
        if (table.alias() != null && !table.alias().isEmpty()) {
            sb.append(' ').append(table.alias());
        }
    }

    protected void appendDerivedTable(StringBuilder sb, DerivedTable table) {
        sb.append('(');
        appendQueryStmt(sb, table.subquery());
        sb.append(')');
        if (table.alias() != null && !table.alias().isEmpty()) {
            sb.append(' ').append(table.alias());
        }
    }

    protected void appendJoin(StringBuilder sb, Join join) {
        if (join.type() == JoinType.COMMA) {
            appendTableSource(sb, join.left());
            sb.append(", ");
            appendTableSource(sb, join.right());
            return;
        }
        boolean natural = join.natural() || isNaturalJoinType(join.type());
        appendTableSource(sb, join.left());
        sb.append(' ').append(joinKeyword(join.type(), natural)).append(' ');
        appendTableSource(sb, join.right());
        if (!natural) {
            if (!join.using().isEmpty()) {
                sb.append(" USING (");
                appendExprList(sb, join.using());
                sb.append(')');
            } else if (join.condition() != null) {
                sb.append(" ON ");
                appendExpr(sb, join.condition());
            }
        }
    }

    protected void appendExpr(StringBuilder sb, Expr expr) {
        if (expr instanceof Id id) {
            appendQName(sb, id.name());
            return;
        }
        if (expr instanceof Binary binary) {
            sb.append('(');
            appendExpr(sb, binary.left());
            sb.append(' ').append(binary.operator()).append(' ');
            appendExpr(sb, binary.right());
            sb.append(')');
            return;
        }
        if (expr instanceof Unary unary) {
            appendUnary(sb, unary);
            return;
        }
        if (expr instanceof FuncCall call) {
            appendFuncCall(sb, call);
            return;
        }
        if (expr instanceof Param param) {
            appendParam(sb, param);
            return;
        }
        if (expr instanceof InList inList) {
            appendInList(sb, inList);
            return;
        }
        if (expr instanceof Exists exists) {
            appendExists(sb, exists);
            return;
        }
        if (expr instanceof CaseExpr caseExpr) {
            appendCaseExpr(sb, caseExpr);
            return;
        }
        if (expr instanceof Literal literal) {
            appendLiteral(sb, literal);
            return;
        }
        if (expr instanceof Star star) {
            appendStar(sb, star);
            return;
        }
        throw new IllegalArgumentException("Unsupported expression: " + expr);
    }

    protected void appendUnary(StringBuilder sb, Unary unary) {
        String op = unary.operator();
        if (isWordOperator(op)) {
            sb.append(op).append(' ');
        } else {
            sb.append(op);
        }
        appendExpr(sb, unary.expr());
    }

    protected void appendFuncCall(StringBuilder sb, FuncCall call) {
        sb.append(call.name()).append('(');
        if (call.distinct()) {
            sb.append("DISTINCT ");
        }
        appendExprList(sb, call.args());
        sb.append(')');
    }

    protected void appendParam(StringBuilder sb, Param param) {
        if (param.name() != null && !param.name().isEmpty()) {
            sb.append(param.name());
        } else {
            sb.append('?');
        }
    }

    protected void appendInList(StringBuilder sb, InList inList) {
        appendExpr(sb, inList.expr());
        if (inList.not()) {
            sb.append(" NOT");
        }
        sb.append(" IN (");
        appendExprList(sb, inList.list());
        sb.append(')');
    }

    protected void appendExists(StringBuilder sb, Exists exists) {
        if (exists.not()) {
            sb.append("NOT ");
        }
        sb.append("EXISTS (");
        appendQueryStmt(sb, exists.subquery());
        sb.append(')');
    }

    protected void appendCaseExpr(StringBuilder sb, CaseExpr caseExpr) {
        sb.append("CASE");
        if (caseExpr.value() != null) {
            sb.append(' ');
            appendExpr(sb, caseExpr.value());
        }
        for (WhenThen item : caseExpr.items()) {
            sb.append(" WHEN ");
            appendExpr(sb, item.when());
            sb.append(" THEN ");
            appendExpr(sb, item.then());
        }
        if (caseExpr.elseExpr() != null) {
            sb.append(" ELSE ");
            appendExpr(sb, caseExpr.elseExpr());
        }
        sb.append(" END");
    }

    protected void appendLiteral(StringBuilder sb, Literal literal) {
        LiteralType type = literal.type();
        Object value = literal.value();
        switch (type) {
            case STRING -> appendStringLiteral(sb, value == null ? "" : value.toString());
            case NUMBER -> sb.append(value == null ? "NULL" : value.toString());
            case BOOLEAN -> sb.append(booleanLiteral(Boolean.TRUE.equals(value)));
            case NULL -> sb.append("NULL");
            case DATE, TIME, DATETIME -> appendTemporalLiteral(sb, type, value);
            case HEX -> appendHexLiteral(sb, value == null ? "" : value.toString());
            default -> throw new IllegalArgumentException("Unsupported literal type: " + type);
        }
    }

    protected void appendStar(StringBuilder sb, Star star) {
        if (star.qualifier() == null) {
            sb.append('*');
            return;
        }
        appendQName(sb, star.qualifier());
        sb.append(".*");
    }

    protected void appendQName(StringBuilder sb, QName name) {
        List<String> parts = name.parts();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                sb.append('.');
            }
            sb.append(parts.get(i));
        }
    }

    protected void appendExprList(StringBuilder sb, List<? extends Expr> exprs) {
        for (int i = 0; i < exprs.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            appendExpr(sb, exprs.get(i));
        }
    }

    protected void appendStringLiteral(StringBuilder sb, String value) {
        sb.append('\'');
        sb.append(escapeSingleQuotes(value));
        sb.append('\'');
    }

    protected String escapeSingleQuotes(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.replace("'", "''");
    }

    protected boolean isWordOperator(String operator) {
        if (operator == null || operator.isEmpty()) {
            return false;
        }
        for (int i = 0; i < operator.length(); i++) {
            if (!Character.isLetter(operator.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    protected boolean isNaturalJoinType(JoinType type) {
        return type == JoinType.NATURAL_JOIN
                || type == JoinType.NATURAL_CROSS_JOIN
                || type == JoinType.NATURAL_LEFT_JOIN
                || type == JoinType.NATURAL_RIGHT_JOIN
                || type == JoinType.NATURAL_INNER_JOIN;
    }

    protected String joinTypeKeyword(JoinType type) {
        return switch (type) {
            case JOIN -> "JOIN";
            case INNER_JOIN -> "INNER JOIN";
            case CROSS_JOIN -> "CROSS JOIN";
            case NATURAL_JOIN -> "NATURAL JOIN";
            case NATURAL_CROSS_JOIN -> "NATURAL CROSS JOIN";
            case NATURAL_LEFT_JOIN -> "NATURAL LEFT JOIN";
            case NATURAL_RIGHT_JOIN -> "NATURAL RIGHT JOIN";
            case NATURAL_INNER_JOIN -> "NATURAL INNER JOIN";
            case LEFT_OUTER_JOIN -> "LEFT OUTER JOIN";
            case LEFT_SEMI_JOIN -> "LEFT SEMI JOIN";
            case LEFT_ANTI_JOIN -> "LEFT ANTI JOIN";
            case RIGHT_OUTER_JOIN -> "RIGHT OUTER JOIN";
            case FULL_OUTER_JOIN -> "FULL OUTER JOIN";
            case STRAIGHT_JOIN -> "STRAIGHT_JOIN";
            case OUTER_APPLY -> "OUTER APPLY";
            case CROSS_APPLY -> "CROSS APPLY";
            case COMMA -> ",";
        };
    }

    protected String joinKeyword(JoinType type, boolean natural) {
        String keyword = joinTypeKeyword(type);
        if (type == JoinType.COMMA) {
            return keyword;
        }
        if (natural && !keyword.startsWith("NATURAL")) {
            return "NATURAL " + keyword;
        }
        return keyword;
    }

    protected String setOpKeyword(SetOp op) {
        return switch (op) {
            case UNION -> "UNION";
            case UNION_ALL -> "UNION ALL";
            case MINUS -> "MINUS";
            case MINUS_DISTINCT -> "MINUS DISTINCT";
            case MINUS_ALL -> "MINUS ALL";
            case EXCEPT -> "EXCEPT";
            case EXCEPT_ALL -> "EXCEPT ALL";
            case EXCEPT_DISTINCT -> "EXCEPT DISTINCT";
            case INTERSECT -> "INTERSECT";
            case INTERSECT_ALL -> "INTERSECT ALL";
            case INTERSECT_DISTINCT -> "INTERSECT DISTINCT";
            case DISTINCT -> "DISTINCT";
        };
    }
}
