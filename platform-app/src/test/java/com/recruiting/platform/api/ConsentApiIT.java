package com.recruiting.platform.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.recruiting.platform.contracts.Role;
import com.recruiting.platform.privacy.logging.TokenMaskingConverter;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Notices, consent e-mail, public link, manual records, status rules and token masking (S-01, NFR-01, NFR-02). */
class ConsentApiIT extends AbstractApiIT {

    @MockitoBean
    private JavaMailSender mailSender;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private org.springframework.context.ApplicationEventPublisher events;

    @Autowired
    private org.springframework.transaction.support.TransactionTemplate tx;

    @BeforeEach
    void mailSetup() {
        when(mailSender.createMimeMessage()).thenAnswer(i -> new MimeMessage(Session.getInstance(new Properties())));
    }

    @Test
    void defaultNoticesExistInBothLanguagesAndOnlyAdminsPublish() throws Exception {
        Client consultant = login(Role.CONSULTANT);
        Client admin = login(Role.ADMIN);
        mvc.perform(get("/api/v1/privacy-notices").param("current", "true").session(consultant.session()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.language=='TR')]").exists())
                .andExpect(jsonPath("$[?(@.language=='EN')]").exists());
        String body = "{\"version\":\"v-" + UUID.randomUUID() + "\",\"language\":\"EN\",\"noticeText\":\"n\","
                + "\"consentText\":\"c\",\"effectiveFrom\":\"2026-01-01\"}";
        mvc.perform(consultant.with(post("/api/v1/privacy-notices")).contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isForbidden());
        mvc.perform(admin.with(post("/api/v1/privacy-notices")).contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isCreated());
        mvc.perform(admin.with(post("/api/v1/privacy-notices")).contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isConflict());
    }

