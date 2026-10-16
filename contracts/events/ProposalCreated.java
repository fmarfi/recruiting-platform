package com.recruiting.platform.contracts;

import java.time.Instant;
import java.util.UUID;

/**
 * A proposal was drafted for an account (A-05). Publisher: pipeline. Listeners:
 * pipeline (timeline).
 *
 * @param eventId    event id
 * @param occurredAt time of the change
 * @param actorId    user who created it
 * @param proposalId the proposal
 * @param accountId  its account
 * @param title      what the proposal covers
 */
public record ProposalCreated(UUID eventId, Instant occurredAt, UUID actorId,
                              UUID proposalId, UUID accountId,
                              String title) implements DomainEvent {
}
