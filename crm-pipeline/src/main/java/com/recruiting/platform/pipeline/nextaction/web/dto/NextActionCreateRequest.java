package com.recruiting.platform.pipeline.nextaction.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/** OpenAPI NextActionCreateRequest. */
public record NextActionCreateRequest(@NotBlank @Size(max = 300) String title, @NotNull LocalDate dueOn,
                                      UUID ownerId, UUID accountId, UUID personId, UUID jobOrderId) {
}
