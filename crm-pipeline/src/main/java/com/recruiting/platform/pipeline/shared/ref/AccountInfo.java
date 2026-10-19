package com.recruiting.platform.pipeline.shared.ref;

import com.recruiting.platform.contracts.AccountStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * What the pipeline module needs to know about an account (the OpenAPI {@code Account} fields). Supplied by
 * the accounts module through {@link AccountDirectory}; nullable fields are unknown to a fallback directory.
 */
public record AccountInfo(UUID id, String name, String sector, String city, String website, String notes,
                          UUID ownerId, AccountStatus status, Instant statusChangedAt, LocalDate openedOn,
                          int openJobCount, Instant lastActivityAt, Instant createdAt, Instant updatedAt,
                          Long version) {
}
