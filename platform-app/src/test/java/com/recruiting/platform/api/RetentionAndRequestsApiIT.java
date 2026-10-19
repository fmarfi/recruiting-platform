package com.recruiting.platform.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.recruiting.platform.contracts.RetentionAction;
import com.recruiting.platform.contracts.Role;
import com.recruiting.platform.privacy.dsr.app.PersonErasureService;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The retention job with time-shifted data (people whose retention window is in the past or about to end), the
 * ADR 0009 erasure order, the audit purge and data-subject requests.
 */
class RetentionAndRequestsApiIT extends AbstractApiIT {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PersonErasureService erasure;

    @Test
    void retentionRunFlagsPeopleNearTheEndAndAnonymisesThoseWhoseTimeIsUp() throws Exception {
        Client admin = login(Role.ADMIN);
        UUID expired = PrivacyTestSupport.insertPerson(jdbc, admin.userId(), "Expired Person",
                "expired-" + UUID.randomUUID() + "@example.test");
        PrivacyTestSupport.insertPrivacy(jdbc, expired, LocalDate.now().minusMonths(30), LocalDate.now().minusDays(1));
        PrivacyTestSupport.insertPersonHistory(jdbc, expired);
        UUID soon = PrivacyTestSupport.insertPerson(jdbc, admin.userId(), "Soon Person", null);
        PrivacyTestSupport.insertPrivacy(jdbc, soon, LocalDate.now().minusMonths(24).plusDays(10),
                LocalDate.now().plusDays(10));
        UUID fresh = PrivacyTestSupport.insertPerson(jdbc, admin.userId(), "Fresh Person", null);
        PrivacyTestSupport.insertPrivacy(jdbc, fresh, LocalDate.now(), LocalDate.now().plusMonths(24));
        // a linked data-subject request must end up minimised, not deleted
        UUID request = createRequest(admin, expired, "Expired Person", "expired@example.test", "ACCESS");

        mvc.perform(admin.with(post("/api/v1/retention/runs")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"dryRun\":true}")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.dryRun").value(true));
        assertThat(jdbc.queryForObject("select name from person where id = ?", String.class, expired))
                .isEqualTo("Expired Person"); // a dry run changes nothing

        mvc.perform(admin.with(post("/api/v1/retention/runs")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"dryRun\":false}")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.anonymisedCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.flaggedCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.triggeredBy.id").value(admin.userId().toString()));

        assertThat(jdbc.queryForObject("select retention_status from person_privacy where person_id = ?",
                String.class, soon)).isEqualTo("FLAGGED");
        assertThat(jdbc.queryForObject("select retention_status from person_privacy where person_id = ?",
                String.class, fresh)).isEqualTo("ACTIVE");
        mvc.perform(get("/api/v1/retention/flagged").session(admin.session())).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.person.id=='" + soon + "')]").exists());

        assertThat(jdbc.queryForMap("select name, email, notes, company from person where id = ?", expired))
                .containsEntry("name", "Anonymised person").containsEntry("email", null)
                .containsEntry("notes", null).containsEntry("company", null);
        Map<String, Object> dsr = jdbc.queryForMap("select requester_name, requester_email, description "
                + "from data_subject_request where id = ?", request);
        assertThat(dsr.get("requester_name")).isEqualTo("Anonymised person");
        assertThat((String) dsr.get("requester_email")).startsWith("hmac:v1:");
        assertThat(dsr.get("description")).isEqualTo("[removed]");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(jdbc.queryForObject("select retention_status from person_privacy where person_id = ?",
                    String.class, expired)).isEqualTo("ANONYMISED");
            List<Map<String, Object>> history = jdbc.queryForList("select name, email, notes from person_aud "
                    + "where id = ?", expired);
            assertThat(history).hasSize(1);
            assertThat(history.get(0)).containsEntry("name", "Anonymised person").containsEntry("email", null)
                    .containsEntry("notes", null);
        });
        assertThat(jdbc.queryForObject("select count(*) from retention_run where triggered_by = ?", Integer.class,
                admin.userId())).isEqualTo(2);
    }

    @Test
    void deletingAPersonKeepsOnlyTheDeleteRecordAndTheMinimisedRequest() throws Exception {
        Client admin = login(Role.ADMIN);
        UUID person = PrivacyTestSupport.insertPerson(jdbc, admin.userId(), "Delete Me",
                "delete-" + UUID.randomUUID() + "@example.test");
        PrivacyTestSupport.insertPrivacy(jdbc, person, LocalDate.now(), LocalDate.now().plusMonths(24));
        PrivacyTestSupport.insertPersonHistory(jdbc, person);
        UUID request = createRequest(admin, person, "Delete Me", "delete@example.test", "DELETION");

        erasure.erase(person, RetentionAction.DELETE, admin.userId());

        assertThat(jdbc.queryForObject("select count(*) from person where id = ?", Integer.class, person)).isZero();
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(jdbc.queryForList("select revtype from person_aud where id = ?", Integer.class, person))
                    .containsExactly(2);
            assertThat(jdbc.queryForObject("select count(*) from data_subject_request_aud where person_id = ?",
                    Integer.class, person)).isZero();
        });
        Map<String, Object> kept = jdbc.queryForMap("select person_id, requester_name from data_subject_request "
                + "where id = ?", request);
        assertThat(kept.get("person_id")).isNull();
        assertThat(kept.get("requester_name")).isEqualTo("Anonymised person");
    }

    @Test
    void retentionSettingsAreReadByManagersAndChangedByAdmins() throws Exception {
        Client manager = login(Role.MANAGER);
        Client admin = login(Role.ADMIN);
        String current = mvc.perform(get("/api/v1/retention/settings").session(manager.session()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.retentionMonths").value(24))
                .andReturn().getResponse().getContentAsString();
        mvc.perform(manager.with(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .put("/api/v1/retention/settings")).contentType(MediaType.APPLICATION_JSON).content(current))
                .andExpect(status().isForbidden());
        mvc.perform(admin.with(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .put("/api/v1/retention/settings")).contentType(MediaType.APPLICATION_JSON)
                .content(current.replace("\"retentionMonths\":24", "\"retentionMonths\":0")))
                .andExpect(status().isBadRequest());
        mvc.perform(admin.with(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .put("/api/v1/retention/settings")).contentType(MediaType.APPLICATION_JSON).content(current))
                .andExpect(status().isOk());
        mvc.perform(admin.with(post("/api/v1/retention/runs")).contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isBadRequest());
    }

    @Test
    void dataSubjectRequestsFollowTheirLifecycle() throws Exception {
        Client consultant = login(Role.CONSULTANT);
        Client manager = login(Role.MANAGER);
        UUID person = PrivacyTestSupport.insertPerson(jdbc, consultant.userId(), "Asking Person",
                "asking-" + UUID.randomUUID() + "@example.test");
        UUID id = createRequest(consultant, null, "Asking Person", "asking@example.test", "DELETION");

        mvc.perform(get("/api/v1/data-subject-requests").session(consultant.session()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/data-subject-requests/" + id).session(manager.session()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.overdue").value(false));
        updateRequest(manager, id, "{\"status\":\"COMPLETED\"}").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ILLEGAL_TRANSITION"));
        updateRequest(manager, id, "{\"status\":\"REJECTED\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REASON_REQUIRED"));
        updateRequest(manager, id, "{\"status\":\"IN_PROGRESS\",\"personId\":\"" + person + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.person.id").value(person.toString()));
        updateRequest(manager, id, "{\"status\":\"COMPLETED\",\"resolution\":\"Person anonymised\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.resolvedAt").exists());
        assertThat(jdbc.queryForObject("select name from person where id = ?", String.class, person))
                .isEqualTo("Anonymised person");
        mvc.perform(post("/api/v1/people/" + person + "/anonymise").contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"again\"}").session(manager.session())).andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.ResultActions updateRequest(Client client, UUID id, String body)
            throws Exception {
        return mvc.perform(client.with(patch("/api/v1/data-subject-requests/" + id))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private UUID createRequest(Client client, UUID personId, String name, String email, String type)
            throws Exception {
        String body = "{\"requesterName\":\"" + name + "\",\"requesterEmail\":\"" + email + "\",\"type\":\"" + type
                + "\",\"description\":\"please help\"" + (personId == null ? "" : ",\"personId\":\"" + personId + "\"")
                + "}";
        String json = mvc.perform(client.with(post("/api/v1/data-subject-requests"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.dueOn").exists()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(com.jayway.jsonpath.JsonPath.read(json, "$.id"));
    }
}
