package com.zuomagai.spin.parser;

import com.zuomagai.spin.parser.generate.MySqlParser;
import com.zuomagai.spin.parser.generate.MySqlParserBaseVisitor;
import org.antlr.v4.runtime.TokenStreamRewriter;

import java.util.Map;

public class MySQLRewriter extends MySqlParserBaseVisitor {

    private final TokenStreamRewriter tokenStreamRewriter;
    private final Map<String, String> tableMapping;

    public MySQLRewriter(TokenStreamRewriter tokenStreamRewriter, Map<String, String> tableMapping) {
        this.tokenStreamRewriter = tokenStreamRewriter;
        this.tableMapping = tableMapping;
    }

    @Override
    public Object visitTableName(MySqlParser.TableNameContext ctx) {
        // TODO rewrite table name
        return super.visitTableName(ctx);
    }

    public String toSQL() {
        return this.tokenStreamRewriter.getText();
    }
}
