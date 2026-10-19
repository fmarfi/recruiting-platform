package com.recruiting.platform.privacy.consent.app;

import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ConsentStatus;
import com.recruiting.platform.contracts.ConsentStatusChanged;
import com.recruiting.platform.contracts.EmailDeliveryStatus;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.contracts.Language;
import com.recruiting.platform.privacy.consent.app.ConsentMailComposer.Mail;
import com.recruiting.platform.privacy.domain.ConsentRequest;
import com.recruiting.platform.privacy.domain.ConsentRequestRepository;
import com.recruiting.platform.privacy.domain.PersonPrivacy;
import com.recruiting.platform.privacy.domain.PrivacyNotice;
import com.recruiting.platform.privacy.notice.app.PrivacyNoticeService;
import com.recruiting.platform.privacy.person.app.PersonGateway;
import com.recruiting.platform.privacy.person.app.PersonInfo;
import com.recruiting.platform.privacy.retention.app.RetentionPolicy;
import com.recruiting.platform.privacy.shared.ConsentTokens;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
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
    private final PersonGateway people;
    private final RetentionPolicy retention;
    private final PrivacyNoticeService notices;
    private final ConsentMailComposer composer;
    private final ConsentMailDispatcher dispatcher;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    ConsentRequestService(ConsentRequestRepository requests, PersonGateway people, RetentionPolicy retention,
            PrivacyNoticeService notices, ConsentMailComposer composer, ConsentMailDispatcher dispatcher,
            ApplicationEventPublisher events, Clock clock) {
        this.requests = requests;
        this.people = people;
        this.retention = retention;
        this.notices = notices;
        this.composer = composer;
        this.dispatcher = dispatcher;
        this.events = events;
        this.clock = clock;
    }

    /** Queues one request; {@code requestedBy} is null for the automatic e-mail after a person is created. */
    @Transactional
    public ConsentRequest send(UUID personId, Language requested, UUID requestedBy) {
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
        if (privacy.getConsentStatus() == ConsentStatus.GIVEN) {
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
        ConsentRequest saved = requests.saveAndFlush(request);

        if (privacy.getNoticeSentAt() == null) {
            privacy.setNoticeSentAt(now);
        }
        ConsentStatus from = privacy.getConsentStatus();
        if (from != ConsentStatus.SENT) {
            privacy.setConsentStatus(ConsentStatus.SENT);
            events.publishEvent(new ConsentStatusChanged(UUID.randomUUID(), now, requestedBy, personId, from,
                    ConsentStatus.SENT, null, null));
        }
        Mail mail = composer.compose(language, person.firstName(), token, notice,
                (int) VALID_FOR.toDays());
        dispatcher.sendAfterCommit(new ConsentMailDispatcher.Job(saved.getId(), person.email(), mail.subject(),
                mail.body()));
        return saved;
    }
}
