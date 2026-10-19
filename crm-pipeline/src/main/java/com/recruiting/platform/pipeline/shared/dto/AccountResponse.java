package com.recruiting.platform.pipeline.shared.dto;

import com.recruiting.platform.contracts.AccountStatus;
import com.recruiting.platform.pipeline.shared.ref.AccountInfo;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** OpenAPI Account, as embedded in My day (the accounts module serves the full resource itself). */
public record AccountResponse(UUID id, String name, String sector, String city, String website, String notes,
                              UserRef owner, AccountStatus status, Instant statusChangedAt, LocalDate openedOn,
                              int openJobCount, Instant lastActivityAt, Instant createdAt, Instant updatedAt,
                              Long version) {

    /**
     * Builds the response from the accounts module's data.
     *
     * @param a     account info
     * @param owner resolved owner
     * @return the response
     */
    public static AccountResponse of(AccountInfo a, UserRef owner) {
        return new AccountResponse(a.id(), a.name(), a.sector(), a.city(), a.website(), a.notes(), owner, a.status(),
                a.statusChangedAt(), a.openedOn(), a.openJobCount(), a.lastActivityAt(), a.createdAt(),
                a.updatedAt(), a.version());
    }
}
