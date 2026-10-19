package com.recruiting.platform.pipeline.shared.dto;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/** Paged list as in the contract: content plus page metadata. */
public record PageResponse<T>(List<T> content, PageMeta page) {

    /** Page metadata. */
    public record PageMeta(int size, int number, long totalElements, int totalPages) {
    }

    /**
     * Wraps a Spring page.
     *
     * @param p      the page
     * @param <E>    entity type
     * @param <T>    DTO type
     * @param mapper maps the whole content list at once (so lookups can be batched)
     * @return the response
     */
    public static <E, T> PageResponse<T> of(Page<E> p, Function<List<E>, List<T>> mapper) {
        return new PageResponse<>(mapper.apply(p.getContent()),
                new PageMeta(p.getSize(), p.getNumber(), p.getTotalElements(), p.getTotalPages()));
    }
}
