package com.recruiting.platform.accounts.view.web.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/** ViewDefinition schema: what a grid shows. Shape and values are validated per entity by ViewDefinitionValidator. */
public record ViewDefinition(@NotNull Map<String, Object> filters, @NotNull List<String> columns,
        @NotNull List<String> sort, String groupBy) {
}
