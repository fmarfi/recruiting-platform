package com.recruiting.platform.privacy.consent.web.dto;

import com.recruiting.platform.contracts.ConsentChannel;
import com.recruiting.platform.contracts.ConsentDecision;
import com.recruiting.platform.contracts.ConsentStatus;
import com.recruiting.platform.contracts.Language;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Request bodies of the Consent tag. */
public final class ConsentRequests {

    private ConsentRequests() {
    }

    /** Body of POST /people/{id}/consent/records; channel is IN_PERSON, PHONE or IMPORT only. */
    public record RecordCreate(@NotNull ConsentDecision decision, @NotNull ConsentChannel channel,
            @NotNull UUID noticeId, Instant recordedAt, @Size(min = 1, max = 1000) String evidence) {
    }

    /** Body of POST /people/{id}/consent/requests (optional). */
    public record RequestCreate(Language language) {
    }

    /** Body of POST /consent-requests/bulk: personIds, or importId, or consentStatus. */
    public record BulkCreate(@Size(max = 1000) List<UUID> personIds, UUID importId,
            List<ConsentStatus> consentStatus, Language language) {
    }
}
