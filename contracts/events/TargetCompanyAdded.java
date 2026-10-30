package com.recruiting.platform.contracts;

import java.time.Instant;
import java.util.UUID;

/**
 * An organisation in the CRM became a target company of a search (R63/R74, since contract 1.3.0,
 * ADR 0013). Publisher: pipeline ({@code POST /job-orders/{id}/target-companies} with an
 * {@code accountId}; never for a free-text company). Listeners: accounts sets
 * {@code account.is_target = true} (role {@link OrganisationRole#TARGET}); idempotent, the flag is
 * never cleared by events.
 *
 * @param eventId         event id
 * @param occurredAt      time of the change
 * @param actorId         the user who added the target
 * @param targetCompanyId the target company row
 * @param jobOrderId      the search
 * @param accountId       the organisation
 */
public record TargetCompanyAdded(UUID eventId, Instant occurredAt, UUID actorId,
                                 UUID targetCompanyId, UUID jobOrderId, UUID accountId)
        implements DomainEvent {
}
