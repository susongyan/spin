package com.zuomagai.spin.sqlir.rewrite;

import com.zuomagai.spin.sqlir.SqlNode;
import com.zuomagai.spin.sqlir.SqlTransformer;
import com.zuomagai.spin.sqlir.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RewritePipeline {
    private final List<SqlTransformer> transformers;

    public RewritePipeline(List<SqlTransformer> transformers) {
        Objects.requireNonNull(transformers, "transformers");
        this.transformers = List.copyOf(transformers);
    }

    public static RewritePipeline of(SqlTransformer... transformers) {
        Objects.requireNonNull(transformers, "transformers");
        List<SqlTransformer> list = new ArrayList<>(transformers.length);
        for (SqlTransformer transformer : transformers) {
            if (transformer == null) {
                throw new IllegalArgumentException("transformer must not be null");
            }
            list.add(transformer);
        }
        return new RewritePipeline(list);
    }

    public Statement rewrite(Statement statement) {
        Objects.requireNonNull(statement, "statement");
        SqlNode current = statement;
        for (SqlTransformer transformer : transformers) {
            current = transformer.transform(current);
        }
        return (Statement) current;
    }

    public List<SqlTransformer> transformers() {
        return transformers;
    }
}
