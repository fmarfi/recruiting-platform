package com.recruiting.platform.pipeline.shared.ref;

import com.recruiting.platform.contracts.ConsentStatus;
import com.recruiting.platform.contracts.PersonType;
import com.recruiting.platform.contracts.RetentionStatus;
import java.time.Instant;
import java.util.UUID;

/** What the pipeline module needs to know about a person (the OpenAPI {@code PersonSummary} fields). */
public record PersonInfo(UUID id, String name, String title, String company, PersonType type, String city,
                         String email, UUID ownerId, Instant lastContactAt, ConsentStatus consentStatus,
                         RetentionStatus retentionStatus) {
}
