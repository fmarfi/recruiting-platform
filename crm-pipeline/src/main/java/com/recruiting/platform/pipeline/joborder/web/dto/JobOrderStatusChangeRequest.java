package com.recruiting.platform.pipeline.joborder.web.dto;

import com.recruiting.platform.contracts.JobOrderStatus;
import jakarta.validation.constraints.NotNull;

/** OpenAPI JobOrderStatusChangeRequest. */
public record JobOrderStatusChangeRequest(@NotNull JobOrderStatus status, String reason, Long version) {
}
