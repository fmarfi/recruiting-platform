package com.recruiting.platform.accounts.offlimits.web.dto;

import com.recruiting.platform.accounts.shared.dto.AccountRef;
import com.recruiting.platform.contracts.OffLimitsReason;
import com.recruiting.platform.security.UserRef;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** The OffLimitsRule schema (1.2.0). */
public record OffLimitsRuleResponse(UUID id, AccountRef account, String companyName, OffLimitsReason reason,
        String note, LocalDate startsOn, LocalDate endsOn, boolean active, Instant createdAt, UserRef createdBy,
        long version) {
}
