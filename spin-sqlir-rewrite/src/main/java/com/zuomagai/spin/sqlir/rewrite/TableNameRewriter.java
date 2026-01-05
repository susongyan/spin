package com.zuomagai.spin.sqlir.rewrite;

import com.zuomagai.spin.sqlir.NamedTable;
import com.zuomagai.spin.sqlir.QName;
import com.zuomagai.spin.sqlir.SqlTransformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class TableNameRewriter extends SqlTransformer {
    private final Function<QName, QName> mapper;

    public TableNameRewriter(Function<QName, QName> mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    public static TableNameRewriter fromMap(Map<String, String> mapping) {
        Objects.requireNonNull(mapping, "mapping");
        Map<String, String> copy = Map.copyOf(mapping);
        return new TableNameRewriter(name -> mapFromMap(name, copy));
    }

    @Override
    public NamedTable transformNamedTable(NamedTable table) {
        QName mapped = mapper.apply(table.name());
        if (mapped == null || mapped.equals(table.name())) {
            return table;
        }
        return new NamedTable(mapped, table.alias(), table.meta());
    }

    private static QName mapFromMap(QName name, Map<String, String> mapping) {
        String fullName = join(name.parts());
        String mapped = mapping.get(fullName);
        if (mapped != null) {
            QName parsed = parseQName(mapped);
            return parsed.equals(name) ? name : parsed;
        }
        if (name.parts().size() > 1) {
            String last = name.last();
            mapped = mapping.get(last);
            if (mapped != null) {
                List<String> parts = new ArrayList<>(name.parts());
                parts.set(parts.size() - 1, mapped);
                QName parsed = new QName(parts);
                return parsed.equals(name) ? name : parsed;
            }
        }
        return name;
    }

    private static String join(List<String> parts) {
        if (parts == null || parts.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                sb.append('.');
            }
            sb.append(parts.get(i));
        }
        return sb.toString();
    }

    private static QName parseQName(String text) {
        List<String> parts = new ArrayList<>();
        for (String part : text.split("\\.")) {
            if (!part.isEmpty()) {
                parts.add(part);
            }
        }
        return new QName(parts);
    }
}
