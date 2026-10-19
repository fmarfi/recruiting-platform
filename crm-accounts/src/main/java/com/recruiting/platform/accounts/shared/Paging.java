package com.recruiting.platform.accounts.shared;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Builds a Pageable from page/size/sort query parameters (ADR 0006) with a whitelist of sort keys. */
public final class Paging {

    private Paging() {
    }

    /**
     * @param allowed  API sort key to entity property
     * @param fallback used when no sort is given
     */
    public static Pageable of(Integer page, Integer size, List<String> sort, Map<String, String> allowed,
            Sort fallback) {
        int p = page == null ? 0 : page;
        int s = size == null ? 20 : size;
        if (p < 0) {
            throw PatchBinder.validation("page", "MIN", "must be 0 or greater");
        }
        if (s < 1 || s > 100) {
            throw PatchBinder.validation("size", "RANGE", "must be between 1 and 100");
        }
        Sort sorting = fallback;
        if (sort != null && !sort.isEmpty()) {
            sorting = Sort.unsorted();
            for (String spec : sort) {
                String[] parts = spec.split(",");
                String prop = allowed.get(parts[0].trim());
                if (prop == null) {
                    throw PatchBinder.validation("sort", "PATTERN", "unsupported sort key: " + parts[0]);
                }
                boolean desc = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim());
                Sort.Order order = desc ? Sort.Order.desc(prop) : Sort.Order.asc(prop);
                sorting = sorting.and(Sort.by(order.nullsLast()));
            }
        }
        return PageRequest.of(p, s, sorting);
    }
}
