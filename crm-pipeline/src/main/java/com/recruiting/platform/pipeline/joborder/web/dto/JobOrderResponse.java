package com.recruiting.platform.pipeline.joborder.web.dto;

import com.recruiting.platform.contracts.JobOrderStatus;
import com.recruiting.platform.contracts.PipelineStage;
import com.recruiting.platform.pipeline.shared.dto.AccountRef;
import com.recruiting.platform.pipeline.shared.dto.UserRef;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/** OpenAPI JobOrder. */
public record JobOrderResponse(UUID id, AccountRef account, String title, String location, String seniority,
                               String description, String feeTerms, int openings, JobOrderStatus status,
                               Instant statusChangedAt, UserRef owner, LocalDate openedOn, LocalDate closedOn,
                               long daysOpen, long activeCandidates, Map<PipelineStage, Long> stageCounts,
                               Instant createdAt, Instant updatedAt, Long version) {
}
