package com.zuomagai.spin.sqlir.druid;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLCommentHint;
import com.alibaba.druid.sql.ast.SQLLimit;
import com.alibaba.druid.sql.ast.SQLOrderBy;
import com.alibaba.druid.sql.ast.SQLOrderingSpecification;
import com.alibaba.druid.sql.ast.expr.SQLAggregateExpr;
import com.alibaba.druid.sql.ast.expr.SQLAggregateOption;
import com.alibaba.druid.sql.ast.expr.SQLAllColumnExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;
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
import com.alibaba.druid.sql.ast.expr.SQLNullExpr;
import com.alibaba.druid.sql.ast.expr.SQLNumberExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.expr.SQLTimeExpr;
import com.alibaba.druid.sql.ast.expr.SQLUnaryExpr;
import com.alibaba.druid.sql.ast.expr.SQLUnaryOperator;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLSelectGroupByClause;
import com.alibaba.druid.sql.ast.statement.SQLSelectOrderByItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.zuomagai.spin.mapper.DruidToSqlirMapper;
import com.zuomagai.spin.sqlir.Binary;
import com.zuomagai.spin.sqlir.CaseExpr;
import com.zuomagai.spin.sqlir.DeleteStmt;
import com.zuomagai.spin.sqlir.DerivedTable;
import com.zuomagai.spin.sqlir.Exists;
import com.zuomagai.spin.sqlir.Expr;
import com.zuomagai.spin.sqlir.FuncCall;
import com.zuomagai.spin.sqlir.GroupBy;
import com.zuomagai.spin.sqlir.Id;
import com.zuomagai.spin.sqlir.InList;
import com.zuomagai.spin.sqlir.InsertStmt;
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
import com.zuomagai.spin.sqlir.QueryStmt;
import com.zuomagai.spin.sqlir.SelectBody;
import com.zuomagai.spin.sqlir.SelectItem;
import com.zuomagai.spin.sqlir.SetOp;
import com.zuomagai.spin.sqlir.SetOpBody;
import com.zuomagai.spin.sqlir.Star;
import com.zuomagai.spin.sqlir.Statement;
import com.zuomagai.spin.sqlir.Unary;
import com.zuomagai.spin.sqlir.UpdateSet;
import com.zuomagai.spin.sqlir.UpdateStmt;
import junit.framework.TestCase;

import java.util.List;

public class DruidToSqlirMapperTest extends TestCase {
    public void testMapSelectWithJoinGroupByOrderLimit() {
        String sql = "select distinct t1.a, sum(t2.b) as total "
                + "from t1 inner join t2 on t1.id = t2.id "
                + "where t1.a > 1 "
                + "group by t1.a having sum(t2.b) > 10 "
                + "order by t1.a desc "
                + "limit 5 offset 2";
        SQLStatement statement = SQLUtils.parseSingleStatement(sql, DbType.mysql);
        Statement mapped = new DruidToSqlirMapper().mapStatement(statement);

        assertTrue(mapped instanceof QueryStmt);
        QueryStmt query = (QueryStmt) mapped;
        assertNotNull(query.orderBy());
        assertNotNull(query.limitOffset());

        SelectBody body = assertExpr(SelectBody.class, query.body());
        assertTrue(body.distinct());
        assertEquals(2, body.selectItems().size());

        SelectItem first = body.selectItems().get(0);
        Id firstId = assertExpr(Id.class, first.expr());
        assertEquals(List.of("t1", "a"), firstId.name().parts());

        SelectItem second = body.selectItems().get(1);
        FuncCall sum = assertExpr(FuncCall.class, second.expr());
        assertEquals("sum", sum.name().toLowerCase());
        assertFalse(sum.distinct());
        assertEquals(1, sum.args().size());
        Id sumArg = assertExpr(Id.class, sum.args().get(0));
        assertEquals(List.of("t2", "b"), sumArg.name().parts());
        assertEquals("total", second.alias());

        Join join = assertExpr(Join.class, body.from());
        assertEquals(JoinType.INNER_JOIN, join.type());
        NamedTable left = assertExpr(NamedTable.class, join.left());
        NamedTable right = assertExpr(NamedTable.class, join.right());
        assertEquals(List.of("t1"), left.name().parts());
        assertEquals(List.of("t2"), right.name().parts());
        Binary joinCondition = assertExpr(Binary.class, join.condition());
        assertEquals("=", joinCondition.operator());

        Binary where = assertExpr(Binary.class, body.where());
        assertEquals(">", where.operator());

        GroupBy groupBy = body.groupBy();
        assertNotNull(groupBy);
        assertEquals(1, groupBy.items().size());
        Id groupByItem = assertExpr(Id.class, groupBy.items().get(0));
        assertEquals(List.of("t1", "a"), groupByItem.name().parts());
        Binary having = assertExpr(Binary.class, groupBy.having());
        assertEquals(">", having.operator());

        OrderBy orderBy = query.orderBy();
        assertEquals(1, orderBy.items().size());
        OrderByItem orderByItem = orderBy.items().get(0);
        assertEquals(OrderDirection.DESC, orderByItem.direction());
        assertNull(orderByItem.nullsOrder());

        LimitOffset limitOffset = query.limitOffset();
        Literal limit = assertLiteral(limitOffset.limit(), LiteralType.NUMBER);
        assertEquals(5, ((Number) limit.value()).intValue());
        Literal offset = assertLiteral(limitOffset.offset(), LiteralType.NUMBER);
        assertEquals(2, ((Number) offset.value()).intValue());
    }

