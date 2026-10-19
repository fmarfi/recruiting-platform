package com.recruiting.platform.pipeline.application.web.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** OpenAPI ApplicationCreateRequest. */
public record ApplicationCreateRequest(@NotNull UUID personId) {
}