    @Test
    void emailLinkFlowGivesThenWithdrawsConsent() throws Exception {
        Client consultant = login(Role.CONSULTANT);
        UUID person = PrivacyTestSupport.insertPerson(jdbc, consultant.userId(), "Elif Yildirim",
                "elif-" + UUID.randomUUID() + "@example.test");

        mvc.perform(consultant.with(post("/api/v1/people/" + person + "/consent/requests"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"language\":\"EN\"}"))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.deliveryStatus").value("QUEUED"));
        String token = tokenFromMail();

        mvc.perform(get("/api/v1/people/" + person + "/consent").session(consultant.session()))
                .andExpect(jsonPath("$.status").value("SENT")).andExpect(jsonPath("$.noticeSentAt").exists());

        mvc.perform(get("/api/v1/public/consent/" + token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Elif")).andExpect(jsonPath("$.language").value("EN"))
                .andExpect(jsonPath("$.status").value("SENT"));
        mvc.perform(post("/api/v1/public/consent/" + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"GIVEN\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("GIVEN"));
        mvc.perform(post("/api/v1/public/consent/" + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"WITHDRAWN\"}")).andExpect(jsonPath("$.status").value("WITHDRAWN"));

        mvc.perform(get("/api/v1/people/" + person + "/consent").session(consultant.session()))
                .andExpect(jsonPath("$.records.length()").value(2))
                .andExpect(jsonPath("$.records[0].channel").value("EMAIL_LINK"))
                .andExpect(jsonPath("$.records[0].recordedBy").doesNotExist());
        mvc.perform(get("/api/v1/public/consent/" + "x".repeat(43))).andExpect(status().isNotFound());
    }

    @Test
    void sendingIsRefusedWithoutEmailOrAfterConsentWasGiven() throws Exception {
        Client consultant = login(Role.CONSULTANT);
        UUID noMail = PrivacyTestSupport.insertPerson(jdbc, consultant.userId(), "No Mail", null);
        mvc.perform(consultant.with(post("/api/v1/people/" + noMail + "/consent/requests")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("STATE_CONFLICT"));

        UUID given = PrivacyTestSupport.insertPerson(jdbc, consultant.userId(), "Given Person",
                "given-" + UUID.randomUUID() + "@example.test");
        record(consultant, given, "GIVEN", "IN_PERSON").andExpect(status().isCreated());
        mvc.perform(consultant.with(post("/api/v1/people/" + given + "/consent/requests")))
                .andExpect(status().isConflict());
        mvc.perform(consultant.with(post("/api/v1/people/" + UUID.randomUUID() + "/consent/requests")))
                .andExpect(status().isNotFound());
    }

    @Test
    void manualRecordsFollowTheLifecycleAndValidateInput() throws Exception {
        Client consultant = login(Role.CONSULTANT);
        UUID person = PrivacyTestSupport.insertPerson(jdbc, consultant.userId(), "Manual Person", null);
        record(consultant, person, "WITHDRAWN", "PHONE").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ILLEGAL_TRANSITION"));
        record(consultant, person, "GIVEN", "EMAIL_LINK").andExpect(status().isBadRequest());
        record(consultant, person, "GIVEN", "PHONE").andExpect(status().isCreated())
                .andExpect(jsonPath("$.channel").value("PHONE")).andExpect(jsonPath("$.recordedBy.id")
                        .value(consultant.userId().toString()));
        record(consultant, person, "GIVEN", "PHONE").andExpect(status().isCreated()); // renewal, same status
        record(consultant, person, "WITHDRAWN", "PHONE").andExpect(status().isCreated());
        mvc.perform(get("/api/v1/people/" + person + "/consent").session(consultant.session()))
                .andExpect(jsonPath("$.status").value("WITHDRAWN")).andExpect(jsonPath("$.records.length()").value(3));
    }

    @Test
    void consentListsAndCampaignsAreForManagers() throws Exception {
        Client consultant = login(Role.CONSULTANT);
        Client manager = login(Role.MANAGER);
        mvc.perform(get("/api/v1/consent-requests").session(consultant.session())).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/consent-requests").session(manager.session())).andExpect(status().isOk());
        UUID mailed = PrivacyTestSupport.insertPerson(jdbc, consultant.userId(), "Bulk One",
                "bulk-" + UUID.randomUUID() + "@example.test");
        UUID noMail = PrivacyTestSupport.insertPerson(jdbc, consultant.userId(), "Bulk Two", null);
        mvc.perform(manager.with(post("/api/v1/consent-requests/bulk")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"personIds\":[\"" + mailed + "\",\"" + noMail + "\"],\"language\":\"TR\"}"))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.queued").value(1))
                .andExpect(jsonPath("$.skipped").value(1));
        // a second campaign inside 7 days skips the unanswered one
        mvc.perform(manager.with(post("/api/v1/consent-requests/bulk")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"personIds\":[\"" + mailed + "\"]}")).andExpect(jsonPath("$.queued").value(0));
    }

    @Test
    void aNewPersonGetsAPrivacyRecordAndTheAutomaticNoticeEmail() throws Exception {
        Client consultant = login(Role.CONSULTANT);
        UUID person = PrivacyTestSupport.insertPerson(jdbc, consultant.userId(), "Auto Person",
                "auto-" + UUID.randomUUID() + "@example.test");
        tx.executeWithoutResult(s -> events.publishEvent(new com.recruiting.platform.contracts.PersonCreated(
                UUID.randomUUID(), java.time.Instant.now(), consultant.userId(), person,
                com.recruiting.platform.contracts.PersonType.CANDIDATE, consultant.userId(), true, null)));
        org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(jdbc.queryForObject("select consent_status from person_privacy where person_id = ?",
                        String.class, person)).isEqualTo("SENT"));
        tokenFromMail();
    }

    @Test
    void consentTokensAreMaskedInLogLines() {
        String line = "GET /api/v1/public/consent/abcDEF123_-xyz0987654321abcDEF123_-xyz0987 returned 200";
        assertThat(TokenMaskingConverter.mask(line)).isEqualTo("GET /api/v1/public/consent/*** returned 200")
                .doesNotContain("abcDEF");
    }

    private org.springframework.test.web.servlet.ResultActions record(Client client, UUID person, String decision,
            String channel) throws Exception {
        String notice = com.jayway.jsonpath.JsonPath.read(mvc.perform(get("/api/v1/privacy-notices")
                .param("current", "true").param("language", "TR").session(client.session())).andReturn()
                .getResponse().getContentAsString(), "$[0].id");
        return mvc.perform(client.with(post("/api/v1/people/" + person + "/consent/records"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"" + decision + "\",\"channel\":\""
                        + channel + "\",\"noticeId\":\"" + notice + "\",\"evidence\":\"signed form\"}"));
    }

    private String tokenFromMail() throws Exception {
        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, timeout(5000).atLeastOnce()).send(sent.capture());
        Matcher m = Pattern.compile("/consent/([A-Za-z0-9_-]{43})").matcher(String.valueOf(
                sent.getValue().getContent()));
        assertThat(m.find()).isTrue();
        return m.group(1);
    }
}
