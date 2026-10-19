package com.recruiting.platform.privacy.publicconsent.web.dto;

import com.recruiting.platform.contracts.ConsentDecision;
import com.recruiting.platform.contracts.ConsentStatus;
import com.recruiting.platform.contracts.Language;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/** Shapes of the public consent page: the view (first name only, no other personal data) and the decision. */
public final class PublicConsentDtos {

    private PublicConsentDtos() {
    }

    public record PublicConsentView(Language language, String noticeVersion, String noticeText, String consentText,
            String firstName, String controllerName, ConsentStatus status, Instant expiresAt) {
    }

    public record PublicDecision(@NotNull ConsentDecision decision) {
    }
}
