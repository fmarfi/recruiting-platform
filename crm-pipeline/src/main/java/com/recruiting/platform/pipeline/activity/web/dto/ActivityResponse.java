package com.recruiting.platform.pipeline.activity.web.dto;

import com.recruiting.platform.contracts.ActivityType;
import com.recruiting.platform.pipeline.shared.dto.AccountRef;
import com.recruiting.platform.pipeline.shared.dto.JobOrderRef;
import com.recruiting.platform.pipeline.shared.dto.PersonRef;
import com.recruiting.platform.pipeline.shared.dto.UserRef;
import java.time.Instant;
import java.util.UUID;

/** OpenAPI Activity. */
public record ActivityResponse(UUID id, ActivityType type, String note, Instant occurredAt, AccountRef account,
                               PersonRef person, JobOrderRef jobOrder, UserRef user, Instant createdAt,
                               Long version) {
}
