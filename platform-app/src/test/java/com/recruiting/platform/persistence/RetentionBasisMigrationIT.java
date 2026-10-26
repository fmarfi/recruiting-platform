package com.recruiting.platform.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * V202610260010: the switch of the retention basis to FROM_LAST_CONSENT recomputes basis_date and retain_until of
 * every person that is not anonymised (schema.md, "Basis switch migration"). Flyway already ran it on the empty
 * database, so the test puts the settings row back to the old default, seeds people and runs the script again.
 */
class RetentionBasisMigrationIT extends Wave2bSupport {

    private static final ZoneId ISTANBUL = ZoneId.of("Europe/Istanbul");

    private String script() throws IOException {
        return new String(new ClassPathResource("db/migration/V202610260010__privacy_retention_basis_switch.sql")
                .getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    private record Seed(UUID id, LocalDate created) {}

    private Seed seedPerson(UUID owner, OffsetDateTime createdAt, String retentionStatus, LocalDate retainUntil,
            Boolean anonymised) {
        UUID id = person(owner);
        jdbc.update("UPDATE person SET created_at = ? WHERE id = ?", createdAt, id);
        jdbc.update("INSERT INTO person_privacy (person_id, basis_date, retain_until, retention_status, flagged_at) "
                + "VALUES (?, ?, ?, ?, CASE WHEN ? = 'FLAGGED' THEN now() END)",
                id, Date.valueOf(createdAt.atZoneSameInstant(ISTANBUL).toLocalDate()), Date.valueOf(retainUntil),
                retentionStatus, retentionStatus);
        return new Seed(id, createdAt.atZoneSameInstant(ISTANBUL).toLocalDate());
    }

    private void consent(UUID personId, String decision, OffsetDateTime at) {
        UUID notice = UUID.randomUUID();
        jdbc.update("INSERT INTO privacy_notice (id, version, language, notice_text, consent_text, effective_from) "
                + "VALUES (?, ?, 'TR', 'n', 'c', current_date)", notice, unique("v"));
        jdbc.update("INSERT INTO consent_record (id, person_id, decision, channel, notice_id, recorded_at) "
                + "VALUES (?, ?, ?, 'PHONE', ?, ?)", UUID.randomUUID(), personId, decision, notice, at);
    }

    private LocalDate retainUntil(UUID personId) {
        return jdbc.queryForObject("SELECT retain_until FROM person_privacy WHERE person_id = ?", Date.class, personId)
                .toLocalDate();
    }

    private LocalDate basisDate(UUID personId) {
        return jdbc.queryForObject("SELECT basis_date FROM person_privacy WHERE person_id = ?", Date.class, personId)
                .toLocalDate();
    }

    private String status(UUID personId) {
        return jdbc.queryForObject("SELECT retention_status FROM person_privacy WHERE person_id = ?", String.class,
                personId);
    }

    @Test
    void switchRecomputesBasisDateAndRetainUntilAndUnflagsPeopleOutsideTheWarningWindow() throws IOException {
        String originalBasis = jdbc.queryForObject("SELECT basis FROM retention_settings WHERE id = 1", String.class);
        int months = jdbc.queryForObject("SELECT retention_months FROM retention_settings WHERE id = 1", Integer.class);
        int warning = jdbc.queryForObject("SELECT warning_days FROM retention_settings WHERE id = 1", Integer.class);
        try {
            jdbc.update("UPDATE retention_settings SET basis = 'FROM_CREATED' WHERE id = 1");
            UUID owner = newUser().getId();
            OffsetDateTime longAgo = OffsetDateTime.now(ZoneOffset.UTC).minusYears(3);
            OffsetDateTime twoYearsAgo = OffsetDateTime.now(ZoneOffset.UTC).minusYears(2).minusDays(10);
            OffsetDateTime recentConsent = OffsetDateTime.now(ZoneOffset.UTC).minusDays(20);
            OffsetDateTime oldConsent = OffsetDateTime.now(ZoneOffset.UTC).minusYears(1).minusDays(3);
            OffsetDateTime newerConsent = OffsetDateTime.now(ZoneOffset.UTC).minusMonths(2);

            // no consent at all: basis falls back to created_at
            Seed none = seedPerson(owner, longAgo, "ACTIVE", longAgo.toLocalDate().plusMonths(months), false);
            // an old GIVEN consent
            Seed old = seedPerson(owner, longAgo, "ACTIVE", longAgo.toLocalDate().plusMonths(months), false);
            consent(old.id(), "GIVEN", oldConsent);
            // the latest GIVEN counts, a later WITHDRAWN or REFUSED does not move the basis
            Seed latest = seedPerson(owner, longAgo, "ACTIVE", longAgo.toLocalDate().plusMonths(months), false);
            consent(latest.id(), "GIVEN", oldConsent);
            consent(latest.id(), "GIVEN", newerConsent);
            consent(latest.id(), "WITHDRAWN", recentConsent);
            // flagged for retention, but a recent GIVEN consent moves it out of the warning window: back to ACTIVE
            Seed flaggedRenewed = seedPerson(owner, twoYearsAgo, "FLAGGED",
                    twoYearsAgo.toLocalDate().plusMonths(months), false);
            consent(flaggedRenewed.id(), "GIVEN", recentConsent);
            // flagged and still due: stays FLAGGED
            Seed flaggedDue = seedPerson(owner, twoYearsAgo, "FLAGGED", twoYearsAgo.toLocalDate().plusMonths(months), false);
            // already anonymised: untouched
            Seed anonymised = seedPerson(owner, longAgo, "ANONYMISED", LocalDate.of(2020, 1, 1), true);
            consent(anonymised.id(), "GIVEN", recentConsent);

            jdbc.execute(script());

            assertThat(jdbc.queryForObject("SELECT basis FROM retention_settings WHERE id = 1", String.class))
                    .isEqualTo("FROM_LAST_CONSENT");

            assertThat(basisDate(none.id())).isEqualTo(none.created());
            assertThat(retainUntil(none.id())).isEqualTo(none.created().plusMonths(months));
            assertThat(lastConsent(none.id())).isNull();

            LocalDate oldBasis = oldConsent.atZoneSameInstant(ISTANBUL).toLocalDate();
            assertThat(basisDate(old.id())).isEqualTo(oldBasis);
            assertThat(retainUntil(old.id())).isEqualTo(oldBasis.plusMonths(months));

            LocalDate newerBasis = newerConsent.atZoneSameInstant(ISTANBUL).toLocalDate();
            assertThat(basisDate(latest.id())).isEqualTo(newerBasis);
            assertThat(retainUntil(latest.id())).isEqualTo(newerBasis.plusMonths(months));
            assertThat(lastConsent(latest.id())).isNotNull();

            LocalDate renewedBasis = recentConsent.atZoneSameInstant(ISTANBUL).toLocalDate();
            assertThat(retainUntil(flaggedRenewed.id())).isEqualTo(renewedBasis.plusMonths(months));
            assertThat(retainUntil(flaggedRenewed.id()).minusDays(warning)).isAfter(LocalDate.now());
            assertThat(status(flaggedRenewed.id())).isEqualTo("ACTIVE");
            assertThat(jdbc.queryForObject("SELECT flagged_at FROM person_privacy WHERE person_id = ?",
                    Object.class, flaggedRenewed.id())).isNull();

            assertThat(retainUntil(flaggedDue.id())).isEqualTo(flaggedDue.created().plusMonths(months));
            assertThat(status(flaggedDue.id())).isEqualTo("FLAGGED");

            assertThat(retainUntil(anonymised.id())).isEqualTo(LocalDate.of(2020, 1, 1));
            assertThat(status(anonymised.id())).isEqualTo("ANONYMISED");
            // the migration never anonymises and never touches the person copy
            assertThat(jdbc.queryForObject("SELECT retention_status FROM person WHERE id = ?", String.class,
                    flaggedRenewed.id())).isEqualTo("ACTIVE");
        } finally {
            jdbc.update("UPDATE retention_settings SET basis = ? WHERE id = 1", originalBasis);
        }
    }

    @Test
    void aBasisChosenByAnAdminIsKeptAndNothingIsRecomputed() throws IOException {
        String originalBasis = jdbc.queryForObject("SELECT basis FROM retention_settings WHERE id = 1", String.class);
        try {
            jdbc.update("UPDATE retention_settings SET basis = 'FROM_LAST_CONTACT' WHERE id = 1");
            UUID owner = newUser().getId();
            OffsetDateTime created = OffsetDateTime.now(ZoneOffset.UTC).minusYears(1);
            LocalDate custom = LocalDate.now().plusMonths(5);
            Seed s = seedPerson(owner, created, "ACTIVE", custom, false);
            consent(s.id(), "GIVEN", OffsetDateTime.now(ZoneOffset.UTC).minusDays(2));

            jdbc.execute(script());

            assertThat(jdbc.queryForObject("SELECT basis FROM retention_settings WHERE id = 1", String.class))
                    .isEqualTo("FROM_LAST_CONTACT");
            assertThat(retainUntil(s.id())).isEqualTo(custom);
            // the last consent is still backfilled: it is a fact, not a policy
            assertThat(lastConsent(s.id())).isNotNull();
        } finally {
            jdbc.update("UPDATE retention_settings SET basis = ? WHERE id = 1", originalBasis);
        }
    }

    private Object lastConsent(UUID personId) {
        return jdbc.queryForObject("SELECT last_consent_at FROM person_privacy WHERE person_id = ?", Object.class,
                personId);
    }
}
