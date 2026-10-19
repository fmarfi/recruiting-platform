package com.recruiting.platform.pipeline.proposal.web.dto;

import com.recruiting.platform.contracts.ProposalStatus;
import com.recruiting.platform.pipeline.shared.dto.AccountRef;
import com.recruiting.platform.pipeline.shared.dto.UserRef;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** OpenAPI Proposal. feeAmount is a decimal string. */
public record ProposalResponse(UUID id, AccountRef account, String title, String terms, String feeAmount,
                               String currency, ProposalStatus status, Instant statusChangedAt, String statusReason,
                               LocalDate sentOn, LocalDate decidedOn, UserRef owner, Instant createdAt,
                               Instant updatedAt, Long version) {
}
