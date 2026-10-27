package com.recruiting.platform.privacy.retention.app;

import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ConsentRequestPurpose;
import com.recruiting.platform.contracts.ConsentStatus;
import com.recruiting.platform.contracts.Language;
import com.recruiting.platform.contracts.RetentionStatus;
import com.recruiting.platform.privacy.consent.app.ConsentRequestService;
import com.recruiting.platform.privacy.domain.ConsentRequestRepository;
import com.recruiting.platform.privacy.domain.PersonPrivacy;
import com.recruiting.platform.privacy.domain.PersonPrivacyRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Renewal reminders (S-03, ADR 0011): before {@code retain_until}, at each configured step (60 and 14 days by
 * default) a RENEWAL request goes to people with consent GIVEN and an INITIAL resend to people still SENT. One
 * reminder per person, step and retention period: {@code reminder_for_retain_until} is unique, so the nightly run is
 * idempotent. People who refused or withdrew are never reminded. Opening the link is never a consent signal.
 */
@Service
public class RenewalReminderService {

    private static final Logger LOG = LoggerFactory.getLogger(RenewalReminderService.class);

    private final PersonPrivacyRepository privacy;
    private final ConsentRequestRepository requests;
    private final ConsentRequestService sender;
    private final RetentionPolicy policy;
    private final TransactionTemplate tx;

    RenewalReminderService(PersonPrivacyRepository privacy, ConsentRequestRepository requests,
            ConsentRequestService sender, RetentionPolicy policy, PlatformTransactionManager txManager) {
        this.privacy = privacy;
        this.requests = requests;
        this.sender = sender;
        this.policy = policy;
        this.tx = new TransactionTemplate(txManager);
    }

    /** Sends the due reminders; returns how many were queued. */
    public int run() {
        LocalDate today = policy.today();
        List<Short> steps = steps();
        if (steps.isEmpty()) {
            return 0;
        }
        int sent = 0;
        List<PersonPrivacy> candidates = privacy.findByConsentStatusInAndRetentionStatusNot(
                List.of(ConsentStatus.GIVEN, ConsentStatus.SENT), RetentionStatus.ANONYMISED);
        for (PersonPrivacy p : candidates) {
            LocalDate until = p.getRetainUntil();
            if (until == null || until.isBefore(today)) {
                continue; // an ended period is erased by the run, not reminded
            }
            Short step = dueStep(until, today, steps);
            if (step == null || requests.existsByPersonIdAndReminderForRetainUntilAndReminderDaysBefore(
                    p.getPersonId(), until, step)) {
                continue;
            }
            if (remind(p.getPersonId(), p.getConsentStatus(), step, until)) {
                sent++;
            }
        }
        return sent;
    }

    /** Date of the next reminder that is not sent yet, for the consent overview; null when there is none. */
    public LocalDate nextReminderOn(PersonPrivacy p) {
        if (p.getRetainUntil() == null || p.getRetentionStatus() == RetentionStatus.ANONYMISED
                || (p.getConsentStatus() != ConsentStatus.GIVEN && p.getConsentStatus() != ConsentStatus.SENT)) {
            return null;
        }
        LocalDate today = policy.today();
        LocalDate next = null;
        for (Short step : steps()) {
            LocalDate on = p.getRetainUntil().minusDays(step);
            boolean sent = requests.existsByPersonIdAndReminderForRetainUntilAndReminderDaysBefore(p.getPersonId(),
                    p.getRetainUntil(), step);
            if (!sent && !on.isBefore(today) && (next == null || on.isBefore(next))) {
                next = on;
            }
        }
        return next;
    }

    private boolean remind(UUID personId, ConsentStatus status, short step, LocalDate until) {
        ConsentRequestPurpose purpose = status == ConsentStatus.GIVEN ? ConsentRequestPurpose.RENEWAL
                : ConsentRequestPurpose.INITIAL;
        try {
            Boolean ok = tx.execute(s -> {
                Language language = requests.findFirstByPersonIdOrderByQueuedAtDesc(personId)
                        .map(r -> r.getLanguage()).orElse(Language.TR);
                sender.sendReminder(personId, language, purpose, step, until);
                return true;
            });
            return Boolean.TRUE.equals(ok);
        } catch (ApiException | DataIntegrityViolationException ex) {
            LOG.info("Reminder skipped for person {}: {}", personId, ex.getClass().getSimpleName());
            return false;
        }
    }

    /** The smallest configured step whose date has been reached (the latest one that applies). */
    private static Short dueStep(LocalDate until, LocalDate today, List<Short> steps) {
        for (Short step : steps) { // ascending
            if (!today.isBefore(until.minusDays(step))) {
                return step;
            }
        }
        return null;
    }

    private List<Short> steps() {
        return policy.settings().getRenewalReminderDays().stream().filter(d -> d != null && d > 0).distinct().sorted()
                .toList();
    }
}
