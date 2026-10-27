package com.recruiting.platform.accounts.offlimits.web.dto;

import com.recruiting.platform.contracts.OffLimitsReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/** Body of POST /off-limits: exactly one of accountId and companyName. */
public record OffLimitsRuleCreateRequest(UUID accountId, @Size(min = 1, max = 200) String companyName,
        @NotNull OffLimitsReason reason, @Size(max = 500) String note, LocalDate startsOn, LocalDate endsOn) {
}
