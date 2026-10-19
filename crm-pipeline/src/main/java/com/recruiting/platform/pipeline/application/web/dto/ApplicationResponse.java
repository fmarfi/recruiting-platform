package com.recruiting.platform.pipeline.application.web.dto;

import com.recruiting.platform.contracts.PipelineStage;
import com.recruiting.platform.pipeline.shared.dto.AccountRef;
import com.recruiting.platform.pipeline.shared.dto.JobOrderRef;
import com.recruiting.platform.pipeline.shared.dto.PersonSummary;
import com.recruiting.platform.pipeline.shared.dto.UserRef;
import java.time.Instant;
import java.util.UUID;

/** OpenAPI Application. {@code addedBy} is the creating user (null until auditing knows the user). */
public record ApplicationResponse(UUID id, JobOrderRef jobOrder, AccountRef account, PersonSummary person,
                                  PipelineStage stage, String rejectionReason, Instant stageChangedAt,
                                  Instant addedAt, UserRef addedBy, Long version) {
}
