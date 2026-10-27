package com.recruiting.platform.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.recruiting.platform.contracts.PersonFileKind;
import com.recruiting.platform.contracts.PersonFileUploaded;
import com.recruiting.platform.contracts.RetentionRenewed;
import com.recruiting.platform.contracts.Role;
import com.recruiting.platform.privacy.retention.app.RetentionBasisSync;
import com.recruiting.platform.privacy.retention.app.RetentionRunService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

/** Consent engine (Wave 2b): sharing guard, CV-upload e-mail, renewal and reminders, tracked link, basis sync. */
@Import(ConsentEngineApiIT.Events.class)
class ConsentEngineApiIT extends AbstractApiIT {

    /** Collects RetentionRenewed events published during a test. */
    static class Events {
        static final java.util.List<RetentionRenewed> RENEWED = new java.util.concurrent.CopyOnWriteArrayList<>();

        @EventListener
        void on(RetentionRenewed e) {
            RENEWED.add(e);
        }
    }

    @MockitoBean
    private JavaMailSender mailSender;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private TransactionTemplate tx;

    @Autowired
    private RetentionRunService runs;

    @Autowired
    private RetentionBasisSync basisSync;

    @BeforeEach
    void mail() {
        when(mailSender.createMimeMessage()).thenAnswer(i -> new MimeMessage(Session.getInstance(new Properties())));
    }

    private UUID person(Client c, String name) {
        return PrivacyTestSupport.insertPerson(jdbc, c.userId(), name, name.replace(' ', '.').toLowerCase()
                + "-" + UUID.randomUUID() + "@example.test");
    }

    private void setStatus(UUID person, String status) {
        PrivacyTestSupport.insertPrivacy(jdbc, person, LocalDate.now(), LocalDate.now().plusMonths(24));
        jdbc.update("update person_privacy set consent_status = ? where person_id = ?", status, person);
    }

    private int requests(UUID person) {
        return jdbc.queryForObject("select count(*) from consent_request where person_id = ?", Integer.class, person);
    }

    private void uploadCv(UUID person, Client c) {
        tx.executeWithoutResult(s -> publisher.publishEvent(new PersonFileUploaded(UUID.randomUUID(), Instant.now(),
                c.userId(), person, UUID.randomUUID(), PersonFileKind.CV, 1)));
    }

