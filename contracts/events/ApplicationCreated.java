package com.recruiting.platform.contracts;

import java.time.Instant;
import java.util.UUID;

/**
 * A candidate was added to a job order's pipeline (A-07). Publisher: pipeline.
 * Listeners: pipeline (timeline). Adding a candidate is not a contact, so the
 * person's last-contact date does not change.
 *
 * @param eventId       event id
 * @param occurredAt    time of the change
 * @param actorId       user who added the candidate
 * @param applicationId the application
 * @param jobOrderId    the job order
 * @param accountId     the job order's account
 * @param personId      the candidate
 * @param stage         initial stage
 */
public record ApplicationCreated(UUID eventId, Instant occurredAt, UUID actorId,
                                 UUID applicationId, UUID jobOrderId, UUID accountId,
                                 UUID personId, PipelineStage stage) implements DomainEvent {
}