    public void testMapUnionAll() {
        String sql = "select 1 union all select 2";
        SQLStatement statement = SQLUtils.parseSingleStatement(sql, DbType.mysql);
        Statement mapped = new DruidToSqlirMapper().mapStatement(statement);

        QueryStmt query = assertExpr(QueryStmt.class, mapped);
        SetOpBody body = assertExpr(SetOpBody.class, query.body());
        assertEquals(SetOp.UNION_ALL, body.op());
        assertTrue(body.left() instanceof SelectBody);
        assertTrue(body.right() instanceof SelectBody);
    }

    public void testMapInsertValues() {
        String sql = "insert into t_user (id, name) values (1, 'a'), (2, 'b')";
        SQLStatement statement = SQLUtils.parseSingleStatement(sql, DbType.mysql);
        Statement mapped = new DruidToSqlirMapper().mapStatement(statement);

        InsertStmt insert = assertExpr(InsertStmt.class, mapped);
        NamedTable table = assertExpr(NamedTable.class, insert.table());
        assertEquals(List.of("t_user"), table.name().parts());

        assertEquals(2, insert.columns().size());
        Id idColumn = assertExpr(Id.class, insert.columns().get(0));
        Id nameColumn = assertExpr(Id.class, insert.columns().get(1));
        assertEquals(List.of("id"), idColumn.name().parts());
        assertEquals(List.of("name"), nameColumn.name().parts());

        assertEquals(2, insert.valuesList().size());
        List<Expr> firstRow = insert.valuesList().get(0);
        Literal firstId = assertLiteral(firstRow.get(0), LiteralType.NUMBER);
        Literal firstName = assertLiteral(firstRow.get(1), LiteralType.STRING);
        assertEquals(1, ((Number) firstId.value()).intValue());
        assertEquals("a", firstName.value());
    }

