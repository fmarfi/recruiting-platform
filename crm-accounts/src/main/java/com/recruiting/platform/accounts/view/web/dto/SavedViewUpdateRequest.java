package com.recruiting.platform.accounts.view.web.dto;

import com.recruiting.platform.contracts.ViewVisibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

/** PATCH body: absent field = unchanged (see PatchBinder). The entity cannot change. */
public record SavedViewUpdateRequest(@Size(min = 1, max = 120) String name, ViewVisibility visibility,
        @Valid ViewDefinition definition, Long version) {
}
