package com.recruiting.platform.privacy.dsr.app;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADR 0009 step 4 (schema.md, "Anonymisation and deletion"): purges the Envers history so it no longer holds the
 * person's data. Idempotent, runs in its own transaction after the listeners. Any new audited table with person
 * data must be added here.
 */
@Component
public class AuditPurger {

    /** Audit tables matched by {@code person_id = :p}. */
    private static final List<String> BY_PERSON_ID = List.of("person_employment_aud", "contact_aud", "activity_aud",
            "application_aud", "next_action_aud", "person_privacy_aud");

    private final JdbcTemplate jdbc;

    AuditPurger(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void purge(UUID p, boolean deleted) {
        if (deleted) {
            purgeDeleted(p);
        } else {
            purgeAnonymised(p);
        }
        // A committed import file would still hold the person's data.
        jdbc.update("update import_job set file_content = null, errors = null "
                + "where status in ('COMPLETED', 'FAILED', 'DISCARDED') "
                + "and (file_content is not null or errors is not null)");
    }

    /** Old revisions go; the current revision stays (ValidityAuditStrategy needs it) with placeholders. */
    private void purgeAnonymised(UUID p) {
        jdbc.update("delete from person_aud where id = ? and revend is not null", p);
        BY_PERSON_ID.forEach(t -> jdbc.update("delete from " + t + " where person_id = ? and revend is not null", p));
        jdbc.update("delete from data_subject_request_aud where person_id = ? and revend is not null", p);
        jdbc.update("delete from network_link_aud where (from_person_id = ? or to_person_id = ?) "
                + "and revend is not null", p, p);

        jdbc.update("update person_aud set name = 'Anonymised person', title = null, company = null, email = null, "
                + "phone = null, city = null, linkedin_url = null, how_we_know = null, notes = null, "
                + "anonymised_at = coalesce(anonymised_at, now()) where id = ? and revend is null", p);
        jdbc.update("update person_employment_aud set company = '[removed]', title = null "
                + "where person_id = ? and revend is null", p);
        jdbc.update("update network_link_aud set note = null where (from_person_id = ? or to_person_id = ?) "
                + "and revend is null", p, p);
        jdbc.update("update activity_aud set note = '[removed]' where person_id = ? and revend is null", p);
        jdbc.update("update application_aud set rejection_reason = '[removed]' "
                + "where person_id = ? and revend is null and rejection_reason is not null", p);
        jdbc.update("update next_action_aud set title = '[removed]' where person_id = ? and revend is null", p);
        jdbc.update("update data_subject_request_aud a set requester_name = d.requester_name, "
                + "requester_email = d.requester_email, description = '[removed]' "
                + "from data_subject_request d where d.id = a.id and a.person_id = ? and a.revend is null", p);
    }

    /** Everything goes, except the id-only delete row of the person and the current (minimised) request row. */
    private void purgeDeleted(UUID p) {
        jdbc.update("delete from person_aud where id = ? and revtype <> 2", p);
        BY_PERSON_ID.forEach(t -> jdbc.update("delete from " + t + " where person_id = ?", p));
        jdbc.update("delete from network_link_aud where from_person_id = ? or to_person_id = ?", p, p);
        jdbc.update("delete from data_subject_request_aud where person_id = ? and revend is not null", p);
        // the database's SET NULL is not audited, so clear the link in the kept row ourselves
        jdbc.update("update data_subject_request_aud set person_id = null where person_id = ? and revend is null", p);
    }
}
