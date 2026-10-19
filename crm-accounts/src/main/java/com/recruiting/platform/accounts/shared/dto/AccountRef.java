package com.recruiting.platform.accounts.shared.dto;

import com.recruiting.platform.contracts.AccountStatus;
import java.util.UUID;

/** Reference to an account. */
public record AccountRef(UUID id, String name, AccountStatus status) {
}
