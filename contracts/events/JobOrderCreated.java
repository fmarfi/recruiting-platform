package com.recruiting.platform.contracts;

import java.time.Instant;
import java.util.UUID;

/**
 * A job order was opened inside an account (A-06). Publisher: pipeline. Listeners:
 * accounts (open job count on the account), pipeline (timeline).
 *
 * @param eventId    event id
 * @param occurredAt time of the change
 * @param actorId    user who created it
 * @param jobOrderId the job order
 * @param accountId  its account
 * @param title      role title
 * @param ownerId    lead consultant
 * @param status     initial status
 */
public record JobOrderCreated(UUID eventId, Instant occurredAt, UUID actorId,
                              UUID jobOrderId, UUID accountId, String title, UUID ownerId,
                              JobOrderStatus status) implements DomainEvent {
}
