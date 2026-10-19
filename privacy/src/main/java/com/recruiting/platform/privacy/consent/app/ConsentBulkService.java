package com.recruiting.platform.privacy.consent.app;

import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.privacy.consent.web.dto.ConsentDtos.BulkResult;
import com.recruiting.platform.privacy.consent.web.dto.ConsentRequests.BulkCreate;
import com.recruiting.platform.privacy.domain.ConsentRequestRepository;
import com.recruiting.platform.privacy.domain.PersonPrivacyRepository;
import com.recruiting.platform.security.CurrentUser;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Consent campaign (S-01): queues requests for many people. Skips people without e-mail, with consent GIVEN, or
 * with an unanswered request younger than 7 days; each person is its own transaction, so one failure skips only
 * that person.
 */
@Service
public class ConsentBulkService {

    static final Duration REMINDER_GAP = Duration.ofDays(7);

    private final ConsentRequestService single;
    private final ConsentRequestRepository requests;
    private final PersonPrivacyRepository privacy;
    private final CurrentUser currentUser;
    private final Clock clock;

    ConsentBulkService(ConsentRequestService single, ConsentRequestRepository requests,
            PersonPrivacyRepository privacy, CurrentUser currentUser, Clock clock) {
        this.single = single;
        this.requests = requests;
        this.privacy = privacy;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    public BulkResult send(BulkCreate request) {
        Set<UUID> ids = new LinkedHashSet<>();
        if (request.personIds() != null) {
            ids.addAll(request.personIds());
        }
        if (request.importId() != null) {
            privacy.findByImportId(request.importId()).forEach(p -> ids.add(p.getPersonId()));
        }
        if (request.consentStatus() != null && !request.consentStatus().isEmpty()) {
            privacy.findByConsentStatusIn(request.consentStatus()).forEach(p -> ids.add(p.getPersonId()));
        }
        if (ids.isEmpty() && request.personIds() == null && request.importId() == null
                && (request.consentStatus() == null || request.consentStatus().isEmpty())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Give personIds, importId or consentStatus.",
                    Map.of("errors", List.of(Map.of("field", "personIds", "code", "REQUIRED",
                            "message", "Choose who should get the e-mail."))));
        }
        UUID actor = currentUser.idOrEmpty().orElse(null);
        int queued = 0;
        int skipped = 0;
        for (UUID id : ids) {
            if (recentlyAsked(id)) {
                skipped++;
                continue;
            }
            try {
                single.send(id, request.language(), actor);
                queued++;
            } catch (ApiException ex) {
                skipped++; // no e-mail, consent given, anonymised or unknown person
            }
        }
        return new BulkResult(queued, skipped);
    }

    private boolean recentlyAsked(UUID personId) {
        return requests.existsByPersonIdAndRespondedAtIsNullAndQueuedAtAfter(personId,
                clock.instant().minus(REMINDER_GAP));
    }
}
