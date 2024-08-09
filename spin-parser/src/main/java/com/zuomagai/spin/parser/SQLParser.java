package com.zuomagai.spin.parser;

import com.zuomagai.spin.parser.util.LRUCache;

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
        CACHE.put(sql, result);
        //todo parse
        // what to include? used in rule matching &
        // - raw statement
        // - tables
        // - column -> value pairs
        return result;
    }
}
