package com.recruiting.platform.privacy.consent.app;

import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ConsentChannel;
import com.recruiting.platform.contracts.ConsentDecision;
import com.recruiting.platform.contracts.ConsentStatus;
import com.recruiting.platform.contracts.ConsentStatusChanged;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.contracts.IllegalTransitionException;
import com.recruiting.platform.contracts.RetentionStatus;
import com.recruiting.platform.privacy.domain.ConsentRecord;
import com.recruiting.platform.privacy.domain.ConsentRecordRepository;
import com.recruiting.platform.privacy.domain.ConsentRequest;
import com.recruiting.platform.privacy.domain.ConsentRequestRepository;
import com.recruiting.platform.privacy.domain.PersonPrivacy;
import com.recruiting.platform.privacy.domain.PrivacyNoticeRepository;
import com.recruiting.platform.privacy.person.app.PersonGateway;
import com.recruiting.platform.privacy.person.app.PersonInfo;
import com.recruiting.platform.privacy.retention.app.RetentionPolicy;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records consent decisions (append-only, NFR-02) and moves the person's consent status along the lifecycle in
 * docs/status-lifecycles.md section 5. One place for both channels: staff recording a decision and the person
 * using the e-mail link.
 */
@Service
public class ConsentService {

    /** One decision to record. {@code allowRepeat} lets the same decision be stored again without a status change. */
    public record Decision(UUID personId, ConsentDecision decision, ConsentChannel channel, UUID noticeId,
            UUID consentRequestId, Instant recordedAt, UUID recordedBy, String evidence, String ipAddress,
            boolean allowRepeat) {
    }

    private final ConsentRecordRepository records;
    private final ConsentRequestRepository requests;
    private final PrivacyNoticeRepository notices;
    private final PersonGateway people;
    private final RetentionPolicy retention;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    ConsentService(ConsentRecordRepository records, ConsentRequestRepository requests,
            PrivacyNoticeRepository notices, PersonGateway people, RetentionPolicy retention,
            ApplicationEventPublisher events, Clock clock) {
        this.records = records;
        this.requests = requests;
        this.notices = notices;
        this.people = people;
        this.retention = retention;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public ConsentRecord record(Decision d) {
        PersonInfo person = people.find(d.personId()).orElseThrow(() -> notFound(d.personId()));
        if (person.anonymised()) {
            throw new ApiException(ErrorCode.STATE_CONFLICT, "This person has been anonymised.");
        }
        if (!notices.existsById(d.noticeId())) {
            throw new ApiException(ErrorCode.NOT_FOUND, "Notice " + d.noticeId() + " does not exist.");
        }
        Instant now = clock.instant();
        Instant recordedAt = d.recordedAt() == null ? now : d.recordedAt();
        if (recordedAt.isAfter(now.plusSeconds(60))) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "The decision date cannot be in the future.",
                    Map.of("errors", List.of(Map.of("field", "recordedAt", "code", "FUTURE",
                            "message", "recordedAt may not be in the future."))));
        }
        PersonPrivacy privacy = retention.ensure(d.personId());
        ConsentStatus from = privacy.getConsentStatus();
        ConsentStatus to = ConsentStatus.valueOf(d.decision().name());
        checkMove(from, to, d.allowRepeat());

        ConsentRecord record = new ConsentRecord();
        record.setPersonId(d.personId());
        record.setDecision(d.decision());
        record.setChannel(d.channel());
        record.setNoticeId(d.noticeId());
        record.setConsentRequestId(d.consentRequestId());
        record.setRecordedAt(recordedAt);
        record.setRecordedBy(d.recordedBy());
        record.setEvidence(d.evidence());
        record.setIpAddress(d.ipAddress());
        ConsentRecord saved = records.save(record);

        if (d.consentRequestId() != null) {
            requests.findById(d.consentRequestId()).ifPresent(r -> markAnswered(r, d.decision(), now));
        }
        privacy.setConsentStatus(to);
        if (to == ConsentStatus.GIVEN && retention.settings().isRenewOnConsent()) {
            privacy.setBasisDate(retention.today());
            privacy.setRetainUntil(retention.retainUntil(privacy.getBasisDate()));
            privacy.setRetentionStatus(RetentionStatus.ACTIVE);
            privacy.setFlaggedAt(null);
        }
        if (from != to) {
            events.publishEvent(new ConsentStatusChanged(UUID.randomUUID(), now, d.recordedBy(), d.personId(),
                    from, to, d.channel(), null));
        }
        return saved;
    }

    private static void checkMove(ConsentStatus from, ConsentStatus to, boolean allowRepeat) {
        if (from == to) {
            if (to == ConsentStatus.GIVEN || allowRepeat) {
                return; // a renewal or an idempotent repeat: new record, same status
            }
            throw new IllegalTransitionException(from, to);
        }
        IllegalTransitionException.check(from, to, null);
    }

    private static void markAnswered(ConsentRequest request, ConsentDecision decision, Instant now) {
        request.setRespondedAt(now);
        request.setResponse(decision);
    }

    private static ApiException notFound(UUID personId) {
        return new ApiException(ErrorCode.NOT_FOUND, "Person " + personId + " does not exist.");
    }
}
