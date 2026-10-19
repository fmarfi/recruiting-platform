package com.recruiting.platform.pipeline.shared.dto;

import com.recruiting.platform.contracts.ConsentStatus;
import com.recruiting.platform.contracts.PersonType;
import com.recruiting.platform.contracts.RetentionStatus;
import java.time.Instant;
import java.util.UUID;

/** OpenAPI PersonSummary. */
public record PersonSummary(UUID id, String name, String title, String company, PersonType type, String city,
                            String email, UserRef owner, Instant lastContactAt, ConsentStatus consentStatus,
                            RetentionStatus retentionStatus) {
}
