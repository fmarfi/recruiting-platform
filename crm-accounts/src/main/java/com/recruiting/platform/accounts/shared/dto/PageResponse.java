package com.recruiting.platform.accounts.shared.dto;

import java.util.List;

/** Page of results in the PagedModel JSON shape. */
public record PageResponse<T>(List<T> content, PageMeta page) {

    /** Paging block from {@code p}, content already mapped by the caller. */
    public static <T> PageResponse<T> of(org.springframework.data.domain.Page<?> p, List<T> content) {
        return new PageResponse<>(content,
                new PageMeta(p.getSize(), p.getNumber(), p.getTotalElements(), p.getTotalPages()));
    }
}
