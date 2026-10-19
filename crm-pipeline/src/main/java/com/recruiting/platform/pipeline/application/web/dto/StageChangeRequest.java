package com.recruiting.platform.pipeline.application.web.dto;

import com.recruiting.platform.contracts.PipelineStage;
import jakarta.validation.constraints.NotNull;

/** OpenAPI StageChangeRequest. */
public record StageChangeRequest(@NotNull PipelineStage stage, String reason, Long version) {
}
