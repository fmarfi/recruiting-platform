package com.recruiting.platform.pipeline.application.web.dto;

import com.recruiting.platform.contracts.PipelineStage;
import com.recruiting.platform.pipeline.shared.dto.AccountRef;
import com.recruiting.platform.pipeline.shared.dto.JobOrderRef;
import com.recruiting.platform.pipeline.shared.dto.PersonSummary;
import com.recruiting.platform.pipeline.shared.dto.UserRef;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** OpenAPI Application. {@code addedBy} is the creating user (null until auditing knows the user). */
public record ApplicationResponse(UUID id, JobOrderRef jobOrder, AccountRef account, PersonSummary person,
                                  PipelineStage stage, String rejectionReason, Instant stageChangedAt,
                                  Instant addedAt, UserRef addedBy, Long version,
                                  String offLimitsOverrideReason, List<PipelineWarning> warnings) {

    /** Copy with the non-blocking warnings of an add or a client-facing move. */
    public ApplicationResponse withWarnings(List<PipelineWarning> w) {
        return new ApplicationResponse(id, jobOrder, account, person, stage, rejectionReason, stageChangedAt, addedAt,
                addedBy, version, offLimitsOverrideReason, w);
    }
}
