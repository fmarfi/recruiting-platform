package com.recruiting.platform.contracts;

import java.time.Instant;
import java.util.UUID;

/**
 * A candidate was moved from one job order to another (since contract 1.5.0, ADR 0015;
 * {@code POST /applications/{id}/move} and {@code POST /application-moves}). Publisher: pipeline.
 * Listeners: pipeline (one timeline entry {@code CANDIDATE_MOVED} per distinct account).
 *
 * <p>A move publishes this event <em>instead of</em> {@link ApplicationStageChanged} for the source
 * and {@link ApplicationCreated} for the target, so the timeline shows one entry, not three.
 * A listener that needs every stage change or every new application must also handle this event.
 * Ids, stages and the optional reason only (events.md rule 9).
 *
 * @param eventId             event id
 * @param occurredAt          time of the move
 * @param actorId             user who moved the candidate
 * @param personId            the candidate
 * @param sourceApplicationId the application on the old job order (kept; closed when it was active)
 * @param sourceJobOrderId    the old job order
 * @param sourceAccountId     the old job order's account
 * @param sourceStageBefore   stage of the source before the move
 * @param sourceClosed        {@code true} when the move closed the source (active to REJECTED with
 *                            {@link CloseReason#MOVED_TO_OTHER_SEARCH}); {@code false} when it was
 *                            already REJECTED
 * @param targetApplicationId the new application
 * @param targetJobOrderId    the new job order
 * @param targetAccountId     the new job order's account (may equal {@code sourceAccountId})
 * @param targetStage         starting stage on the target (SOURCED or SCREENED)
 * @param reason              optional free-text reason (3 to 500 characters), or {@code null}
 */
public record CandidateMoved(UUID eventId, Instant occurredAt, UUID actorId,
                             UUID personId,
                             UUID sourceApplicationId, UUID sourceJobOrderId, UUID sourceAccountId,
                             PipelineStage sourceStageBefore, boolean sourceClosed,
                             UUID targetApplicationId, UUID targetJobOrderId, UUID targetAccountId,
                             PipelineStage targetStage, String reason) implements DomainEvent {
}
