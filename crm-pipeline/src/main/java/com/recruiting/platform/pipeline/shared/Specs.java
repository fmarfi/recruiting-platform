package com.recruiting.platform.pipeline.shared;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

/** Helpers for JPA Specifications. */
public final class Specs {

    private static final char ESCAPE = '!';

    private Specs() {
    }

    /**
     * Accent- and case-insensitive contains match, served by the immutable_unaccent function of the schema.
     *
     * @param cb    criteria builder
     * @param field the text expression
     * @param q     the user input (LIKE wildcards are escaped)
     * @return the predicate
     */
    public static Predicate containsUnaccented(CriteriaBuilder cb, Expression<String> field, String q) {
        String escaped = q.trim().toLowerCase().replace("!", "!!").replace("%", "!%").replace("_", "!_");
        Expression<String> left = cb.function("immutable_unaccent", String.class, cb.lower(field));
        Expression<String> right = cb.function("immutable_unaccent", String.class, cb.literal("%" + escaped + "%"));
        return cb.like(left, right, ESCAPE);
    }
}
