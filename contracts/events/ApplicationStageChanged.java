package com.recruiting.platform.contracts;

import java.time.Instant;
import java.util.UUID;

/**
 * A candidate moved to another pipeline stage (A-07). Publisher: pipeline. Listeners:
 * pipeline (timeline, dashboard counts).
 *
 * @param eventId       event id
 * @param occurredAt    time of the change
 * @param actorId       user who moved the candidate
 * @param applicationId the application
 * @param jobOrderId    the job order
 * @param accountId     the job order's account
 * @param personId      the candidate
 * @param from          previous stage
 * @param to            new stage
 * @param reason        reason given; always set when {@code to} is {@link PipelineStage#REJECTED}
 */
public record ApplicationStageChanged(UUID eventId, Instant occurredAt, UUID actorId,
                                      UUID applicationId, UUID jobOrderId, UUID accountId,
                                      UUID personId, PipelineStage from, PipelineStage to,
                                      String reason) implements DomainEvent {
}
