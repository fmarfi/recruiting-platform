package com.recruiting.platform.accounts.person.web.dto;

import com.recruiting.platform.contracts.ConsentStatus;
import com.recruiting.platform.contracts.PersonType;
import com.recruiting.platform.contracts.RetentionStatus;
import com.recruiting.platform.contracts.SourceChannel;
import com.recruiting.platform.security.UserRef;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Person as in the PersonSummary schema (lists, refs). {@code cvMatch} is set only with the {@code cv} filter. */
public record PersonSummaryResponse(UUID id, String name, String title, String company, PersonType type, String city,
        String email, UserRef owner, Instant lastContactAt, ConsentStatus consentStatus,
        RetentionStatus retentionStatus, List<String> tags, SourceChannel sourceChannel, boolean shareable,
        String cvMatch) {

    public PersonSummaryResponse withCvMatch(String match) {
        return new PersonSummaryResponse(id, name, title, company, type, city, email, owner, lastContactAt,
                consentStatus, retentionStatus, tags, sourceChannel, shareable, match);
    }
}
