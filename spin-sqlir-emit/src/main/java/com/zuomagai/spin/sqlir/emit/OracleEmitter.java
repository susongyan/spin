package com.zuomagai.spin.sqlir.emit;

import com.zuomagai.spin.sqlir.Expr;
import com.zuomagai.spin.sqlir.LimitOffset;

public final class OracleEmitter extends AbstractSqlEmitter {
    @Override
    protected Dialect dialect() {
        return Dialect.ORACLE;
    }

    @Override
    protected boolean supportsNullsOrder() {
        return true;
    }

    @Override
    protected String booleanLiteral(boolean value) {
        return value ? "1" : "0";
    }

    @Override
    protected void appendHexLiteral(StringBuilder sb, String value) {
        sb.append("hextoraw('");
        if (value != null) {
            sb.append(value);
        }
        sb.append("')");
    }

    @Override
    protected void appendLimitOffset(StringBuilder sb, LimitOffset limitOffset) {
        Expr limit = limitOffset.limit();
        Expr offset = limitOffset.offset();
        if (limit == null && offset == null) {
            return;
        }
        if (offset != null) {
            sb.append("OFFSET ");
            appendExpr(sb, offset);
            sb.append(" ROWS");
        }
        if (limit != null) {
            sb.append(offset != null ? " FETCH NEXT " : " FETCH FIRST ");
            appendExpr(sb, limit);
            sb.append(" ROWS ONLY");
        }
    }
}
