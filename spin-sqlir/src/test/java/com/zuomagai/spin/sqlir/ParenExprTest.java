package com.zuomagai.spin.sqlir;

import junit.framework.TestCase;

import java.util.List;

public class ParenExprTest extends TestCase {
    private static final Meta META = Meta.empty();

    public void testParenExprAcceptUsesVisitorHook() {
        ParenExpr expr = new ParenExpr(new Id(qname("a"), META), META);
        SqlVisitor<String> visitor = new SqlBaseVisitor<>() {
            @Override
            public String visitParenExpr(ParenExpr expr) {
                return "paren";
            }
        };

        assertEquals("paren", expr.accept(visitor));
    }

    public void testParenExprTransformPreservesWhenUnchanged() {
        ParenExpr expr = new ParenExpr(new Id(qname("a"), META), META);
        SqlTransformer transformer = new SqlTransformer();

        Expr transformed = transformer.transformExpr(expr);
        assertSame(expr, transformed);
    }

    public void testParenExprTransformUpdatesWhenChildChanges() {
        ParenExpr expr = new ParenExpr(new Id(qname("a"), META), META);

        Expr transformed = new MetaChangingTransformer().transformExpr(expr);
        assertNotSame(expr, transformed);
        ParenExpr parenExpr = (ParenExpr) transformed;
        Id inner = (Id) parenExpr.expr();
        assertEquals(Meta.of("changed", true), inner.meta());
    }

    private static QName qname(String... parts) {
        return new QName(List.of(parts));
    }

    private static final class MetaChangingTransformer extends SqlTransformer {
        @Override
        public Id transformId(Id id) {
            return new Id(id.name(), Meta.of("changed", true));
        }
    }
}
