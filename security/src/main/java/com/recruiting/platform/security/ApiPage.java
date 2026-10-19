package com.recruiting.platform.security;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/** Page envelope of the REST API ({@code content} + {@code page} meta, contract schema PageMeta). */
public record ApiPage<T>(List<T> content, PageMeta page) {

    /** Paging numbers of the contract: size, 0-based number, totalElements, totalPages. */
    public record PageMeta(int size, int number, long totalElements, int totalPages) {
    }

    public static <E, T> ApiPage<T> of(Page<E> page, Function<E, T> mapper) {
        return new ApiPage<>(page.getContent().stream().map(mapper).toList(),
                new PageMeta(page.getSize(), page.getNumber(), page.getTotalElements(), page.getTotalPages()));
    }
}
