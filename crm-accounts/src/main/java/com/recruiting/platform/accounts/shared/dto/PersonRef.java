package com.recruiting.platform.accounts.shared.dto;

import java.util.UUID;

/** Reference to a person. */
public record PersonRef(UUID id, String name) {
}
