package com.recruiting.platform.accounts.account.web.dto;

import com.recruiting.platform.contracts.AccountStatus;
import jakarta.validation.constraints.NotNull;

/** Body of PATCH /accounts/{id}/status. */
public record AccountStatusChangeRequest(@NotNull AccountStatus status, String reason, Long version) {
}
