package com.zuomagai.spin.sqlir.emit;

import com.zuomagai.spin.sqlir.Binary;
import com.zuomagai.spin.sqlir.Id;
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
import com.zuomagai.spin.sqlir.QName;
import com.zuomagai.spin.sqlir.QueryStmt;
import com.zuomagai.spin.sqlir.SelectBody;
import com.zuomagai.spin.sqlir.SelectItem;
import com.zuomagai.spin.sqlir.Star;
import junit.framework.TestCase;

import java.util.List;

public class SqlEmitterTest extends TestCase {
    private static final Meta META = Meta.empty();

    public void testMySqlSelectWithWhereOrderLimit() {
        SelectItem idItem = new SelectItem(new Id(qname("u", "id"), META), "id", META);
        SelectItem starItem = new SelectItem(new Star(qname("u"), META), null, META);
        SelectBody body = new SelectBody(false, List.of(idItem, starItem),
                new NamedTable(qname("users"), "u", META),
                new Binary(new Id(qname("u", "tenant_id"), META), "=",
                        new Param(":tenantId", null, META), META),
                null,
                META);
        OrderBy orderBy = new OrderBy(List.of(
                new OrderByItem(new Id(qname("u", "id"), META), OrderDirection.DESC, NullsOrder.LAST, META)
        ), META);
        LimitOffset limitOffset = new LimitOffset(
                new Literal(LiteralType.NUMBER, 10, META),
                new Literal(LiteralType.NUMBER, 5, META),
                META
        );
        QueryStmt stmt = new QueryStmt(body, orderBy, limitOffset, META);

        String sql = new MySqlEmitter().emit(stmt);
        assertEquals("SELECT u.id AS id, u.* FROM users u WHERE (u.tenant_id = :tenantId) ORDER BY u.id DESC LIMIT 10 OFFSET 5", sql);
    }

    public void testOracleOrderByAndPagination() {
        SelectBody body = new SelectBody(false,
                List.of(new SelectItem(new Id(qname("t", "c"), META), null, META)),
                new NamedTable(qname("t"), null, META),
                null,
                null,
                META);
        OrderBy orderBy = new OrderBy(List.of(
                new OrderByItem(new Id(qname("t", "c"), META), OrderDirection.ASC, NullsOrder.LAST, META)
        ), META);
        LimitOffset limitOffset = new LimitOffset(
                new Literal(LiteralType.NUMBER, 10, META),
                new Literal(LiteralType.NUMBER, 5, META),
                META
        );
        QueryStmt stmt = new QueryStmt(body, orderBy, limitOffset, META);

        String sql = new OracleEmitter().emit(stmt);
        assertEquals("SELECT t.c FROM t ORDER BY t.c ASC NULLS LAST OFFSET 5 ROWS FETCH NEXT 10 ROWS ONLY", sql);
    }

    public void testMySqlLiterals() {
        SelectBody body = new SelectBody(false,
                List.of(
                        new SelectItem(new Literal(LiteralType.STRING, "O'Reilly", META), null, META),
                        new SelectItem(new Literal(LiteralType.HEX, "0A", META), null, META)
                ),
                null,
                null,
                null,
                META);
        QueryStmt stmt = new QueryStmt(body, null, null, META);

        String sql = new MySqlEmitter().emit(stmt);
        assertEquals("SELECT 'O''Reilly', x'0A'", sql);
    }

    public void testOracleBooleanLiteral() {
        SelectBody body = new SelectBody(false,
                List.of(
                        new SelectItem(new Literal(LiteralType.BOOLEAN, Boolean.TRUE, META), null, META),
                        new SelectItem(new Literal(LiteralType.BOOLEAN, Boolean.FALSE, META), null, META)
                ),
                null,
                null,
                null,
                META);
        QueryStmt stmt = new QueryStmt(body, null, null, META);

        String sql = new OracleEmitter().emit(stmt);
        assertEquals("SELECT 1, 0", sql);
    }

    private static QName qname(String... parts) {
        return new QName(List.of(parts));
    }
}