    public void testMapUpdateAndDelete() {
        String updateSql = "update t_user set name = 'bob', age = age + 1 where id = 7";
        SQLStatement updateStatement = SQLUtils.parseSingleStatement(updateSql, DbType.mysql);
        Statement updateMapped = new DruidToSqlirMapper().mapStatement(updateStatement);

        UpdateStmt update = assertExpr(UpdateStmt.class, updateMapped);
        NamedTable updateTable = assertExpr(NamedTable.class, update.table());
        assertEquals(List.of("t_user"), updateTable.name().parts());
        assertEquals(2, update.setItems().size());

        UpdateSet nameSet = update.setItems().get(0);
        Id nameTarget = assertExpr(Id.class, nameSet.target());
        Literal nameValue = assertLiteral(nameSet.value(), LiteralType.STRING);
        assertEquals(List.of("name"), nameTarget.name().parts());
        assertEquals("bob", nameValue.value());

        UpdateSet ageSet = update.setItems().get(1);
        Id ageTarget = assertExpr(Id.class, ageSet.target());
        Binary ageValue = assertExpr(Binary.class, ageSet.value());
        assertEquals(List.of("age"), ageTarget.name().parts());
        assertEquals("+", ageValue.operator());

        Binary updateWhere = assertExpr(Binary.class, update.where());
        assertEquals("=", updateWhere.operator());

        String deleteSql = "delete from t_user where id = 9";
        SQLStatement deleteStatement = SQLUtils.parseSingleStatement(deleteSql, DbType.mysql);
        Statement deleteMapped = new DruidToSqlirMapper().mapStatement(deleteStatement);

        DeleteStmt delete = assertExpr(DeleteStmt.class, deleteMapped);
        NamedTable deleteTable = assertExpr(NamedTable.class, delete.table());
        assertEquals(List.of("t_user"), deleteTable.name().parts());
        Binary deleteWhere = assertExpr(Binary.class, delete.where());
        assertEquals("=", deleteWhere.operator());
    }

    public void testMapMysqlCteJoinAggregate() {
        String sql = "with cte as (select id, category from t1) "
                + "select cte.category, count(*) as cnt "
                + "from cte inner join t2 on cte.id = t2.id "
                + "where t2.flag = ? "
                + "group by cte.category having count(*) > ?";
        SQLStatement statement = SQLUtils.parseSingleStatement(sql, DbType.mysql);
        Statement mapped = new DruidToSqlirMapper().mapStatement(statement);

        QueryStmt query = assertExpr(QueryStmt.class, mapped);
        SelectBody body = assertExpr(SelectBody.class, query.body());
        assertEquals(2, body.selectItems().size());

        Id category = assertExpr(Id.class, body.selectItems().get(0).expr());
        assertEquals(List.of("cte", "category"), category.name().parts());
        FuncCall count = assertExpr(FuncCall.class, body.selectItems().get(1).expr());
        assertEquals("count", count.name().toLowerCase());
        assertEquals("cnt", body.selectItems().get(1).alias());

        Join join = assertExpr(Join.class, body.from());
        assertEquals(JoinType.INNER_JOIN, join.type());
        NamedTable left = assertExpr(NamedTable.class, join.left());
        NamedTable right = assertExpr(NamedTable.class, join.right());
        assertEquals(List.of("cte"), left.name().parts());
        assertEquals(List.of("t2"), right.name().parts());
        Binary joinCondition = assertExpr(Binary.class, join.condition());
        assertEquals("=", joinCondition.operator());

        Binary where = assertExpr(Binary.class, body.where());
        assertEquals("=", where.operator());
        Param whereParam = assertExpr(Param.class, where.right());
        assertEquals("?", whereParam.name());

        GroupBy groupBy = body.groupBy();
        assertNotNull(groupBy);
        assertEquals(1, groupBy.items().size());
        Id groupByItem = assertExpr(Id.class, groupBy.items().get(0));
        assertEquals(List.of("cte", "category"), groupByItem.name().parts());
        Binary having = assertExpr(Binary.class, groupBy.having());
        assertEquals(">", having.operator());
        Param havingParam = assertExpr(Param.class, having.right());
        assertEquals("?", havingParam.name());
    }

