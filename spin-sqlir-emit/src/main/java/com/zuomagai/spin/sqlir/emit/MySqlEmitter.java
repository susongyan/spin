package com.zuomagai.spin.sqlir.emit;

import com.zuomagai.spin.sqlir.Expr;
import com.zuomagai.spin.sqlir.LimitOffset;

public final class MySqlEmitter extends AbstractSqlEmitter {
    private static final String MAX_LIMIT = "18446744073709551615";

    @Override
    protected Dialect dialect() {
        return Dialect.MYSQL;
    }

    @Override
    protected void appendLimitOffset(StringBuilder sb, LimitOffset limitOffset) {
        Expr limit = limitOffset.limit();
        Expr offset = limitOffset.offset();
        if (limit == null && offset == null) {
            return;
        }
        sb.append("LIMIT ");
        if (limit == null) {
            sb.append(MAX_LIMIT);
        } else {
            appendExpr(sb, limit);
        }
        if (offset != null) {
            sb.append(" OFFSET ");
            appendExpr(sb, offset);
        }
    }
}
