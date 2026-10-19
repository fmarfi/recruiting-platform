package com.recruiting.platform.accounts.contact.web.dto;

import com.recruiting.platform.accounts.person.web.dto.PersonSummaryResponse;
import com.recruiting.platform.accounts.shared.dto.AccountRef;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Contact as in the Contact schema. */
public record ContactResponse(UUID id, AccountRef account, PersonSummaryResponse person, String role, boolean primary,
        LocalDate endedOn, boolean active, Instant createdAt) {
}
