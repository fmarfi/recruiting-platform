package com.recruiting.platform.pipeline.activity.web.dto;

import com.recruiting.platform.contracts.ActivityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

/** OpenAPI ActivityCreateRequest. At least one of accountId and personId (checked in the service). */
public record ActivityCreateRequest(@NotNull ActivityType type, @NotBlank @Size(max = 4000) String note,
                                    Instant occurredAt, UUID accountId, UUID personId, UUID jobOrderId) {
}
