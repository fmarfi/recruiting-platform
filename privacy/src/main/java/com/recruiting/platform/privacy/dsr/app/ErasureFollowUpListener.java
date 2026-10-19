package com.recruiting.platform.privacy.dsr.app;

import com.recruiting.platform.contracts.PersonAnonymised;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * ADR 0009 step 3 and 4 for privacy: keep the consent records as legal evidence but drop IP address and evidence
 * text, mark the privacy record ANONYMISED, then purge the Envers history in its own transaction.
 */
@Component
class ErasureFollowUpListener {

    private final JdbcTemplate jdbc;
    private final AuditPurger purger;

    ErasureFollowUpListener(JdbcTemplate jdbc, AuditPurger purger) {
        this.jdbc = jdbc;
        this.purger = purger;
    }

    @ApplicationModuleListener
    void on(PersonAnonymised event) {
        UUID p = event.personId();
        if (!event.deleted()) {
            jdbc.update("update consent_record set ip_address = null, evidence = null where person_id = ?", p);
            jdbc.update("update person_privacy set retention_status = 'ANONYMISED', flagged_at = null, "
                    + "version = version + 1 where person_id = ?", p);
        }
        purger.purge(p, event.deleted());
    }
}