    public void testMapMysqlDerivedTableCaseAndLimit() {
        String sql = "select t.id, case when t.score >= 60 then 'pass' else 'fail' end as status "
                + "from (select id, score from t_score) t "
                + "order by t.id asc limit 10 offset 5";
        SQLStatement statement = SQLUtils.parseSingleStatement(sql, DbType.mysql);
        Statement mapped = new DruidToSqlirMapper().mapStatement(statement);

        QueryStmt query = assertExpr(QueryStmt.class, mapped);
        SelectBody body = assertExpr(SelectBody.class, query.body());

        DerivedTable derivedTable = assertExpr(DerivedTable.class, body.from());
        assertEquals("t", derivedTable.alias());
        SelectBody derivedBody = assertExpr(SelectBody.class, derivedTable.subquery().body());
        NamedTable derivedFrom = assertExpr(NamedTable.class, derivedBody.from());
        assertEquals(List.of("t_score"), derivedFrom.name().parts());

        assertEquals(2, body.selectItems().size());
        Id id = assertExpr(Id.class, body.selectItems().get(0).expr());
        assertEquals(List.of("t", "id"), id.name().parts());
        CaseExpr caseExpr = assertExpr(CaseExpr.class, body.selectItems().get(1).expr());
        assertEquals(1, caseExpr.items().size());
        assertNotNull(caseExpr.elseExpr());
        assertEquals("status", body.selectItems().get(1).alias());

        OrderBy orderBy = query.orderBy();
        assertNotNull(orderBy);
        assertEquals(1, orderBy.items().size());
        assertEquals(OrderDirection.ASC, orderBy.items().get(0).direction());

        LimitOffset limitOffset = query.limitOffset();
        assertNotNull(limitOffset);
        Literal limit = assertLiteral(limitOffset.limit(), LiteralType.NUMBER);
        Literal offset = assertLiteral(limitOffset.offset(), LiteralType.NUMBER);
        assertEquals(10, ((Number) limit.value()).intValue());
        assertEquals(5, ((Number) offset.value()).intValue());
    }

    public void testMapMysqlInListAndExists() {
        String sql = "select u.id from users u "
                + "where u.status in ('A','B') "
                + "and exists (select 1 from orders o where o.user_id = u.id and o.amount > 100)";
        SQLStatement statement = SQLUtils.parseSingleStatement(sql, DbType.mysql);
        Statement mapped = new DruidToSqlirMapper().mapStatement(statement);

        QueryStmt query = assertExpr(QueryStmt.class, mapped);
        SelectBody body = assertExpr(SelectBody.class, query.body());
        NamedTable from = assertExpr(NamedTable.class, body.from());
        assertEquals(List.of("users"), from.name().parts());

        Binary where = assertExpr(Binary.class, body.where());
        assertEquals("AND", where.operator());
        InList inList = assertExpr(InList.class, where.left());
        assertEquals(2, inList.list().size());
        Literal firstStatus = assertLiteral(inList.list().get(0), LiteralType.STRING);
        Literal secondStatus = assertLiteral(inList.list().get(1), LiteralType.STRING);
        assertEquals("A", firstStatus.value());
        assertEquals("B", secondStatus.value());

        Exists exists = assertExpr(Exists.class, where.right());
        SelectBody subBody = assertExpr(SelectBody.class, exists.subquery().body());
        NamedTable subFrom = assertExpr(NamedTable.class, subBody.from());
        assertEquals(List.of("orders"), subFrom.name().parts());
    }

    public void testMapMysqlLeftJoinAggregation() {
        String sql = "select u.id, sum(o.amount) as total "
                + "from users u left join orders o on u.id = o.user_id "
                + "where u.active = 1 "
                + "group by u.id";
        SQLStatement statement = SQLUtils.parseSingleStatement(sql, DbType.mysql);
        Statement mapped = new DruidToSqlirMapper().mapStatement(statement);

        QueryStmt query = assertExpr(QueryStmt.class, mapped);
        SelectBody body = assertExpr(SelectBody.class, query.body());

        Join join = assertExpr(Join.class, body.from());
        assertEquals(JoinType.LEFT_OUTER_JOIN, join.type());
        Binary joinCondition = assertExpr(Binary.class, join.condition());
        assertEquals("=", joinCondition.operator());

        assertEquals(2, body.selectItems().size());
        Id userId = assertExpr(Id.class, body.selectItems().get(0).expr());
        assertEquals(List.of("u", "id"), userId.name().parts());
        FuncCall sum = assertExpr(FuncCall.class, body.selectItems().get(1).expr());
        assertEquals("sum", sum.name().toLowerCase());
        assertEquals("total", body.selectItems().get(1).alias());

        Binary where = assertExpr(Binary.class, body.where());
        assertEquals("=", where.operator());
        GroupBy groupBy = body.groupBy();
        assertNotNull(groupBy);
        assertEquals(1, groupBy.items().size());
    }

