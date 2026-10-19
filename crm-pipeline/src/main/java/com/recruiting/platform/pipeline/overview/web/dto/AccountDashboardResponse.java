package com.recruiting.platform.pipeline.overview.web.dto;

import com.recruiting.platform.contracts.PipelineStage;
import com.recruiting.platform.pipeline.nextaction.web.dto.NextActionResponse;
import java.util.Map;
import java.util.UUID;

/** OpenAPI AccountDashboard. */
public record AccountDashboardResponse(UUID accountId, long openJobOrders,
                                       Map<PipelineStage, Long> candidatesPerStage, Double avgDaysOpen,
                                       long placedCount, long openProposals, NextActionResponse nextAction) {
}
