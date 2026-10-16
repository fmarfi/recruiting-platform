package com.recruiting.platform.contracts;

import java.time.Instant;
import java.util.UUID;

/**
 * A proposal moved to another status (A-05). Publisher: pipeline. Listeners: pipeline
 * (timeline).
 *
 * @param eventId    event id
 * @param occurredAt time of the change
 * @param actorId    user who changed it
 * @param proposalId the proposal
 * @param accountId  its account
 * @param from       previous status
 * @param to         new status
 * @param reason     reason given; always set when {@code to} is {@link ProposalStatus#REJECTED}
 */
public record ProposalStatusChanged(UUID eventId, Instant occurredAt, UUID actorId,
                                    UUID proposalId, UUID accountId, ProposalStatus from,
                                    ProposalStatus to, String reason) implements DomainEvent {
}
