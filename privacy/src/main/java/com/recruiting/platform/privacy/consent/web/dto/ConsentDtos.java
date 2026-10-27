package com.recruiting.platform.privacy.consent.web.dto;

import com.recruiting.platform.contracts.ConsentChannel;
import com.recruiting.platform.contracts.ConsentDecision;
import com.recruiting.platform.contracts.ConsentRequestPurpose;
import com.recruiting.platform.contracts.ConsentStatus;
import com.recruiting.platform.contracts.EmailDeliveryStatus;
import com.recruiting.platform.contracts.Language;
import com.recruiting.platform.contracts.RetentionStatus;
import com.recruiting.platform.contracts.ShareBlockReason;
import com.recruiting.platform.security.UserRef;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Response shapes of the Consent tag (contract schemas PrivacyNoticeRef, ConsentRecord, ConsentRequest, ...). */
public final class ConsentDtos {

    private ConsentDtos() {
    }

    public record NoticeRef(UUID id, String version, Language language) {
    }

    public record ConsentRecordResponse(UUID id, UUID personId, ConsentDecision decision, ConsentChannel channel,
            NoticeRef notice, Instant recordedAt, UserRef recordedBy, String evidence) {
    }

    public record ConsentRequestResponse(UUID id, UUID personId, NoticeRef notice, Language language,
            EmailDeliveryStatus deliveryStatus, Instant queuedAt, Instant sentAt, String failureReason,
            Instant expiresAt, Instant respondedAt, ConsentDecision response, UserRef requestedBy,
            Instant openedAt, ConsentRequestPurpose purpose, Integer reminderDaysBefore) {
    }

    public record RetentionInfo(RetentionStatus status, LocalDate basisDate, LocalDate retainUntil,
            Instant flaggedAt) {
    }

    public record ConsentOverviewResponse(UUID personId, ConsentStatus status, Instant noticeSentAt,
            ConsentRequestResponse lastRequest, RetentionInfo retention, boolean shareable,
            ShareBlockReason shareBlockReason, Instant lastConsentAt, LocalDate nextReminderOn,
            List<ConsentRecordResponse> records) {
    }

    public record ShareCheckResult(UUID personId, boolean shareable, ShareBlockReason reason) {
    }

    public record BulkResult(int queued, int skipped) {
    }
}
