package com.recruiting.platform.pipeline.timeline.web.dto;

import com.recruiting.platform.contracts.EntityType;
import com.recruiting.platform.contracts.TimelineKind;
import com.recruiting.platform.pipeline.shared.dto.JobOrderRef;
import com.recruiting.platform.pipeline.shared.dto.PersonRef;
import com.recruiting.platform.pipeline.shared.dto.UserRef;
import java.time.Instant;
import java.util.UUID;

/** OpenAPI TimelineEntry. */
public record TimelineEntryResponse(UUID id, UUID accountId, Instant occurredAt, TimelineKind kind,
                                    EntityType entityType, UUID entityId, String fromStatus, String toStatus,
                                    String reason, String summary, PersonRef person, JobOrderRef jobOrder,
                                    UserRef actor) {
}
