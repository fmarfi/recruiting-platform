package com.recruiting.platform.accounts.account.web.dto;

import com.recruiting.platform.accounts.shared.dto.UserRef;
import com.recruiting.platform.contracts.AccountStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Account as in the Account schema. */
public record AccountResponse(UUID id, String name, String sector, String city, String website, String notes,
        UserRef owner, AccountStatus status, Instant statusChangedAt, LocalDate openedOn, int openJobCount,
        Instant lastActivityAt, Instant createdAt, Instant updatedAt, long version) {
}
