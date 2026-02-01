package com.zuomagai.spin.demo;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.zuomagai.spin.mapper.DruidToSqlirMapper;
import com.zuomagai.spin.sqlir.Statement;
import com.zuomagai.spin.sqlir.emit.MySqlEmitter;
import com.zuomagai.spin.sqlir.emit.SqlEmitter;
import com.zuomagai.spin.sqlir.rewrite.TableNameRewriter;

import java.util.List;
import java.util.Map;

public final class TableRewriteDemo {
    private TableRewriteDemo() {
    }

    public static void main(String[] args) {
        Map<String, String> mapping = Map.of(
                "users", "users_2024",
                "orders", "orders_2024",
                "app.audit", "app.audit_2024"
        );
        List<String> sqls = List.of(
                "select u.id, o.total from users u join orders o on u.id = o.user_id where u.status = 1",
                "insert into app.audit (id, action) values (1, 'login')",
                "update users set name = 'bob' where id = 7"
        );

        DruidToSqlirMapper mapper = new DruidToSqlirMapper();
        TableNameRewriter rewriter = TableNameRewriter.fromMap(mapping);
        SqlEmitter emitter = new MySqlEmitter();

        for (String sql : sqls) {
            String rewritten = rewriteSql(sql, mapper, rewriter, emitter);
            System.out.println("Original:  " + sql);
            System.out.println("Rewritten: " + rewritten);
            System.out.println();
        }
    }

    private static String rewriteSql(String sql, DruidToSqlirMapper mapper,
                                     TableNameRewriter rewriter, SqlEmitter emitter) {
        SQLStatement statement = SQLUtils.parseSingleStatement(sql, DbType.mysql);
        Statement ir = mapper.mapStatement(statement);
        Statement rewritten = (Statement) rewriter.transform(ir);
        return emitter.emit(rewritten);
    }
}