    public void testMapMysqlInListParamsAndLimit() {
        String sql = "select * from t where id in (?, ?, ?) order by id limit ? offset ?";
        SQLStatement statement = SQLUtils.parseSingleStatement(sql, DbType.mysql);
        Statement mapped = new DruidToSqlirMapper().mapStatement(statement);

        QueryStmt query = assertExpr(QueryStmt.class, mapped);
        SelectBody body = assertExpr(SelectBody.class, query.body());

        Star star = assertExpr(Star.class, body.selectItems().get(0).expr());
        assertNull(star.qualifier());
        NamedTable from = assertExpr(NamedTable.class, body.from());
        assertEquals(List.of("t"), from.name().parts());

        InList inList = assertExpr(InList.class, body.where());
        assertEquals(3, inList.list().size());
        Param firstParam = assertExpr(Param.class, inList.list().get(0));
        Param secondParam = assertExpr(Param.class, inList.list().get(1));
        Param thirdParam = assertExpr(Param.class, inList.list().get(2));
        assertEquals("?", firstParam.name());
        assertEquals("?", secondParam.name());
        assertEquals("?", thirdParam.name());

        OrderBy orderBy = query.orderBy();
        assertNotNull(orderBy);
        assertEquals(1, orderBy.items().size());

        LimitOffset limitOffset = query.limitOffset();
        assertNotNull(limitOffset);
        assertExpr(Param.class, limitOffset.limit());
        assertExpr(Param.class, limitOffset.offset());
    }

    public void testMapOracleCteAggregation() {
        String sql = "with sales_cte as (select dept_id, amount from sales) "
                + "select dept_id, sum(amount) as total "
                + "from sales_cte "
                + "group by dept_id having sum(amount) > :min_total";
        SQLStatement statement = SQLUtils.parseSingleStatement(sql, DbType.oracle);
        Statement mapped = new DruidToSqlirMapper().mapStatement(statement);

        QueryStmt query = assertExpr(QueryStmt.class, mapped);
        SelectBody body = assertExpr(SelectBody.class, query.body());
        NamedTable from = assertExpr(NamedTable.class, body.from());
        assertEquals(List.of("sales_cte"), from.name().parts());

        assertEquals(2, body.selectItems().size());
        FuncCall sum = assertExpr(FuncCall.class, body.selectItems().get(1).expr());
        assertEquals("sum", sum.name().toLowerCase());
        assertEquals("total", body.selectItems().get(1).alias());

        GroupBy groupBy = body.groupBy();
        assertNotNull(groupBy);
        Binary having = assertExpr(Binary.class, groupBy.having());
        assertEquals(">", having.operator());
        Param minTotal = assertExpr(Param.class, having.right());
        assertEquals(":min_total", minTotal.name());
    }

    public void testMapOracleLeftJoinCase() {
        String sql = "select e.emp_id, case when e.salary > 10000 then 'H' else 'L' end as level "
                + "from emp e left join dept d on e.dept_id = d.id";
        SQLStatement statement = SQLUtils.parseSingleStatement(sql, DbType.oracle);
        Statement mapped = new DruidToSqlirMapper().mapStatement(statement);

        QueryStmt query = assertExpr(QueryStmt.class, mapped);
        SelectBody body = assertExpr(SelectBody.class, query.body());

        Join join = assertExpr(Join.class, body.from());
        assertEquals(JoinType.LEFT_OUTER_JOIN, join.type());
        Binary joinCondition = assertExpr(Binary.class, join.condition());
        assertEquals("=", joinCondition.operator());

        assertEquals(2, body.selectItems().size());
        CaseExpr caseExpr = assertExpr(CaseExpr.class, body.selectItems().get(1).expr());
        assertEquals(1, caseExpr.items().size());
        assertNotNull(caseExpr.elseExpr());
    }

