package com.recruiting.platform.pipeline.proposal.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** OpenAPI ProposalCreateRequest. */
public record ProposalCreateRequest(@NotNull UUID accountId, @NotBlank @Size(max = 200) String title,
                                    @Size(max = 10000) String terms,
                                    @Pattern(regexp = "^\\d+(\\.\\d{1,2})?$") String feeAmount,
                                    @Pattern(regexp = "^[A-Z]{3}$") String currency, UUID ownerId) {
}
