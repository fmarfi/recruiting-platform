package com.recruiting.platform.security.audit.web.dto;

import com.recruiting.platform.contracts.AccessAction;
import com.recruiting.platform.contracts.EntityType;
import com.recruiting.platform.security.UserRef;
import java.time.Instant;
import java.util.UUID;

/** Contract schema AccessLogEntry. */
public record AccessLogEntryResponse(UUID id, UserRef user, AccessAction action, EntityType entityType,
        UUID entityId, Instant at, String ipAddress) {
}
