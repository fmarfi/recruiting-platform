package com.recruiting.platform.contracts;

import java.time.Instant;
import java.util.UUID;

/**
 * Phase 2 (B-07): a consultant accepted an AI-suggested candidate for a job order.
 * Publisher: ai (no publisher in Phase 1). Listeners: pipeline (creates the
 * application in {@link PipelineStage#SOURCED} if the candidate is not on the job yet).
 * The AI never publishes this on its own; it needs a human action (NFR-06).
 *
 * @param eventId    event id
 * @param occurredAt time of the decision
 * @param actorId    consultant who accepted the suggestion
 * @param jobOrderId the job order
 * @param personId   the candidate
 * @param score      AI suitability score 0-100 at the time of the decision
 */
public record CandidateShortlisted(UUID eventId, Instant occurredAt, UUID actorId,
                                   UUID jobOrderId, UUID personId,
                                   int score) implements DomainEvent {
}
