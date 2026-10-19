package com.recruiting.platform.pipeline.shared.dto;

import com.recruiting.platform.contracts.AccountStatus;
import java.util.UUID;

/** OpenAPI AccountRef. */
public record AccountRef(UUID id, String name, AccountStatus status) {
}