    @Test
    void shareCheckAnswersFromTheMasterConsentData() throws Exception {
        Client c = login(Role.CONSULTANT);
        UUID given = person(c, "Given Gina");
        UUID pending = person(c, "Pending Paul");
        UUID refused = person(c, "Refused Rita");
        UUID unasked = person(c, "Unasked Uma");
        setStatus(given, "GIVEN");
        setStatus(pending, "SENT");
        setStatus(refused, "REFUSED");
        String body = "{\"personIds\":[\"" + given + "\",\"" + pending + "\",\"" + refused + "\",\"" + unasked
                + "\",\"" + UUID.randomUUID() + "\"]}";
        mvc.perform(c.with(post("/api/v1/consent/share-check")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].shareable").value(true)).andExpect(jsonPath("$[0].reason").doesNotExist())
                .andExpect(jsonPath("$[1].reason").value("CONSENT_PENDING"))
                .andExpect(jsonPath("$[2].reason").value("CONSENT_REFUSED"))
                .andExpect(jsonPath("$[3].reason").value("CONSENT_NOT_REQUESTED"));
        mvc.perform(c.with(post("/api/v1/consent/share-check")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"personIds\":[]}")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/people/" + pending + "/consent").session(c.session()))
                .andExpect(jsonPath("$.shareable").value(false))
                .andExpect(jsonPath("$.shareBlockReason").value("CONSENT_PENDING"));
    }

    @Test
    void cvUploadSendsOneNoticeAndNeverToRefusedOrWithdrawnPeople() throws Exception {
        Client c = login(Role.CONSULTANT);
        UUID fresh = person(c, "Fresh Fay");
        uploadCv(fresh, c);
        await().atMost(Duration.ofSeconds(10)).until(() -> requests(fresh) == 1);
        assertThat(jdbc.queryForObject("select consent_status from person_privacy where person_id = ?", String.class,
                fresh)).isEqualTo("SENT");
        uploadCv(fresh, c); // second CV while the first request is pending: no duplicate
        UUID refused = person(c, "Refused Ruth");
        UUID withdrawn = person(c, "Withdrawn Wes");
        setStatus(refused, "REFUSED");
        setStatus(withdrawn, "WITHDRAWN");
        uploadCv(refused, c);
        uploadCv(withdrawn, c);
        UUID control = person(c, "Control Cem");
        uploadCv(control, c); // the control person proves the listeners have run
        await().atMost(Duration.ofSeconds(10)).until(() -> requests(control) == 1);
        Thread.sleep(500);
        assertThat(requests(fresh)).isEqualTo(1);
        assertThat(requests(refused)).isZero();
        assertThat(requests(withdrawn)).isZero();

        jdbc.update("update retention_settings set auto_send_on_cv_upload = false where id = 1");
        try {
            UUID off = person(c, "Off Olga");
            uploadCv(off, c);
            UUID after = person(c, "After Ali");
            jdbc.update("update retention_settings set auto_send_on_cv_upload = false where id = 1");
            uploadCv(after, c);
            Thread.sleep(1500);
            assertThat(requests(off)).isZero();
            assertThat(requests(after)).isZero();
        } finally {
            jdbc.update("update retention_settings set auto_send_on_cv_upload = true where id = 1");
        }
    }

    @Test
    void renewalRulesTrackedLinkAndRetentionRestart() throws Exception {
        Client c = login(Role.CONSULTANT);
        UUID p = person(c, "Renew Rana");
        mvc.perform(c.with(post("/api/v1/people/" + p + "/consent/requests")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"purpose\":\"RENEWAL\"}")).andExpect(status().isConflict()); // not GIVEN yet
        mvc.perform(c.with(post("/api/v1/people/" + p + "/consent/records")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"GIVEN\",\"channel\":\"PHONE\",\"noticeId\":\"" + noticeId() + "\"}"))
                .andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("select last_consent_at from person_privacy where person_id = ?",
                java.sql.Timestamp.class, p)).isNotNull();
        mvc.perform(c.with(post("/api/v1/people/" + p + "/consent/requests"))).andExpect(status().isConflict());
        mvc.perform(c.with(post("/api/v1/people/" + p + "/consent/requests")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"purpose\":\"RENEWAL\",\"language\":\"EN\"}")).andExpect(status().isAccepted())
                .andExpect(jsonPath("$.purpose").value("RENEWAL")).andExpect(jsonPath("$.openedAt").doesNotExist());
        // the link is not exposed: read the hash row, issue a known token instead
        String token = "renewal-token-" + UUID.randomUUID() + "-0123456789";
        int updated = jdbc.update("update consent_request set token_hash = encode(sha256(convert_to(?, 'UTF8')), 'hex') "
                + "where person_id = ? and purpose = 'RENEWAL'", token, p);
        assertThat(updated).as("renewal request rows").isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from person where id = ? and anonymised_at is null",
                Integer.class, p)).as("person still active").isEqualTo(1);

        RetentionRenewedEvents.clear();
        mvc.perform(get("/api/v1/public/consent/" + token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("GIVEN"));
        assertThat(jdbc.queryForObject("select opened_at from consent_request where person_id = ? "
                + "and purpose = 'RENEWAL'", java.sql.Timestamp.class, p)).isNotNull(); // tracked, not consent
        assertThat(jdbc.queryForObject("select count(*) from consent_record where person_id = ?", Integer.class, p))
                .isEqualTo(1); // opening created no record
        // age the clock, then renew through the link
        jdbc.update("update person_privacy set basis_date = current_date - 700, retain_until = current_date + 30, "
                + "retention_status = 'FLAGGED', flagged_at = now() where person_id = ?", p);
        mvc.perform(post("/api/v1/public/consent/" + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"GIVEN\"}")).andExpect(status().isOk());
        Map<String, Object> row = jdbc.queryForMap("select basis_date, retain_until, retention_status, flagged_at "
                + "from person_privacy where person_id = ?", p);
        assertThat(((java.sql.Date) row.get("basis_date")).toLocalDate()).isEqualTo(LocalDate.now());
        assertThat(((java.sql.Date) row.get("retain_until")).toLocalDate()).isAfter(LocalDate.now().plusMonths(23));
        assertThat(row.get("retention_status")).isEqualTo("ACTIVE");
        assertThat(row.get("flagged_at")).isNull();
        assertThat(RetentionRenewedEvents.forPerson(p)).hasSize(1);
        assertThat(RetentionRenewedEvents.forPerson(p).get(0).actorId()).isNull();
    }

