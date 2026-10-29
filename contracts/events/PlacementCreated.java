package com.recruiting.platform.contracts;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A candidate was placed and the placement recorded (CO-021, since contract 1.3.0, ADR 0013).
 * Publisher: pipeline ({@code POST /applications/{id}/placement}). Listeners: accounts creates an
 * off-limits rule (reason {@link OffLimitsReason#GUARANTEE_PERIOD}) on the client account from
 * today to {@code guaranteeEndOn}, unless an active rule already covers that period; pipeline
 * writes the timeline entry {@link TimelineKind#PLACEMENT}. No pay or fee in the payload.
 *
 * @param eventId        event id
 * @param occurredAt     time of the placement
 * @param actorId        the user who placed
 * @param placementId    the placement
 * @param applicationId  the application (now PLACED)
 * @param jobOrderId     the job order
 * @param accountId      the client account
 * @param personId       the placed person
 * @param startDate      the start date
 * @param guaranteeEndOn last day of the guarantee
 */
public record PlacementCreated(UUID eventId, Instant occurredAt, UUID actorId,
                               UUID placementId, UUID applicationId, UUID jobOrderId,
                               UUID accountId, UUID personId, LocalDate startDate,
                               LocalDate guaranteeEndOn) implements DomainEvent {
}
