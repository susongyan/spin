package com.zuomagai.spin.parser;

import com.zuomagai.spin.parser.generate.MySqlParser;
import com.zuomagai.spin.parser.generate.MySqlParserBaseVisitor;
import org.antlr.v4.runtime.TokenStreamRewriter;

public class MySQLRewriter extends MySqlParserBaseVisitor {

    private TokenStreamRewriter tokenStreamRewriter;

    public MySQLRewriter(TokenStreamRewriter tokenStreamRewriter) {
        this.tokenStreamRewriter = tokenStreamRewriter;
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
