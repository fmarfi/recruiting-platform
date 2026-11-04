package com.recruiting.platform.pipeline.application.web.dto;

import com.recruiting.platform.contracts.CloseReason;
import com.recruiting.platform.contracts.PipelineStage;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** OpenAPI StageChangeRequest (rating and rationale since 1.3.0, closeReason since 1.3.0 R65). */
public record StageChangeRequest(@NotNull PipelineStage stage, String reason, CloseReason closeReason, Long version,
                                 String offLimitsOverrideReason, @Min(1) @Max(5) Integer rating,
                                 @Size(max = 2000) String rationale) {
}
