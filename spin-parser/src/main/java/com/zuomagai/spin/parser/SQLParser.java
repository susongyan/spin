package com.zuomagai.spin.parser;

import com.zuomagai.spin.parser.generate.MySqlLexer;
import com.zuomagai.spin.parser.generate.MySqlParser;
import com.zuomagai.spin.parser.util.LRUCache;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.Collections;
import java.util.Map;

public class SQLParser {
    private static final int MAX_CACHE_SIZE = 1024;
    private static Map<String, SQLParseResult> CACHE = Collections.synchronizedMap(new LRUCache<>(MAX_CACHE_SIZE));

    public static SQLParseResult parse(String sql) {
        if (CACHE.containsKey(sql)) {
            return CACHE.get(sql);
        }

        SQLParseResult result = new SQLParseResult();

        MySqlLexer lexer = new MySqlLexer(CharStreams.fromString(sql));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        MySqlParser parser = new MySqlParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy()); // fail-fast   catch , back
        MySqlParser.RootContext rootContext = parser.root();

        MySQLVisitor visitor = new MySQLVisitor();
        visitor.visit(rootContext);

        //todo parse
        // what to include? used in rule matching &
        // - raw statement
        // - tables
        // - column -> value pairs
        CACHE.put(sql, result);
        return result;
    }
}