    public void testMapOracleInExistsWithParams() {
        String sql = "select e.emp_id from emp e "
                + "where e.dept_id in (:d1, :d2) "
                + "and exists (select 1 from bonus b where b.emp_id = e.emp_id and b.amount > :min_bonus)";
        SQLStatement statement = SQLUtils.parseSingleStatement(sql, DbType.oracle);
        Statement mapped = new DruidToSqlirMapper().mapStatement(statement);

        QueryStmt query = assertExpr(QueryStmt.class, mapped);
        SelectBody body = assertExpr(SelectBody.class, query.body());

        Binary where = assertExpr(Binary.class, body.where());
        assertEquals("AND", where.operator());
        InList inList = assertExpr(InList.class, where.left());
        assertEquals(2, inList.list().size());
        Param first = assertExpr(Param.class, inList.list().get(0));
        Param second = assertExpr(Param.class, inList.list().get(1));
        assertEquals(":d1", first.name());
        assertEquals(":d2", second.name());

        Exists exists = assertExpr(Exists.class, where.right());
        SelectBody subBody = assertExpr(SelectBody.class, exists.subquery().body());
        NamedTable subFrom = assertExpr(NamedTable.class, subBody.from());
        assertEquals(List.of("bonus"), subFrom.name().parts());
    }

    public void testMapOraclePagination() {
        String sql = "select * from emp order by emp_id offset 5 rows fetch next 10 rows only";
        SQLStatement statement = SQLUtils.parseSingleStatement(sql, DbType.oracle);
        Statement mapped = new DruidToSqlirMapper().mapStatement(statement);

        QueryStmt query = assertExpr(QueryStmt.class, mapped);
        SelectBody body = assertExpr(SelectBody.class, query.body());
        NamedTable from = assertExpr(NamedTable.class, body.from());
        assertEquals(List.of("emp"), from.name().parts());

        OrderBy orderBy = query.orderBy();
        assertNotNull(orderBy);
        assertEquals(1, orderBy.items().size());

        LimitOffset limitOffset = query.limitOffset();
        assertNotNull(limitOffset);
        Literal limit = assertLiteral(limitOffset.limit(), LiteralType.NUMBER);
        Literal offset = assertLiteral(limitOffset.offset(), LiteralType.NUMBER);
        assertEquals(10, ((Number) limit.value()).intValue());
        assertEquals(5, ((Number) offset.value()).intValue());
    }

    public void testMapOracleDerivedTable() {
        String sql = "select v.dept_id, v.cnt "
                + "from (select dept_id, count(*) cnt from emp group by dept_id) v "
                + "where v.cnt > 1";
        SQLStatement statement = SQLUtils.parseSingleStatement(sql, DbType.oracle);
        Statement mapped = new DruidToSqlirMapper().mapStatement(statement);

        QueryStmt query = assertExpr(QueryStmt.class, mapped);
        SelectBody body = assertExpr(SelectBody.class, query.body());

        DerivedTable derivedTable = assertExpr(DerivedTable.class, body.from());
        assertEquals("v", derivedTable.alias());
        SelectBody derivedBody = assertExpr(SelectBody.class, derivedTable.subquery().body());
        GroupBy groupBy = derivedBody.groupBy();
        assertNotNull(groupBy);
        assertEquals(1, groupBy.items().size());

        Binary where = assertExpr(Binary.class, body.where());
        assertEquals(">", where.operator());
        Literal threshold = assertLiteral(where.right(), LiteralType.NUMBER);
        assertEquals(1, ((Number) threshold.value()).intValue());
    }

