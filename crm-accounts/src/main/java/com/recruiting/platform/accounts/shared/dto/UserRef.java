package com.recruiting.platform.accounts.shared.dto;

import java.util.UUID;

/** Reference to a user (id and display name). */
public record UserRef(UUID id, String name) {
}
