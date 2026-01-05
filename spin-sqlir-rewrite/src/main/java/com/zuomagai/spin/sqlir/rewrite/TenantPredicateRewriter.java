package com.zuomagai.spin.sqlir.rewrite;

import com.zuomagai.spin.sqlir.Binary;
import com.zuomagai.spin.sqlir.DeleteStmt;
import com.zuomagai.spin.sqlir.Expr;
import com.zuomagai.spin.sqlir.Id;
import com.zuomagai.spin.sqlir.Meta;
import com.zuomagai.spin.sqlir.Param;
import com.zuomagai.spin.sqlir.QName;
import com.zuomagai.spin.sqlir.SelectBody;
import com.zuomagai.spin.sqlir.SqlTransformer;
import com.zuomagai.spin.sqlir.UpdateStmt;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TenantPredicateRewriter extends SqlTransformer {
    private final Expr predicate;
    private final boolean applyToSelectWithoutFrom;

    public TenantPredicateRewriter(Expr predicate) {
        this(predicate, false);
    }

    public TenantPredicateRewriter(Expr predicate, boolean applyToSelectWithoutFrom) {
        this.predicate = Objects.requireNonNull(predicate, "predicate");
        this.applyToSelectWithoutFrom = applyToSelectWithoutFrom;
    }

    public static TenantPredicateRewriter forColumnValue(String column, Expr value) {
        Objects.requireNonNull(column, "column");
        Objects.requireNonNull(value, "value");
        Expr predicate = new Binary(new Id(parseQName(column), Meta.empty()), "=", value, Meta.empty());
        return new TenantPredicateRewriter(predicate);
    }

    public static TenantPredicateRewriter forColumnParam(String column, String paramName) {
        Objects.requireNonNull(paramName, "paramName");
        return forColumnValue(column, new Param(paramName, null, Meta.empty()));
    }

    public static TenantPredicateRewriter forColumnParamIndex(String column, int index) {
        return forColumnValue(column, new Param(null, index, Meta.empty()));
    }

    @Override
    public SelectBody transformSelectBody(SelectBody body) {
        SelectBody transformed = super.transformSelectBody(body);
        if (transformed.from() == null && !applyToSelectWithoutFrom) {
            return transformed;
        }
        Expr nextWhere = injectPredicate(transformed.where());
        if (nextWhere == transformed.where()) {
            return transformed;
        }
        return new SelectBody(transformed.distinct(), transformed.selectItems(), transformed.from(),
                nextWhere, transformed.groupBy(), transformed.meta());
    }

    @Override
    public UpdateStmt transformUpdateStmt(UpdateStmt stmt) {
        UpdateStmt transformed = super.transformUpdateStmt(stmt);
        Expr nextWhere = injectPredicate(transformed.where());
        if (nextWhere == transformed.where()) {
            return transformed;
        }
        return new UpdateStmt(transformed.table(), transformed.setItems(), nextWhere, transformed.meta());
    }

    @Override
    public DeleteStmt transformDeleteStmt(DeleteStmt stmt) {
        DeleteStmt transformed = super.transformDeleteStmt(stmt);
        Expr nextWhere = injectPredicate(transformed.where());
        if (nextWhere == transformed.where()) {
            return transformed;
        }
        return new DeleteStmt(transformed.table(), nextWhere, transformed.meta());
    }

    private Expr injectPredicate(Expr where) {
        if (where == null) {
            return predicate;
        }
        if (containsPredicate(where, predicate)) {
            return where;
        }
        return new Binary(where, "AND", predicate, Meta.empty());
    }

    private boolean containsPredicate(Expr expr, Expr predicate) {
        if (expr == null) {
            return false;
        }
        if (expr.equals(predicate)) {
            return true;
        }
        if (expr instanceof Binary binary && isAnd(binary.operator())) {
            return containsPredicate(binary.left(), predicate) || containsPredicate(binary.right(), predicate);
        }
        return false;
    }

    private boolean isAnd(String operator) {
        return operator != null && operator.equalsIgnoreCase("AND");
    }

    private static QName parseQName(String text) {
        List<String> parts = new ArrayList<>();
        for (String part : text.split("\\.")) {
            if (!part.isEmpty()) {
                parts.add(part);
            }
        }
        return new QName(parts);
    }
}
