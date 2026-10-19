package com.recruiting.platform.privacy.retention.web.dto;

import com.recruiting.platform.contracts.ConsentStatus;
import com.recruiting.platform.contracts.PersonType;
import com.recruiting.platform.contracts.RetentionAction;
import com.recruiting.platform.contracts.RetentionBasis;
import com.recruiting.platform.contracts.RetentionStatus;
import com.recruiting.platform.privacy.consent.web.dto.ConsentDtos.RetentionInfo;
import com.recruiting.platform.security.UserRef;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Shapes of the Retention tag. */
public final class RetentionDtos {

    private RetentionDtos() {
    }

    /** Contract schema RetentionSettings (request and response). */
    public record Settings(@Min(1) @Max(120) int retentionMonths, @NotNull RetentionBasis basis,
            @NotNull RetentionAction action, @Min(0) @Max(365) int warningDays, boolean autoSendNotice,
            @NotEmpty List<PersonType> autoSendPersonTypes, boolean renewOnConsent, Long version) {
    }

    public record RunRequest(@NotNull Boolean dryRun) {
    }

    public record RunResponse(UUID id, Instant startedAt, Instant finishedAt, UserRef triggeredBy, boolean dryRun,
            int flaggedCount, int anonymisedCount, int deletedCount, String failureReason) {
    }

    /** The slice of PersonSummary the retention list needs. */
    public record PersonSummary(UUID id, String name, String title, String company, PersonType type, String city,
            String email, UserRef owner, Instant lastContactAt, ConsentStatus consentStatus,
            RetentionStatus retentionStatus) {
    }

    public record Item(PersonSummary person, RetentionInfo retention) {
    }
}
