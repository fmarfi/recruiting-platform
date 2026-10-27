package com.recruiting.platform.pipeline.exposure.web.dto;

import com.recruiting.platform.contracts.ExposureChannel;
import com.recruiting.platform.contracts.PipelineStage;
import com.recruiting.platform.pipeline.shared.dto.AccountRef;
import com.recruiting.platform.pipeline.shared.dto.JobOrderRef;
import com.recruiting.platform.pipeline.shared.dto.UserRef;
import java.time.Instant;
import java.util.UUID;

/**
 * OpenAPI ExposureEntry: one presentation of a person to a client account. {@code stage} is the current stage of the
 * application (the outcome so far), null when the application was removed.
 */
public record ExposureResponse(UUID id, AccountRef account, JobOrderRef jobOrder, UUID applicationId,
        ExposureChannel channel, Instant presentedAt, UserRef presentedBy, PipelineStage stage) {
}
