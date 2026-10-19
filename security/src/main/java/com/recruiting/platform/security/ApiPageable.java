package com.recruiting.platform.security;

import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Builds a {@link Pageable} from the contract's page, size and {@code sort=property,asc|desc} parameters. */
public final class ApiPageable {

    private ApiPageable() {
    }

    /**
     * @param sort     raw sort parameters, may be null
     * @param allowed  API property name to entity property name; anything else is a 400
     * @param fallback sort when none is given
     */
    public static Pageable of(Integer page, Integer size, List<String> sort, Map<String, String> allowed,
            Sort fallback) {
        int p = page == null ? 0 : Math.max(page, 0);
        int s = size == null ? 20 : Math.min(Math.max(size, 1), 100);
        if (sort == null || sort.isEmpty()) {
            return PageRequest.of(p, s, fallback);
        }
        List<Sort.Order> orders = new ArrayList<>();
        for (String raw : sort) {
            String[] parts = raw.split(",");
            String property = allowed.get(parts[0]);
            if (property == null || parts.length > 2) {
                throw badSort(parts[0]);
            }
            boolean desc = parts.length == 2 && "desc".equalsIgnoreCase(parts[1]);
            if (parts.length == 2 && !desc && !"asc".equalsIgnoreCase(parts[1])) {
                throw badSort(parts[0]);
            }
            orders.add(desc ? Sort.Order.desc(property) : Sort.Order.asc(property));
        }
        return PageRequest.of(p, s, Sort.by(orders));
    }

    private static ApiException badSort(String property) {
        return new ApiException(ErrorCode.VALIDATION_FAILED, "Cannot sort by '" + property + "'.",
                Map.of("errors", List.of(Map.of("field", "sort", "code", "INVALID",
                        "message", "Unknown sort property."))));
    }
}
