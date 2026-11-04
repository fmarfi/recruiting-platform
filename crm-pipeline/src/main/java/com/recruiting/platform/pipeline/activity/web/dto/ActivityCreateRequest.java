package com.recruiting.platform.pipeline.activity.web.dto;

import com.recruiting.platform.contracts.ActivityDirection;
import com.recruiting.platform.contracts.ActivityOutcome;
import com.recruiting.platform.contracts.ActivitySource;
import com.recruiting.platform.contracts.ActivityType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** OpenAPI ActivityCreateRequest. At least one of accountId and personId (checked in the service). */
public record ActivityCreateRequest(@NotNull ActivityType type, @NotBlank @Size(max = 4000) String note,
                                    Instant occurredAt, UUID accountId, UUID personId, UUID jobOrderId,
                                    ActivityOutcome outcome, ActivityDirection direction,
                                    @Size(max = 20) List<UUID> attendeePersonIds,
                                    @Min(0) @Max(1440) Integer durationMin, ActivitySource source,
                                    @Size(min = 1, max = 300) String externalMessageId) {
}
