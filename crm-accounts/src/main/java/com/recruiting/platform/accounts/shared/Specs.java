package com.recruiting.platform.accounts.shared;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

/** Accent- and case-insensitive LIKE on the trigram-indexed expression immutable_unaccent(lower(col)). */
public final class Specs {

    private Specs() {
    }

    public static String escapeLike(String q) {
        return q.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    public static Predicate unaccentContains(CriteriaBuilder cb, Path<String> column, String q) {
        Expression<String> left = cb.function("immutable_unaccent", String.class, cb.lower(column));
        Expression<String> right = cb.function("immutable_unaccent", String.class,
                cb.lower(cb.literal("%" + escapeLike(q) + "%")));
        return cb.like(left, right, '\\');
    }
}
