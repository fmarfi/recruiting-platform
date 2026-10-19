package com.recruiting.platform.pipeline.joborder.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/** OpenAPI JobOrderCreateRequest. */
public record JobOrderCreateRequest(@NotNull UUID accountId, @NotBlank @Size(max = 200) String title,
                                    @Size(max = 200) String location, @Size(max = 100) String seniority,
                                    @Size(max = 10000) String description, @Size(max = 2000) String feeTerms,
                                    @Min(1) Integer openings, LocalDate openedOn, UUID ownerId) {
}
