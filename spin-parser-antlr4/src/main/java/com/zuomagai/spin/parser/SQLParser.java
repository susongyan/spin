package com.zuomagai.spin.parser;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.util.JdbcConstants;
import com.zuomagai.spin.parser.generate.PlSqlLexer;
import com.zuomagai.spin.parser.generate.PlSqlParser;
import com.zuomagai.spin.parser.util.LRUCache;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.atn.PredictionMode;

import java.util.Collections;
import java.util.Map;

public class SQLParser {
    private static final int MAX_CACHE_SIZE = 1024;
    private static final Map<String, SQLParseResult> CACHE = Collections.synchronizedMap(new LRUCache<>(MAX_CACHE_SIZE));

    public static SQLParseResult parse(String sql) {
        if (CACHE.containsKey(sql)) {
            return CACHE.get(sql);
        }

        SQLParseResult result = new SQLParseResult();

        long begin = System.currentTimeMillis();
//        MySqlLexer lexer = new MySqlLexer(CharStreams.fromString(sql));
//        CommonTokenStream tokens = new CommonTokenStream(lexer);
//        MySqlParser parser = new MySqlParser(tokens);
//        parser.setErrorHandler(new BailErrorStrategy()); // fail-fast   catch , back
//        MySqlParser.RootContext rootContext = parser.root();
//
//
//        MySQLVisitor visitor = new MySQLVisitor();
//        visitor.visit(rootContext);

        PlSqlLexer lexer = new PlSqlLexer(CharStreams.fromString(sql));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlSqlParser parser = new PlSqlParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
        PlSqlParser.Data_manipulation_language_statementsContext context = parser.data_manipulation_language_statements();

        System.out.println(System.currentTimeMillis() - begin + "ms");

        //todo parse
        // what to include? used in rule matching &
        // - raw statement
        // - tables
        // - column -> value pairs
//        CACHE.put(sql, result);
        return result;
    }

    public static void main(String[] args) throws JSQLParserException, InterruptedException, ClassNotFoundException {


//        Class.forName(MySqlLexer.class.getName());

        String sql = "select COUNT(*) from t_user";
        long begin = System.currentTimeMillis();
        SQLParser.parse(sql);
        System.out.println("antlr4 parse: " + (System.currentTimeMillis() - begin) + "ms");

        begin = System.currentTimeMillis();
        CCJSqlParserUtil.parse(sql);
        System.out.println("jsql parser: " + (System.currentTimeMillis() - begin) + "ms");

        begin = System.currentTimeMillis();
        String dbType = JdbcConstants.ORACLE_DRIVER;
        SQLStatement statement = SQLUtils.parseSingleStatement(sql, dbType);
        System.out.println("druid parser: " + (System.currentTimeMillis() - begin) + "ms");
    }
}