    public void testMapManualExpressionsAndHints() {
        SQLSelectQueryBlock queryBlock = new SQLSelectQueryBlock();
        queryBlock.addSelectItem(new SQLAllColumnExpr());
        queryBlock.addSelectItem(new SQLPropertyExpr(new SQLIdentifierExpr("t"), "*"));
        queryBlock.addSelectItem(new SQLUnaryExpr(SQLUnaryOperator.Negative, new SQLIntegerExpr(1)));
        queryBlock.addSelectItem(new SQLBinaryOpExpr(new SQLIdentifierExpr("a"), SQLBinaryOperator.GreaterThan,
                new SQLIntegerExpr(1)));
        SQLAggregateExpr countDistinct = new SQLAggregateExpr("count", SQLAggregateOption.DISTINCT,
                new SQLAllColumnExpr());
        queryBlock.addSelectItem(countDistinct);
        queryBlock.addSelectItem(new SQLMethodInvokeExpr("func", new SQLIdentifierExpr("pkg"),
                new SQLIntegerExpr(2)));
        SQLVariantRefExpr paramExpr = new SQLVariantRefExpr(":id");
        paramExpr.setIndex(1);
        queryBlock.addSelectItem(paramExpr);
        SQLInListExpr inListExpr = new SQLInListExpr(new SQLIdentifierExpr("status"));
        inListExpr.addTarget(new SQLCharExpr("A"));
        inListExpr.addTarget(new SQLCharExpr("B"));
        queryBlock.addSelectItem(inListExpr);
        SQLSelectQueryBlock subQueryBlock = new SQLSelectQueryBlock();
        subQueryBlock.addSelectItem(new SQLIntegerExpr(1));
        queryBlock.addSelectItem(new SQLExistsExpr(new SQLSelect(subQueryBlock)));
        SQLCaseExpr caseExpr = new SQLCaseExpr();
        caseExpr.addItem(new SQLBinaryOpExpr(new SQLIdentifierExpr("score"),
                SQLBinaryOperator.GreaterThanOrEqual, new SQLIntegerExpr(60)), new SQLCharExpr("pass"));
        caseExpr.setElseExpr(new SQLCharExpr("fail"));
        queryBlock.addSelectItem(caseExpr);
        queryBlock.addSelectItem(new SQLCharExpr("text"));
        queryBlock.addSelectItem(new SQLNumberExpr(3.14));
        queryBlock.addSelectItem(new SQLBooleanExpr(true));
        queryBlock.addSelectItem(new SQLNullExpr());
        queryBlock.addSelectItem(new SQLHexExpr("FF"));
        queryBlock.addSelectItem(new SQLDateExpr("2020-01-01"));
        queryBlock.addSelectItem(new SQLTimeExpr("12:34:56"));
        queryBlock.addSelectItem(new SQLDateTimeExpr("2020-01-01 12:34:56"));

        SQLSelectGroupByClause groupByClause = new SQLSelectGroupByClause();
        groupByClause.addItem(new SQLIdentifierExpr("g"));
        groupByClause.setHint(new SQLCommentHint("group_hint"));
        queryBlock.setGroupBy(groupByClause);
        queryBlock.getHints().add(new SQLCommentHint("query_hint"));

        SQLSelectOrderByItem orderByItem = new SQLSelectOrderByItem(new SQLIdentifierExpr("a"),
                SQLOrderingSpecification.ASC);
        orderByItem.setNullsOrderType(SQLSelectOrderByItem.NullsOrderType.NullsFirst);
        SQLOrderBy orderBy = new SQLOrderBy();
        orderBy.addItem(orderByItem);
        queryBlock.setOrderBy(orderBy);

        SQLLimit limit = new SQLLimit();
        limit.setRowCount(new SQLIntegerExpr(5));
        limit.setOffset(new SQLIntegerExpr(1));
        queryBlock.setLimit(limit);

        SQLSelect select = new SQLSelect(queryBlock);
        select.getHints().add(new SQLCommentHint("select_hint"));

        QueryStmt query = new DruidToSqlirMapper().mapSelect(select);
        assertEquals(List.of("select_hint"), query.meta().get("hints"));
        SelectBody body = assertExpr(SelectBody.class, query.body());
        assertEquals(List.of("query_hint"), body.meta().get("hints"));
        assertEquals("group_hint", body.groupBy().meta().get("hint"));

        OrderBy mappedOrderBy = query.orderBy();
        OrderByItem mappedOrderItem = mappedOrderBy.items().get(0);
        assertEquals(OrderDirection.ASC, mappedOrderItem.direction());
        assertEquals(NullsOrder.FIRST, mappedOrderItem.nullsOrder());

        LimitOffset limitOffset = query.limitOffset();
        Literal limitLiteral = assertLiteral(limitOffset.limit(), LiteralType.NUMBER);
        Literal offsetLiteral = assertLiteral(limitOffset.offset(), LiteralType.NUMBER);
        assertEquals(5, ((Number) limitLiteral.value()).intValue());
        assertEquals(1, ((Number) offsetLiteral.value()).intValue());

        Star star = assertExpr(Star.class, selectExpr(body, 0));
        assertNull(star.qualifier());
        Star qualifiedStar = assertExpr(Star.class, selectExpr(body, 1));
        assertEquals(List.of("t"), qualifiedStar.qualifier().parts());
        Unary unary = assertExpr(Unary.class, selectExpr(body, 2));
        assertEquals("-", unary.operator());
        Binary binary = assertExpr(Binary.class, selectExpr(body, 3));
        assertEquals(">", binary.operator());
        FuncCall aggregate = assertExpr(FuncCall.class, selectExpr(body, 4));
        assertTrue(aggregate.distinct());
        assertEquals("count", aggregate.name().toLowerCase());
        FuncCall methodCall = assertExpr(FuncCall.class, selectExpr(body, 5));
        assertEquals("pkg.func", methodCall.name().toLowerCase());
        Param param = assertExpr(Param.class, selectExpr(body, 6));
        assertEquals(":id", param.name());
        assertEquals(Integer.valueOf(1), param.index());
        InList inList = assertExpr(InList.class, selectExpr(body, 7));
        assertFalse(inList.not());
        Exists exists = assertExpr(Exists.class, selectExpr(body, 8));
        assertFalse(exists.not());
        CaseExpr mappedCase = assertExpr(CaseExpr.class, selectExpr(body, 9));
        assertEquals(1, mappedCase.items().size());

        Literal stringLiteral = assertLiteral(selectExpr(body, 10), LiteralType.STRING);
        assertEquals("text", stringLiteral.value());
        Literal numberLiteral = assertLiteral(selectExpr(body, 11), LiteralType.NUMBER);
        assertEquals(3.14, ((Number) numberLiteral.value()).doubleValue(), 0.0001);
        Literal booleanLiteral = assertLiteral(selectExpr(body, 12), LiteralType.BOOLEAN);
        assertEquals(Boolean.TRUE, booleanLiteral.value());
        Literal nullLiteral = assertLiteral(selectExpr(body, 13), LiteralType.NULL);
        assertNull(nullLiteral.value());
        Literal hexLiteral = assertLiteral(selectExpr(body, 14), LiteralType.HEX);
        assertEquals("FF", hexLiteral.value());
        Literal dateLiteral = assertLiteral(selectExpr(body, 15), LiteralType.DATE);
        assertEquals("2020-01-01", dateLiteral.value());
        Literal timeLiteral = assertLiteral(selectExpr(body, 16), LiteralType.TIME);
        assertEquals("12:34:56", timeLiteral.value());
        Literal dateTimeLiteral = assertLiteral(selectExpr(body, 17), LiteralType.DATETIME);
        assertEquals("2020-01-01 12:34:56", dateTimeLiteral.value());
    }

    private static Expr selectExpr(SelectBody body, int index) {
        return body.selectItems().get(index).expr();
    }

    private static <T> T assertExpr(Class<T> type, Object value) {
        assertNotNull(value);
        assertTrue(type.isInstance(value));
        return type.cast(value);
    }

    private static Literal assertLiteral(Expr expr, LiteralType type) {
        Literal literal = assertExpr(Literal.class, expr);
        assertEquals(type, literal.type());
        return literal;
    }
}
