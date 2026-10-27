package com.recruiting.platform.privacy.consent.app;

import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.BulkActionType;
import com.recruiting.platform.contracts.BulkResultCode;
import com.recruiting.platform.contracts.ConsentStatus;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.contracts.Language;
import com.recruiting.platform.contracts.Role;
import com.recruiting.platform.pipeline.BulkActionHandler;
import com.recruiting.platform.privacy.domain.PersonPrivacyRepository;
import com.recruiting.platform.privacy.person.app.PersonGateway;
import com.recruiting.platform.privacy.person.app.PersonInfo;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Bulk REQUEST_CONSENT (ADR 0010 consent gate): MANAGER/ADMIN only; skips unknown people, anonymised people, people
 * without e-mail, GIVEN, a request younger than 7 days, and never re-asks REFUSED or WITHDRAWN people. Each record
 * is its own transaction (the single-send service is transactional), so one failure never affects the others.
 */
@Component
class RequestConsentBulkHandler implements BulkActionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RequestConsentBulkHandler.class);

    private final PersonGateway people;
    private final PersonPrivacyRepository privacy;
    private final ConsentRequestService requests;
    private final Clock clock;

    RequestConsentBulkHandler(PersonGateway people, PersonPrivacyRepository privacy, ConsentRequestService requests,
            Clock clock) {
        this.people = people;
        this.privacy = privacy;
        this.requests = requests;
        this.clock = clock;
    }

    @Override
    public BulkActionType action() {
        return BulkActionType.REQUEST_CONSENT;
    }

    @Override
    public Result handle(UUID actorId, Role role, Map<String, Object> params, UUID id) {
        if (role != Role.MANAGER && role != Role.ADMIN) {
            return Result.skipped(BulkResultCode.FORBIDDEN);
        }
        PersonInfo person = people.find(id).orElse(null);
        if (person == null) {
            return Result.skipped(BulkResultCode.NOT_FOUND);
        }
        if (person.anonymised()) {
            return Result.skipped(BulkResultCode.ANONYMISED);
        }
        ConsentStatus status = privacy.findById(id).map(p -> p.getConsentStatus()).orElse(ConsentStatus.NOT_SENT);
        switch (status) {
            case GIVEN:
                return Result.skipped(BulkResultCode.CONSENT_GIVEN);
            case REFUSED:
                return Result.skipped(BulkResultCode.CONSENT_REFUSED);
            case WITHDRAWN:
                return Result.skipped(BulkResultCode.CONSENT_WITHDRAWN);
            default:
                break;
        }
        if (person.email() == null || person.email().isBlank()) {
            return Result.skipped(BulkResultCode.NO_EMAIL);
        }
        if (requests.askedSince(id, clock.instant().minus(ConsentBulkService.REMINDER_GAP))) {
            return Result.skipped(BulkResultCode.REQUEST_PENDING);
        }
        try {
            requests.send(id, language(params), actorId);
            return Result.ok();
        } catch (ApiException ex) {
            return Result.skipped(ex.code() == ErrorCode.NOT_FOUND ? BulkResultCode.NOT_FOUND
                    : BulkResultCode.STATE_CONFLICT);
        } catch (RuntimeException ex) {
            LOG.warn("Bulk consent request failed for a person: {}", ex.getClass().getSimpleName());
            return Result.failed(BulkResultCode.INTERNAL_ERROR);
        }
    }

    private static Language language(Map<String, Object> params) {
        Object value = params == null ? null : params.get("language");
        if (value == null) {
            return Language.TR;
        }
        try {
            return Language.valueOf(value.toString());
        } catch (IllegalArgumentException ex) {
            return Language.TR;
        }
    }
}
