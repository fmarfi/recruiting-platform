package com.recruiting.platform.accounts.person.web.dto;

import com.recruiting.platform.accounts.shared.dto.UserRef;
import com.recruiting.platform.contracts.ConsentStatus;
import com.recruiting.platform.contracts.PersonType;
import com.recruiting.platform.contracts.RetentionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Person as in the Person schema. */
public record PersonResponse(UUID id, String name, String title, String company, PersonType type, String city,
        String email, UserRef owner, Instant lastContactAt, ConsentStatus consentStatus,
        RetentionStatus retentionStatus, String phone, String linkedinUrl, String howWeKnow, String notes,
        List<Employment> employments, boolean anonymised, Instant createdAt, Instant updatedAt, long version) {
}
