package com.recruiting.platform.accounts.duplicate.web.dto;

import com.recruiting.platform.accounts.person.web.dto.PersonSummaryResponse;
import com.recruiting.platform.contracts.DuplicateMatchReason;
import com.recruiting.platform.contracts.DuplicateStatus;
import com.recruiting.platform.security.UserRef;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** The DuplicateCandidate schema. */
public record DuplicateCandidateResponse(UUID id, PersonSummaryResponse personA, PersonSummaryResponse personB,
        List<DuplicateMatchReason> reasons, DuplicateStatus status, Instant detectedAt, Instant resolvedAt,
        UserRef resolvedBy) {
}
