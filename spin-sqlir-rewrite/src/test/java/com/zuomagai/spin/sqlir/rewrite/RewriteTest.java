package com.zuomagai.spin.sqlir.rewrite;

import com.zuomagai.spin.sqlir.Binary;
import com.zuomagai.spin.sqlir.Expr;
import com.zuomagai.spin.sqlir.Id;
import com.zuomagai.spin.sqlir.Join;
import com.zuomagai.spin.sqlir.JoinType;
import com.zuomagai.spin.sqlir.Literal;
import com.zuomagai.spin.sqlir.LiteralType;
import com.zuomagai.spin.sqlir.Meta;
import com.zuomagai.spin.sqlir.NamedTable;
import com.zuomagai.spin.sqlir.Param;
import com.zuomagai.spin.sqlir.QName;
import com.zuomagai.spin.sqlir.QueryStmt;
import com.zuomagai.spin.sqlir.SelectBody;
import com.zuomagai.spin.sqlir.SelectItem;
import com.zuomagai.spin.sqlir.Star;
import com.zuomagai.spin.sqlir.UpdateSet;
import com.zuomagai.spin.sqlir.UpdateStmt;
import junit.framework.TestCase;

import java.util.List;
import java.util.Map;

public class RewriteTest extends TestCase {
    private static final Meta META = Meta.empty();

    public void testTenantPredicateAddedToSelect() {
        SelectBody body = new SelectBody(false,
                List.of(new SelectItem(new Star(null, META), null, META)),
                new NamedTable(qname("users"), null, META),
                null,
                null,
                META);
        QueryStmt stmt = new QueryStmt(body, null, null, META);

        TenantPredicateRewriter rewriter = TenantPredicateRewriter.forColumnParam("tenant_id", ":tenantId");
        QueryStmt rewritten = (QueryStmt) rewriter.transform(stmt);

        Expr expected = new Binary(id("tenant_id"), "=", new Param(":tenantId", null, META), META);
        assertEquals(expected, ((SelectBody) rewritten.body()).where());
    }

    public void testTenantPredicateIdempotent() {
        Expr predicate = new Binary(id("tenant_id"), "=", new Param(":tenantId", null, META), META);
        SelectBody body = new SelectBody(false,
                List.of(new SelectItem(new Star(null, META), null, META)),
                new NamedTable(qname("users"), null, META),
                predicate,
                null,
                META);
        QueryStmt stmt = new QueryStmt(body, null, null, META);

        TenantPredicateRewriter rewriter = TenantPredicateRewriter.forColumnParam("tenant_id", ":tenantId");
        QueryStmt rewritten = (QueryStmt) rewriter.transform(stmt);

        assertSame(stmt, rewritten);
    }

    public void testTableNameRewriteFullAndLastPart() {
        Join join = new Join(
                new NamedTable(qname("app", "users"), "u", META),
                JoinType.INNER_JOIN,
                new NamedTable(qname("sales", "orders"), "o", META),
                new Binary(id("u", "id"), "=", id("o", "user_id"), META),
                List.of(),
                false,
                META
        );
        SelectBody body = new SelectBody(false,
                List.of(new SelectItem(new Star(null, META), null, META)),
                join,
                null,
                null,
                META);
        QueryStmt stmt = new QueryStmt(body, null, null, META);

        TableNameRewriter rewriter = TableNameRewriter.fromMap(Map.of(
                "app.users", "app.users_02",
                "orders", "orders_01"
        ));
        QueryStmt rewritten = (QueryStmt) rewriter.transform(stmt);
        Join rewrittenJoin = (Join) ((SelectBody) rewritten.body()).from();

        NamedTable left = (NamedTable) rewrittenJoin.left();
        NamedTable right = (NamedTable) rewrittenJoin.right();
        assertEquals(qname("app", "users_02"), left.name());
        assertEquals(qname("sales", "orders_01"), right.name());
    }

    public void testRewritePipelineAppliesBothRules() {
        UpdateSet setItem = new UpdateSet(id("name"), new Param(":name", null, META), META);
        Expr where = new Binary(id("status"), "=", new Literal(LiteralType.NUMBER, 1, META), META);
        UpdateStmt stmt = new UpdateStmt(new NamedTable(qname("users"), null, META),
                List.of(setItem), where, META);

        RewritePipeline pipeline = RewritePipeline.of(
                TableNameRewriter.fromMap(Map.of("users", "users_99")),
                TenantPredicateRewriter.forColumnParam("tenant_id", ":tenantId")
        );
        UpdateStmt rewritten = (UpdateStmt) pipeline.rewrite(stmt);

        NamedTable table = (NamedTable) rewritten.table();
        assertEquals(qname("users_99"), table.name());

        Expr expectedPredicate = new Binary(id("tenant_id"), "=", new Param(":tenantId", null, META), META);
        Expr expectedWhere = new Binary(where, "AND", expectedPredicate, META);
        assertEquals(expectedWhere, rewritten.where());
    }

    private static QName qname(String... parts) {
        return new QName(List.of(parts));
    }

    private static Id id(String... parts) {
        return new Id(qname(parts), META);
    }
}
