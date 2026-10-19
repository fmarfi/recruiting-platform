package com.recruiting.platform.pipeline.shared.dto;

import com.recruiting.platform.contracts.JobOrderStatus;
import java.util.UUID;

/** OpenAPI JobOrderRef. */
public record JobOrderRef(UUID id, String title, JobOrderStatus status) {
}
