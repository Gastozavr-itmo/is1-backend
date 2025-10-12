package ru.se.ifmo.is1.repository.util;

import java.util.*;

public class SortSupport {

    public record Rule(String orderByExpr, String joinClause) {
        public static Rule column(String qualifiedColumn) {
            return new Rule(qualifiedColumn, null);
        }
        public static Rule joined(String qualifiedColumn, String joinClause) {
            return new Rule(qualifiedColumn, joinClause);
        }
    }

    public static Built build(Map<String, Rule> whitelist, String sortKey, String dir, String defaultKey) {
        String key = (sortKey == null || sortKey.isBlank()) ? defaultKey : sortKey;
        Rule r = whitelist.get(key);
        if (r == null) {
            throw new IllegalArgumentException("Invalid sort field: " + key);
        }
        String direction = "desc".equalsIgnoreCase(dir) ? "desc" : "asc";
        String orderBy = r.orderByExpr() + " " + direction;
        Set<String> joins = new LinkedHashSet<>();
        if (r.joinClause() != null && !r.joinClause().isBlank()) joins.add(r.joinClause());
        return new Built(orderBy, joins);
    }

    public record Built(String orderBy, Set<String> joins) {}
}
