package com.recruiting.platform.api;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/** Test helpers: people are owned by accounts, which is not wired in this branch, so tests insert them with SQL. */
final class PrivacyTestSupport {

    private PrivacyTestSupport() {
    }

    static UUID insertPerson(JdbcTemplate jdbc, UUID ownerId, String name, String email) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into person (id, name, type, email, owner_id, title, company, city, notes) "
                + "values (?, ?, 'CANDIDATE', ?, ?, 'CFO', 'Acme', 'Ankara', 'secret notes')",
                id, name, email, ownerId);
        // the first revision JPA would have written; Envers needs it before it can update or delete the row
        Long rev = jdbc.queryForObject("select nextval('revinfo_seq')", Long.class);
        jdbc.update("insert into revinfo (rev, revtstmp) values (?, 0)", rev);
        jdbc.update("insert into person_aud (id, rev, revtype, name, email) values (?, ?, 0, ?, ?)", id, rev, name,
                email);
        return id;
    }

    /** Creates the privacy record with a retention window ending {@code retainUntil}. */
    static void insertPrivacy(JdbcTemplate jdbc, UUID personId, LocalDate basis, LocalDate retainUntil) {
        jdbc.update("insert into person_privacy (person_id, basis_date, retain_until) values (?, ?, ?)", personId,
                java.sql.Date.valueOf(basis), java.sql.Date.valueOf(retainUntil));
        // Envers expects a first revision before it can update the row (what JPA inserts would have written)
        Long rev = jdbc.queryForObject("select nextval('revinfo_seq')", Long.class);
        jdbc.update("insert into revinfo (rev, revtstmp) values (?, 3)", rev);
        jdbc.update("insert into person_privacy_aud (person_id, rev, revtype, consent_status, basis_date, "
                + "retain_until, retention_status) values (?, ?, 0, 'NOT_SENT', ?, ?, 'ACTIVE')", personId, rev,
                java.sql.Date.valueOf(basis), java.sql.Date.valueOf(retainUntil));
    }

    /** Two Envers rows for the person: an old closed revision and the current one, both holding personal data. */
    static void insertPersonHistory(JdbcTemplate jdbc, UUID personId) {
        jdbc.update("delete from person_aud where id = ?", personId);
        Long oldRev = jdbc.queryForObject("select nextval('revinfo_seq')", Long.class);
        jdbc.update("insert into revinfo (rev, revtstmp) values (?, 1)", oldRev);
        Long currentRev = jdbc.queryForObject("select nextval('revinfo_seq')", Long.class);
        jdbc.update("insert into revinfo (rev, revtstmp) values (?, 2)", currentRev);
        jdbc.update("insert into person_aud (id, rev, revtype, revend, name, email, notes) "
                + "values (?, ?, 0, ?, 'Old Name', 'old@example.test', 'old notes')", personId, oldRev, currentRev);
        jdbc.update("insert into person_aud (id, rev, revtype, name, email, notes) "
                + "values (?, ?, 1, 'Current Name', 'cur@example.test', 'cur notes')", personId, currentRev);
    }
}
