package com.recruiting.platform.pipeline.application.web.dto;

import com.recruiting.platform.contracts.CandidateList;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** OpenAPI RankingRequest. */
public record RankingRequest(@NotNull CandidateList list, @NotNull @Size(min = 1, max = 500) List<UUID> applicationIds) {
}
