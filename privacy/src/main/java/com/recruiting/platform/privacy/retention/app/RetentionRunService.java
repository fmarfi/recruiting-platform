package com.recruiting.platform.privacy.retention.app;

import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.contracts.RetentionAction;
import com.recruiting.platform.contracts.RetentionBasis;
import com.recruiting.platform.contracts.RetentionFlagged;
import com.recruiting.platform.contracts.RetentionStatus;
import com.recruiting.platform.privacy.domain.PersonPrivacy;
import com.recruiting.platform.privacy.domain.PersonPrivacyRepository;
import com.recruiting.platform.privacy.domain.RetentionRun;
import com.recruiting.platform.privacy.domain.RetentionRunRepository;
import com.recruiting.platform.privacy.domain.RetentionSettings;
import com.recruiting.platform.privacy.dsr.app.PersonErasureService;
import com.recruiting.platform.privacy.person.app.PersonGateway;
import com.recruiting.platform.privacy.person.app.PersonInfo;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The retention job (S-03, NFR-03). One run: create missing privacy records, refresh the retention dates from the
 * settings, anonymise or delete everyone whose period has ended, and flag everyone whose period ends within the
 * warning window (event RetentionFlagged). A dry run only counts. Every run is logged in retention_run. Time comes
 * from the injected Clock so tests can shift it.
 */
@Service
public class RetentionRunService {

    private static final Logger LOG = LoggerFactory.getLogger(RetentionRunService.class);
    private static final int BATCH = 500;

    private final ReentrantLock running = new ReentrantLock();
    private final PersonPrivacyRepository privacy;
    private final RetentionRunRepository runs;
    private final PersonGateway people;
    private final RetentionPolicy policy;
    private final PersonErasureService erasure;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final TransactionTemplate tx;

    RetentionRunService(PersonPrivacyRepository privacy, RetentionRunRepository runs, PersonGateway people,
            RetentionPolicy policy, PersonErasureService erasure, ApplicationEventPublisher events, Clock clock,
            PlatformTransactionManager txManager) {
        this.privacy = privacy;
        this.runs = runs;
        this.people = people;
        this.policy = policy;
        this.erasure = erasure;
        this.events = events;
        this.clock = clock;
        this.tx = new TransactionTemplate(txManager);
    }

    /** Runs the job; {@code actorId} is null for the nightly scheduler. */
    public RetentionRun run(boolean dryRun, UUID actorId) {
        if (!running.tryLock()) {
            throw new ApiException(ErrorCode.STATE_CONFLICT, "A retention run is already in progress.");
        }
        try {
            RetentionRun run = new RetentionRun();
            run.setStartedAt(clock.instant());
            run.setTriggeredBy(actorId);
            run.setDryRun(dryRun);
            execute(run, dryRun, actorId);
            run.setFinishedAt(clock.instant());
            return runs.save(run);
        } finally {
            running.unlock();
        }
    }

    private void execute(RetentionRun run, boolean dryRun, UUID actorId) {
        RetentionSettings settings = policy.settings();
        LocalDate today = policy.today();
        try {
            if (!dryRun) {
                backfill();
                refreshDates(settings);
            }
            List<UUID> due = new ArrayList<>();
            List<UUID> toFlag = new ArrayList<>();
            for (PersonPrivacy p : privacy.findByRetentionStatusNot(RetentionStatus.ANONYMISED)) {
                LocalDate until = dryRun ? projectedUntil(p, settings) : p.getRetainUntil();
                if (until.isBefore(today)) {
                    due.add(p.getPersonId());
                } else if (p.getRetentionStatus() == RetentionStatus.ACTIVE
                        && !until.minusDays(settings.getWarningDays()).isAfter(today)) {
                    toFlag.add(p.getPersonId());
                }
            }
            run.setFlaggedCount(dryRun ? toFlag.size() : flag(toFlag));
            eraseAll(run, due, settings.getAction(), dryRun, actorId);
        } catch (RuntimeException ex) {
            LOG.error("Retention run failed", ex);
            run.setFailureReason(ex.getClass().getSimpleName());
        }
    }

    /** Dry runs must not write, so they project the dates the refresh would set. */
    private LocalDate projectedUntil(PersonPrivacy p, RetentionSettings settings) {
        return p.getBasisDate().plusMonths(settings.getRetentionMonths());
    }

    private void backfill() {
        List<UUID> ids = people.idsWithoutPrivacy(BATCH);
        while (!ids.isEmpty()) {
            Map<UUID, PersonInfo> infos = people.findAll(ids);
            ids.forEach(id -> tx.executeWithoutResult(s -> policy.create(id, infos.get(id))));
            ids = people.idsWithoutPrivacy(BATCH);
        }
    }

    private void refreshDates(RetentionSettings settings) {
        List<PersonPrivacy> all = privacy.findByRetentionStatusNot(RetentionStatus.ANONYMISED);
        for (int from = 0; from < all.size(); from += BATCH) {
            List<PersonPrivacy> chunk = all.subList(from, Math.min(from + BATCH, all.size()));
            Map<UUID, PersonInfo> infos = people.findAll(chunk.stream().map(PersonPrivacy::getPersonId).toList());
            tx.executeWithoutResult(s -> chunk.forEach(p -> refresh(p.getPersonId(), infos.get(p.getPersonId()),
                    settings)));
        }
    }

    private void refresh(UUID personId, PersonInfo info, RetentionSettings settings) {
        privacy.findById(personId).ifPresent(p -> {
            if (settings.getBasis() == RetentionBasis.FROM_LAST_CONTACT && info != null) {
                LocalDate candidate = policy.basisFor(info);
                if (candidate.isAfter(p.getBasisDate())) {
                    p.setBasisDate(candidate);
                }
            }
            LocalDate until = p.getBasisDate().plusMonths(settings.getRetentionMonths());
            if (!until.equals(p.getRetainUntil())) {
                p.setRetainUntil(until);
            }
        });
    }

    private int flag(List<UUID> ids) {
        Map<UUID, PersonInfo> infos = people.findAll(ids);
        int count = 0;
        for (UUID id : ids) {
            PersonInfo info = infos.get(id);
            Boolean done = tx.execute(s -> privacy.findById(id).map(p -> {
                p.setRetentionStatus(RetentionStatus.FLAGGED);
                p.setFlaggedAt(clock.instant());
                events.publishEvent(new RetentionFlagged(UUID.randomUUID(), clock.instant(), null, id,
                        info == null ? null : info.ownerId(), p.getRetainUntil()));
                return true;
            }).orElse(false));
            if (Boolean.TRUE.equals(done)) {
                count++;
            }
        }
        return count;
    }

    private void eraseAll(RetentionRun run, List<UUID> due, RetentionAction action, boolean dryRun, UUID actorId) {
        int done = 0;
        for (UUID id : due) {
            if (!dryRun) {
                try {
                    erasure.erase(id, action, actorId);
                } catch (RuntimeException ex) {
                    LOG.warn("Retention could not erase person {}: {}", id, ex.getClass().getSimpleName());
                    if (run.getFailureReason() == null) {
                        run.setFailureReason("Person " + id + ": " + ex.getClass().getSimpleName());
                    }
                    continue;
                }
            }
            done++;
        }
        if (action == RetentionAction.DELETE) {
            run.setDeletedCount(done);
        } else {
            run.setAnonymisedCount(done);
        }
    }
}
