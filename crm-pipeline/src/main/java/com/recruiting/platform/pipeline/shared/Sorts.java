package com.recruiting.platform.pipeline.shared;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;

/** Parses {@code sort=property,asc|desc} against a whitelist of API names mapped to entity properties. */
public final class Sorts {

    private Sorts() {
    }

    /**
     * Parses sort parameters.
     *
     * @param params   raw values, may be {@code null} or empty
     * @param allowed  API name to entity property
     * @param fallback sort used when nothing is given
     * @return the sort
     */
    public static Sort parse(List<String> params, Map<String, String> allowed, Sort fallback) {
        if (params == null || params.isEmpty()) {
            return fallback;
        }
        List<Sort.Order> orders = new ArrayList<>();
        for (String p : params) {
            String[] parts = p.split(",");
            String property = allowed.get(parts[0].trim());
            if (property == null) {
                throw Transitions.invalid("sort", "Cannot sort by " + parts[0] + ".");
            }
            boolean desc = parts.length > 1 && parts[1].trim().equalsIgnoreCase("desc");
            orders.add(desc ? Sort.Order.desc(property) : Sort.Order.asc(property));
        }
        return Sort.by(orders);
    }
}
