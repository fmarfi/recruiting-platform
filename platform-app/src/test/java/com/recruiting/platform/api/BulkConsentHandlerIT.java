package com.recruiting.platform.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.recruiting.platform.contracts.BulkActionType;
import com.recruiting.platform.contracts.BulkOutcome;
import com.recruiting.platform.contracts.BulkResultCode;
import com.recruiting.platform.contracts.Role;
import com.recruiting.platform.pipeline.BulkActionHandler;
import com.recruiting.platform.pipeline.BulkActionHandler.Result;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Bulk REQUEST_CONSENT handler: consent gate codes (ADR 0010), roles, no re-asking REFUSED or WITHDRAWN. */
class BulkConsentHandlerIT extends AbstractApiIT {

    @MockitoBean
    private JavaMailSender mailSender;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private List<BulkActionHandler> handlers;

    @BeforeEach
    void mail() {
        when(mailSender.createMimeMessage()).thenAnswer(i -> new MimeMessage(Session.getInstance(new Properties())));
    }

    private BulkActionHandler handler() {
        return handlers.stream().filter(h -> h.action() == BulkActionType.REQUEST_CONSENT).findFirst().orElseThrow();
    }

    private UUID person(Client c, String email, String status) {
        UUID p = PrivacyTestSupport.insertPerson(jdbc, c.userId(), "Bulk " + UUID.randomUUID(), email);
        if (status != null) {
            PrivacyTestSupport.insertPrivacy(jdbc, p, java.time.LocalDate.now(), java.time.LocalDate.now().plusYears(2));
            jdbc.update("update person_privacy set consent_status = ? where person_id = ?", status, p);
        }
        return p;
    }

    private Result run(Client c, Role role, UUID id) {
        return handler().handle(c.userId(), role, Map.of("language", "EN"), id);
    }

    @Test
    void appliesTheConsentGateWithADRCodes() throws Exception {
        Client m = login(Role.MANAGER);
        String mail = "bulk-" + UUID.randomUUID() + "@example.test";
        UUID fresh = person(m, mail, null);
        assertThat(run(m, Role.MANAGER, fresh)).isEqualTo(Result.ok());
        assertThat(jdbc.queryForObject("select consent_status from person_privacy where person_id = ?", String.class,
                fresh)).isEqualTo("SENT");
        assertThat(run(m, Role.ADMIN, fresh)).isEqualTo(Result.skipped(BulkResultCode.REQUEST_PENDING));

        assertThat(run(m, Role.MANAGER, person(m, mail + "1", "GIVEN"))).isEqualTo(Result.skipped(BulkResultCode.CONSENT_GIVEN));
        assertThat(run(m, Role.MANAGER, person(m, mail + "2", "REFUSED")))
                .isEqualTo(Result.skipped(BulkResultCode.CONSENT_REFUSED));
        assertThat(run(m, Role.MANAGER, person(m, mail + "3", "WITHDRAWN")))
                .isEqualTo(Result.skipped(BulkResultCode.CONSENT_WITHDRAWN));
        assertThat(run(m, Role.MANAGER, person(m, null, null))).isEqualTo(Result.skipped(BulkResultCode.NO_EMAIL));
        assertThat(run(m, Role.MANAGER, UUID.randomUUID())).isEqualTo(Result.skipped(BulkResultCode.NOT_FOUND));
        UUID anon = person(m, mail + "4", null);
        jdbc.update("update person set anonymised_at = now() where id = ?", anon);
        assertThat(run(m, Role.MANAGER, anon)).isEqualTo(Result.skipped(BulkResultCode.ANONYMISED));
        assertThat(jdbc.queryForObject("select count(*) from consent_request where person_id = ?", Integer.class,
                anon)).isZero();
    }

    @Test
    void consultantsAreForbiddenAndNothingIsSent() throws Exception {
        Client c = login(Role.CONSULTANT);
        UUID p = person(c, "bulk-" + UUID.randomUUID() + "@example.test", null);
        Result r = run(c, Role.CONSULTANT, p);
        assertThat(r.outcome()).isEqualTo(BulkOutcome.SKIPPED);
        assertThat(r.code()).isEqualTo(BulkResultCode.FORBIDDEN);
        assertThat(jdbc.queryForObject("select count(*) from consent_request where person_id = ?", Integer.class, p))
                .isZero();
    }
}
