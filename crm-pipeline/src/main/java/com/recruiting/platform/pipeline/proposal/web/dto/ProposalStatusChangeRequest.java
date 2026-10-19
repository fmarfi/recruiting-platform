package com.recruiting.platform.pipeline.proposal.web.dto;

import com.recruiting.platform.contracts.ProposalStatus;
import jakarta.validation.constraints.NotNull;

/** OpenAPI ProposalStatusChangeRequest. */
public record ProposalStatusChangeRequest(@NotNull ProposalStatus status, String reason, Long version) {
}
