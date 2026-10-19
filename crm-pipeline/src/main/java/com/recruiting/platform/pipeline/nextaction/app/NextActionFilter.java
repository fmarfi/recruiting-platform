package com.recruiting.platform.pipeline.nextaction.app;

import java.time.LocalDate;
import java.util.UUID;

/** Filters of GET /next-actions. */
public record NextActionFilter(Boolean mine, UUID ownerId, UUID accountId, UUID personId, UUID jobOrderId,
                               Boolean done, LocalDate dueBefore) {
}
