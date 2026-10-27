package com.recruiting.platform.privacy.retention.app;

import com.recruiting.platform.contracts.RetentionRenewed;
import com.recruiting.platform.privacy.domain.PersonPrivacy;
import com.recruiting.platform.privacy.domain.PersonPrivacyRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * One-off job after the basis switch to FROM_LAST_CONSENT (schema.md "Basis switch migration"). The migration changes
 * {@code person_privacy} with SQL, which bypasses Envers, so the latest {@code person_privacy_aud} row is stale. This
 * job finds rows whose live values differ from that row and re-saves them through JPA so a fresh revision is written,
 * and publishes {@code RetentionRenewed} for rows the migration moved from FLAGGED back to ACTIVE (the person copy
 * and the "Renew consent" next action follow that event). Idempotent: once the audit row matches, nothing is found.
 * Runs at start-up and at the start of every retention run.
 *
 * <p>Envers only writes a revision for a changed field, so each stale row is saved twice: first with a one-day shifted
 * {@code retain_until}, then with the real value. The history keeps one extra revision; the purge removes it with the
 * rest of the person's history.
 */
@Component
public class RetentionBasisSync {

    private static final Logger LOG = LoggerFactory.getLogger(RetentionBasisSync.class);

    private final JdbcTemplate jdbc;
    private final PersonPrivacyRepository privacy;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final TransactionTemplate tx;

    RetentionBasisSync(JdbcTemplate jdbc, PersonPrivacyRepository privacy, ApplicationEventPublisher events,
            Clock clock, PlatformTransactionManager txManager) {
        this.jdbc = jdbc;
        this.privacy = privacy;
        this.events = events;
        this.clock = clock;
        this.tx = new TransactionTemplate(txManager);
    }

    @EventListener(ApplicationReadyEvent.class)
    void onReady() {
        try {
            sync();
        } catch (RuntimeException ex) {
            LOG.warn("Retention basis sync skipped: {}", ex.getClass().getSimpleName());
        }
    }

    /** Returns the number of rows re-saved. */
    public int sync() {
        List<Map<String, Object>> stale = jdbc.queryForList(
                "select pp.person_id, a.retention_status as aud_status from person_privacy pp "
                + "join person_privacy_aud a on a.person_id = pp.person_id and a.revend is null and a.revtype <> 2 "
                + "where pp.retention_status <> 'ANONYMISED' and ("
                + "a.basis_date is distinct from pp.basis_date or a.retain_until is distinct from pp.retain_until "
                + "or a.retention_status is distinct from pp.retention_status "
                + "or a.flagged_at is distinct from pp.flagged_at "
                + "or a.last_consent_at is distinct from pp.last_consent_at) limit 1000");
        int done = 0;
        for (Map<String, Object> row : stale) {
            UUID id = (UUID) row.get("person_id");
            boolean wasFlagged = "FLAGGED".equals(row.get("aud_status"));
            try {
                if (resave(id, wasFlagged)) {
                    done++;
                }
            } catch (RuntimeException ex) {
                LOG.warn("Retention basis sync failed for person {}: {}", id, ex.getClass().getSimpleName());
            }
        }
        if (done > 0) {
            LOG.info("Retention basis sync re-saved {} person_privacy rows", done);
        }
        return done;
    }

    private boolean resave(UUID id, boolean wasFlagged) {
        LocalDate until = tx.execute(s -> privacy.findById(id).map(p -> {
            LocalDate real = p.getRetainUntil();
            p.setRetainUntil(real.plusDays(1)); // makes the entity dirty so Envers writes a revision
            privacy.saveAndFlush(p);
            return real;
        }).orElse(null));
        if (until == null) {
            return false;
        }
        tx.executeWithoutResult(s -> {
            PersonPrivacy p = privacy.findById(id).orElseThrow();
            p.setRetainUntil(until);
            privacy.saveAndFlush(p);
            if (wasFlagged && p.getRetentionStatus() == com.recruiting.platform.contracts.RetentionStatus.ACTIVE) {
                events.publishEvent(new RetentionRenewed(UUID.randomUUID(), clock.instant(), null, id, until));
            }
        });
        return true;
    }
}
