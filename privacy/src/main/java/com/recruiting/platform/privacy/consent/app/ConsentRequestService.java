package com.recruiting.platform.privacy.consent.app;

import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ConsentStatus;
import com.recruiting.platform.contracts.ConsentRequestPurpose;
import com.recruiting.platform.contracts.ConsentStatusChanged;
import com.recruiting.platform.contracts.EmailDeliveryStatus;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.contracts.Language;
import com.recruiting.platform.privacy.consent.app.ConsentMailComposer.Mail;
import com.recruiting.platform.privacy.domain.ConsentRequest;
import com.recruiting.platform.privacy.domain.ConsentRequestRepository;
import com.recruiting.platform.privacy.domain.PersonPrivacy;
import com.recruiting.platform.privacy.domain.PersonPrivacyRepository;
import com.recruiting.platform.privacy.domain.PrivacyNotice;
import com.recruiting.platform.privacy.notice.app.PrivacyNoticeService;
import com.recruiting.platform.privacy.person.app.PersonGateway;
import com.recruiting.platform.privacy.person.app.PersonInfo;
import com.recruiting.platform.privacy.retention.app.RetentionPolicy;
import com.recruiting.platform.privacy.shared.ConsentTokens;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Queues the KVKK information notice and consent request e-mail (S-01). The status moves to SENT from NOT_SENT,
 * REFUSED or WITHDRAWN; a resend while SENT keeps the status. Not allowed once consent is GIVEN.
 */
@Service
public class ConsentRequestService {

    static final Duration VALID_FOR = Duration.ofDays(30);

    private final ConsentRequestRepository requests;
    private final PersonPrivacyRepository locked;
    private final PersonGateway people;
    private final RetentionPolicy retention;
    private final PrivacyNoticeService notices;
    private final ConsentMailComposer composer;
    private final ConsentMailDispatcher dispatcher;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    ConsentRequestService(ConsentRequestRepository requests, PersonPrivacyRepository locked, PersonGateway people, RetentionPolicy retention,
            PrivacyNoticeService notices, ConsentMailComposer composer, ConsentMailDispatcher dispatcher,
            ApplicationEventPublisher events, Clock clock) {
        this.requests = requests;
        this.locked = locked;
        this.people = people;
        this.retention = retention;
        this.notices = notices;
        this.composer = composer;
        this.dispatcher = dispatcher;
        this.events = events;
        this.clock = clock;
    }

    /** Queues one INITIAL request; {@code requestedBy} is null for the automatic e-mail after a person is created. */
    @Transactional
    public ConsentRequest send(UUID personId, Language requested, UUID requestedBy) {
        return send(personId, requested, requestedBy, ConsentRequestPurpose.INITIAL);
    }

    /** Queues one request with the given purpose (null means INITIAL). RENEWAL only while consent is GIVEN. */
    @Transactional
    public ConsentRequest send(UUID personId, Language requested, UUID requestedBy, ConsentRequestPurpose purpose) {
        return create(personId, requested, requestedBy, purpose == null ? ConsentRequestPurpose.INITIAL : purpose,
                null, null);
    }

    /**
     * Automatic notice and consent e-mail after a CV upload (S-01, ADR 0011): only for NOT_SENT or SENT people, never
     * to REFUSED or WITHDRAWN, never while an unanswered request is still valid. Returns the request or empty.
     */
    @Transactional
    public Optional<ConsentRequest> sendAutomaticInitial(UUID personId, Language language) {
        PersonPrivacy privacy = retention.ensure(personId);
        privacy = locked.lockById(privacy.getPersonId()).orElse(privacy);
        ConsentStatus status = privacy.getConsentStatus();
        if (status != ConsentStatus.NOT_SENT && status != ConsentStatus.SENT) {
            return Optional.empty();
        }
        if (pending(personId)) {
            return Optional.empty();
        }
        return Optional.of(create(personId, language, null, ConsentRequestPurpose.INITIAL, null, null));
    }

    /** An unanswered request whose link is still valid (a failed delivery does not count). */
    public boolean pending(UUID personId) {
        return requests.existsByPersonIdAndRespondedAtIsNullAndExpiresAtAfterAndDeliveryStatusNot(personId,
                clock.instant(), EmailDeliveryStatus.FAILED);
    }

    /** One automatic reminder (S-03): RENEWAL while GIVEN, INITIAL while SENT. Unique per person and step. */
    @Transactional
    public ConsentRequest sendReminder(UUID personId, Language language, ConsentRequestPurpose purpose,
            short daysBefore, LocalDate retainUntil) {
        return create(personId, language, null, purpose, daysBefore, retainUntil);
    }

    private ConsentRequest create(UUID personId, Language requested, UUID requestedBy, ConsentRequestPurpose purpose,
            Short reminderDays, LocalDate reminderFor) {
        Language language = requested == null ? Language.TR : requested;
        PersonInfo person = people.find(personId).orElseThrow(
                () -> new ApiException(ErrorCode.NOT_FOUND, "Person " + personId + " does not exist."));
        if (person.anonymised()) {
            throw new ApiException(ErrorCode.STATE_CONFLICT, "This person has been anonymised.");
        }
        if (person.email() == null || person.email().isBlank()) {
            throw new ApiException(ErrorCode.STATE_CONFLICT, "The person has no e-mail address.");
        }
        PersonPrivacy privacy = retention.ensure(personId);
        if (purpose == ConsentRequestPurpose.RENEWAL) {
            if (privacy.getConsentStatus() != ConsentStatus.GIVEN) {
                throw new ApiException(ErrorCode.STATE_CONFLICT,
                        "A renewal can only be requested while consent is given.");
            }
        } else if (privacy.getConsentStatus() == ConsentStatus.GIVEN) {
            throw new ApiException(ErrorCode.STATE_CONFLICT, "Consent has already been given.");
        }
        PrivacyNotice notice = notices.currentFor(language).orElseThrow(() -> new ApiException(
                ErrorCode.STATE_CONFLICT, "No privacy notice is published for " + language + "."));

        Instant now = clock.instant();
        String token = ConsentTokens.newToken();
        ConsentRequest request = new ConsentRequest();
        request.setPersonId(personId);
        request.setNoticeId(notice.getId());
        request.setLanguage(language);
        request.setTokenHash(ConsentTokens.hash(token));
        request.setDeliveryStatus(EmailDeliveryStatus.QUEUED);
        request.setQueuedAt(now);
        request.setExpiresAt(now.plus(VALID_FOR));
        request.setRequestedBy(requestedBy);
        request.setPurpose(purpose);
        request.setReminderDaysBefore(reminderDays);
        request.setReminderForRetainUntil(reminderFor);
        ConsentRequest saved = requests.saveAndFlush(request);

        if (purpose == ConsentRequestPurpose.INITIAL) {
            if (privacy.getNoticeSentAt() == null) {
                privacy.setNoticeSentAt(now);
            }
            ConsentStatus from = privacy.getConsentStatus();
            if (from != ConsentStatus.SENT) {
                privacy.setConsentStatus(ConsentStatus.SENT);
                events.publishEvent(new ConsentStatusChanged(UUID.randomUUID(), now, requestedBy, personId, from,
                        ConsentStatus.SENT, null, null));
            }
        }
        Mail mail = composer.compose(language, person.firstName(), token, notice, (int) VALID_FOR.toDays(), purpose);
        dispatcher.sendAfterCommit(new ConsentMailDispatcher.Job(saved.getId(), person.email(), mail.subject(),
                mail.body()));
        return saved;
    }
}
