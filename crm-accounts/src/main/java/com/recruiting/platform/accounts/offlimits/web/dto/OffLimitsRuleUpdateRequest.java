package com.recruiting.platform.accounts.offlimits.web.dto;

import com.recruiting.platform.contracts.OffLimitsReason;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** PATCH body: absent = unchanged; endsOn null reopens the rule. */
public record OffLimitsRuleUpdateRequest(OffLimitsReason reason, @Size(max = 500) String note, LocalDate startsOn,
        LocalDate endsOn, Long version) {
}