    @Test
    void remindersAreSentOncePerStepAndNeverToRefusedPeople() throws Exception {
        Client c = login(Role.CONSULTANT);
        UUID given = person(c, "Remind Reza");
        UUID sent = person(c, "Remind Selin");
        UUID refused = person(c, "Remind Rafet");
        UUID early = person(c, "Remind Early");
        LocalDate until50 = LocalDate.now().plusDays(50);
        for (UUID p : List.of(given, sent, refused)) {
            PrivacyTestSupport.insertPrivacy(jdbc, p, until50.minusMonths(24), until50);
        }
        PrivacyTestSupport.insertPrivacy(jdbc, early, LocalDate.now(), LocalDate.now().plusMonths(24));
        jdbc.update("update person_privacy set consent_status = 'GIVEN' where person_id in (?, ?)", given, early);
        jdbc.update("update person_privacy set consent_status = 'SENT' where person_id = ?", sent);
        jdbc.update("update person_privacy set consent_status = 'REFUSED' where person_id = ?", refused);

        runs.run(false, null);
        runs.run(false, null); // idempotent
        assertThat(requests(given)).isEqualTo(1);
        assertThat(jdbc.queryForMap("select purpose, reminder_days_before, reminder_for_retain_until "
                + "from consent_request where person_id = ?", given)).containsEntry("purpose", "RENEWAL")
                .satisfies(m -> assertThat(((Number) m.get("reminder_days_before")).intValue()).isEqualTo(60));
        assertThat(requests(sent)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select purpose from consent_request where person_id = ?", String.class, sent))
                .isEqualTo("INITIAL");
        assertThat(requests(refused)).isZero();
        assertThat(requests(early)).isZero();
        // an unrelated status copy must not change: the person stays GIVEN after a RENEWAL request
        assertThat(jdbc.queryForObject("select consent_status from person_privacy where person_id = ?", String.class,
                given)).isEqualTo("GIVEN");
        mvc.perform(get("/api/v1/people/" + early + "/consent").session(c.session()))
                .andExpect(jsonPath("$.nextReminderOn").value(LocalDate.now().plusMonths(24).minusDays(60).toString()));

        // 40 days later the 14-day step becomes due: one more reminder, then no more
        jdbc.update("update person_privacy set retain_until = current_date + 10 where person_id = ?", given);
        jdbc.update("update person_privacy set basis_date = current_date - 700 where person_id = ?", given);
        runs.run(false, null);
        assertThat(requests(given)).isEqualTo(2);
        runs.run(false, null);
        assertThat(requests(given)).isEqualTo(2);
    }

    @Test
    void basisSyncResavesStalePrivacyRowsThroughJpaAndRenewsUnflaggedPeople() throws Exception {
        Client c = login(Role.CONSULTANT);
        UUID p = person(c, "Sync Sven");
        PrivacyTestSupport.insertPrivacy(jdbc, p, LocalDate.now().minusMonths(23), LocalDate.now().plusDays(20));
        jdbc.update("update person_privacy_aud set retention_status = 'FLAGGED' where person_id = ?", p);
        // what the basis-switch migration does with SQL: new dates, unflagged, no Envers revision
        jdbc.update("update person_privacy set basis_date = current_date, retain_until = current_date + 730, "
                + "retention_status = 'ACTIVE' where person_id = ?", p);
        RetentionRenewedEvents.clear();

        assertThat(basisSync.sync()).isGreaterThanOrEqualTo(1);
        Map<String, Object> aud = jdbc.queryForMap("select basis_date, retain_until, retention_status "
                + "from person_privacy_aud where person_id = ? and revend is null", p);
        assertThat(aud.get("retention_status")).isEqualTo("ACTIVE");
        assertThat(((java.sql.Date) aud.get("retain_until")).toLocalDate()).isEqualTo(LocalDate.now().plusDays(730));
        assertThat(RetentionRenewedEvents.forPerson(p)).hasSize(1);
        basisSync.sync(); // idempotent
        assertThat(RetentionRenewedEvents.forPerson(p)).hasSize(1);
    }

    private UUID noticeId() {
        return jdbc.queryForObject("select id from privacy_notice where language = 'TR' limit 1", UUID.class);
    }

    /** Small facade over the collected events. */
    static final class RetentionRenewedEvents {
        static void clear() {
            Events.RENEWED.clear();
        }

        static List<RetentionRenewed> forPerson(UUID id) {
            return Events.RENEWED.stream().filter(e -> e.personId().equals(id)).toList();
        }
    }
}
