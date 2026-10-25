package com.recruiting.platform.accounts.view.web.dto;

import com.recruiting.platform.contracts.ListEntity;
import com.recruiting.platform.contracts.ViewVisibility;
import com.recruiting.platform.security.UserRef;
import java.time.Instant;
import java.util.UUID;

/** SavedView schema. */
public record SavedViewResponse(UUID id, ListEntity entity, String name, ViewVisibility visibility, UserRef owner,
        ViewDefinition definition, boolean editable, Instant createdAt, Instant updatedAt, long version) {
}
