package com.recruiting.platform.accounts.view.web.dto;

import com.recruiting.platform.contracts.ListEntity;
import com.recruiting.platform.contracts.ViewVisibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** SavedViewCreateRequest schema. Visibility defaults to PRIVATE. */
public record SavedViewCreateRequest(@NotNull ListEntity entity, @NotBlank @Size(max = 120) String name,
        ViewVisibility visibility, @NotNull @Valid ViewDefinition definition) {
}
