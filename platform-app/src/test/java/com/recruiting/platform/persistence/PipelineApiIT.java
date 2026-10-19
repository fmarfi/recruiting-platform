package com.recruiting.platform.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.recruiting.platform.accounts.domain.Account;
import com.recruiting.platform.accounts.domain.AccountRepository;
import com.recruiting.platform.accounts.domain.Person;
import com.recruiting.platform.accounts.domain.PersonRepository;
import com.recruiting.platform.contracts.AccountStatus;
import com.recruiting.platform.contracts.ConsentStatus;
import com.recruiting.platform.contracts.PersonAnonymised;
import com.recruiting.platform.contracts.PersonType;
import com.recruiting.platform.contracts.RetentionFlagged;
import com.recruiting.platform.contracts.RetentionStatus;
import com.recruiting.platform.pipeline.domain.NextActionRepository;
import com.recruiting.platform.security.IdentifiedUser;
import com.recruiting.platform.security.domain.AppUser;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * W1-11 end-to-end checks of the pipeline endpoints through MockMvc on a real PostgreSQL: status rules, guards,
 * reasons, roles, timeline from events, follow-ups, activities, dashboard and My day, and job orders in /search.
 * Runs through the real filter chain, error mapping and method security; only the login step is replaced by
 * a ready authentication whose principal carries the user id (as the session principal does).
 */
@AutoConfigureMockMvc
class PipelineApiIT extends AbstractPersistenceIT {

    private record Principal(UUID userId) implements IdentifiedUser {
    }

    @Autowired
    private MockMvc mvc;
    @Autowired
    private AccountRepository accounts;
    @Autowired
    private PersonRepository people;
    @Autowired
    private NextActionRepository nextActions;
    @Autowired
    private ApplicationEventPublisher publisher;

    private UUID consultant;
    private UUID otherConsultant;
    private UUID manager;
    private UUID accountId;
    private UUID personA;
    private UUID personB;

    @BeforeEach
    void setUp() {
        consultant = newUser().getId();
        otherConsultant = newUser().getId();
        AppUser m = newUser();
        manager = m.getId();
        Account a = new Account();
        a.setName("Api Corp " + UUID.randomUUID());
        a.setSector("Retail");
        a.setOwnerId(consultant);
        a.setStatus(AccountStatus.ACTIVE);
        a.setStatusChangedAt(Instant.now());
        a.setOpenedOn(LocalDate.now());
        a.setOpenJobCount(0);
        accountId = tx.execute(s -> accounts.save(a)).getId();
        personA = newPerson();
        personB = newPerson();
    }

    private UUID newPerson() {
        Person p = new Person();
        p.setName("Candidate " + UUID.randomUUID());
        p.setType(PersonType.CANDIDATE);
        p.setOwnerId(consultant);
        p.setConsentStatus(ConsentStatus.NOT_SENT);
        p.setRetentionStatus(RetentionStatus.ACTIVE);
        return tx.execute(s -> people.save(p)).getId();
    }

    private RequestPostProcessor as(UUID id, String role) {
        return authentication(new UsernamePasswordAuthenticationToken(new Principal(id), "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private ResultActions call(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder b,
            UUID who, String role, String body) throws Exception {
        b.with(as(who, role)).with(csrf());
        if (body != null) {
            b.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mvc.perform(b);
    }

    private String id(ResultActions r) throws Exception {
        return JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id");
    }

    private String newJob() throws Exception {
        return id(call(post("/api/v1/job-orders"), consultant, "CONSULTANT",
                "{\"accountId\":\"" + accountId + "\",\"title\":\"CFO " + UUID.randomUUID() + "\"}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("OPEN")));
    }

    private void jobStatus(String job, String to, String reason, UUID who, String role, int expected,
            String code) throws Exception {
        String body = "{\"status\":\"" + to + "\"" + (reason == null ? "" : ",\"reason\":\"" + reason + "\"") + "}";
        ResultActions r = call(patch("/api/v1/job-orders/" + job + "/status"), who, role, body)
                .andExpect(status().is(expected));
        if (code != null) {
            r.andExpect(jsonPath("$.code").value(code));
        }
    }

    @Test
    void jobOrderStatusRules() throws Exception {
        String job = newJob();
        jobStatus(job, "FILLED", null, consultant, "CONSULTANT", 409, "ILLEGAL_TRANSITION");
        jobStatus(job, "OPEN", null, consultant, "CONSULTANT", 409, "ILLEGAL_TRANSITION");
        jobStatus(job, "CANCELLED", null, consultant, "CONSULTANT", 400, "REASON_REQUIRED");
        jobStatus(job, "CANCELLED", "ab", consultant, "CONSULTANT", 400, "VALIDATION_FAILED");
        jobStatus(job, "CANCELLED", "Client cancelled the search", consultant, "CONSULTANT", 200, null);
        jobStatus(job, "OPEN", null, consultant, "CONSULTANT", 403, "FORBIDDEN");
        jobStatus(job, "OPEN", null, manager, "MANAGER", 200, null);
        jobStatus(job, "SOURCING", null, consultant, "CONSULTANT", 200, null);
        call(get("/api/v1/job-orders/" + job), consultant, "CONSULTANT", null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SOURCING"))
                .andExpect(jsonPath("$.stageCounts.SOURCED").value(0));
        call(get("/api/v1/job-orders").param("accountId", accountId.toString()), consultant, "CONSULTANT", null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void searchFindsJobOrdersAndUnknownAccountIs404() throws Exception {
        String token = "Zeppelin" + UUID.randomUUID().toString().substring(0, 8);
        call(post("/api/v1/job-orders"), consultant, "CONSULTANT",
                "{\"accountId\":\"" + accountId + "\",\"title\":\"" + token + " director\"}")
                .andExpect(status().isCreated());
        call(get("/api/v1/search").param("q", token.toLowerCase()).param("types", "JOB_ORDER"), consultant,
                "CONSULTANT", null).andExpect(status().isOk()).andExpect(jsonPath("$.jobOrders.length()").value(1))
                .andExpect(jsonPath("$.totals.jobOrders").value(1));
        call(get("/api/v1/search").param("q", token).param("sector", "Mining"), consultant, "CONSULTANT", null)
                .andExpect(jsonPath("$.totals.jobOrders").value(0));
        call(post("/api/v1/job-orders"), consultant, "CONSULTANT",
                "{\"accountId\":\"" + UUID.randomUUID() + "\",\"title\":\"Ghost account\"}")
                .andExpect(status().isNotFound());
    }

    @Test
    void fillingNeedsAPlacedCandidateAndOnlyFromOffer() throws Exception {
        String job = newJob();
        jobStatus(job, "SOURCING", null, consultant, "CONSULTANT", 200, null);
        jobStatus(job, "SHORTLIST_SENT", null, consultant, "CONSULTANT", 200, null);
        jobStatus(job, "INTERVIEWING", null, consultant, "CONSULTANT", 200, null);
        jobStatus(job, "OFFER", null, consultant, "CONSULTANT", 200, null);
        jobStatus(job, "FILLED", null, consultant, "CONSULTANT", 409, "TRANSITION_GUARD_FAILED");
        String app = id(call(post("/api/v1/job-orders/" + job + "/applications"), consultant, "CONSULTANT",
                "{\"personId\":\"" + personA + "\"}").andExpect(status().isCreated()));
        stage(app, "OFFER", null, 200, null);
        stage(app, "PLACED", null, 200, null);
        jobStatus(job, "FILLED", null, consultant, "CONSULTANT", 200, null);
        // closed job order: no new candidates, no stage moves
        call(post("/api/v1/job-orders/" + job + "/applications"), consultant, "CONSULTANT",
                "{\"personId\":\"" + personB + "\"}").andExpect(status().is(409));
    }

    private void stage(String app, String to, String reason, int expected, String code) throws Exception {
        String body = "{\"stage\":\"" + to + "\"" + (reason == null ? "" : ",\"reason\":\"" + reason + "\"") + "}";
        ResultActions r = call(patch("/api/v1/applications/" + app + "/stage"), consultant, "CONSULTANT", body)
                .andExpect(status().is(expected));
        if (code != null) {
            r.andExpect(jsonPath("$.code").value(code));
        }
    }

    @Test
    void pipelineStageRulesAndDelete() throws Exception {
        String job = newJob();
        String app = id(call(post("/api/v1/job-orders/" + job + "/applications"), consultant, "CONSULTANT",
                "{\"personId\":\"" + personA + "\"}").andExpect(status().isCreated())
                .andExpect(jsonPath("$.stage").value("SOURCED")));
        call(post("/api/v1/job-orders/" + job + "/applications"), consultant, "CONSULTANT",
                "{\"personId\":\"" + personA + "\"}").andExpect(status().is(409))
                .andExpect(jsonPath("$.code").value("DUPLICATE"));
        stage(app, "PLACED", null, 409, "ILLEGAL_TRANSITION");
        stage(app, "SOURCED", null, 409, "ILLEGAL_TRANSITION");
        stage(app, "REJECTED", null, 400, "REASON_REQUIRED");
        stage(app, "PRESENTED", null, 200, null);
        stage(app, "SCREENED", null, 200, null);
        stage(app, "REJECTED", "Salary expectations too high", 200, null);
        call(get("/api/v1/applications/" + app), consultant, "CONSULTANT", null)
                .andExpect(jsonPath("$.rejectionReason").value("Salary expectations too high"));
        stage(app, "SOURCED", null, 200, null);
        call(get("/api/v1/applications/" + app), consultant, "CONSULTANT", null)
                .andExpect(jsonPath("$.rejectionReason").doesNotExist());
        // delete only while SOURCED
        stage(app, "SCREENED", null, 200, null);
        call(delete("/api/v1/applications/" + app), consultant, "CONSULTANT", null).andExpect(status().is(409));
        stage(app, "SOURCED", null, 200, null);
        call(delete("/api/v1/applications/" + app), consultant, "CONSULTANT", null)
                .andExpect(status().isNoContent());
        call(get("/api/v1/applications/" + app), consultant, "CONSULTANT", null).andExpect(status().isNotFound());
    }

    @Test
    void proposalStatusRulesAndEditLock() throws Exception {
        String p = id(call(post("/api/v1/proposals"), consultant, "CONSULTANT",
                "{\"accountId\":\"" + accountId + "\",\"title\":\"Retained search\",\"feeAmount\":\"1000.50\","
                        + "\"currency\":\"TRY\"}").andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT")));
        proposalStatus(p, "ACCEPTED", null, 409, "ILLEGAL_TRANSITION");
        call(patch("/api/v1/proposals/" + p), consultant, "CONSULTANT", "{\"terms\":\"12 months\"}")
                .andExpect(status().isOk());
        proposalStatus(p, "SENT", null, 200, null);
        call(patch("/api/v1/proposals/" + p), consultant, "CONSULTANT", "{\"terms\":\"changed\"}")
                .andExpect(status().is(409)).andExpect(jsonPath("$.code").value("STATE_CONFLICT"));
        proposalStatus(p, "REJECTED", null, 400, "REASON_REQUIRED");
        proposalStatus(p, "REJECTED", "Too expensive for them", 200, null);
        proposalStatus(p, "DRAFT", null, 200, null);
        call(get("/api/v1/proposals").param("accountId", accountId.toString()), consultant, "CONSULTANT", null)
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    private void proposalStatus(String p, String to, String reason, int expected, String code) throws Exception {
        String body = "{\"status\":\"" + to + "\"" + (reason == null ? "" : ",\"reason\":\"" + reason + "\"") + "}";
        ResultActions r = call(patch("/api/v1/proposals/" + p + "/status"), consultant, "CONSULTANT", body)
                .andExpect(status().is(expected));
        if (code != null) {
            r.andExpect(jsonPath("$.code").value(code));
        }
    }

    @Test
    void activitiesLogCorrectAndDeleteByAuthorOrManager() throws Exception {
        call(post("/api/v1/activities"), consultant, "CONSULTANT", "{\"type\":\"CALL\",\"note\":\"x\"}")
                .andExpect(status().isBadRequest());
        call(post("/api/v1/activities"), consultant, "CONSULTANT",
                "{\"type\":\"CALL\",\"note\":\"Called HR\",\"accountId\":\"" + accountId
                        + "\",\"occurredAt\":\"2999-01-01T00:00:00Z\"}").andExpect(status().isBadRequest());
        String a = id(call(post("/api/v1/activities"), consultant, "CONSULTANT",
                "{\"type\":\"MEETING\",\"note\":\"Kick-off\",\"accountId\":\"" + accountId + "\",\"personId\":\""
                        + personA + "\"}").andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.id").value(consultant.toString())));
        call(patch("/api/v1/activities/" + a), otherConsultant, "CONSULTANT", "{\"note\":\"hijack\"}")
                .andExpect(status().isForbidden());
        call(patch("/api/v1/activities/" + a), consultant, "CONSULTANT", "{\"note\":\"Kick-off done\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.note").value("Kick-off done"));
        call(get("/api/v1/activities").param("accountId", accountId.toString()).param("type", "MEETING"),
                consultant, "CONSULTANT", null).andExpect(jsonPath("$.content.length()").value(1));
        call(delete("/api/v1/activities/" + a), otherConsultant, "CONSULTANT", null)
                .andExpect(status().isForbidden());
        call(delete("/api/v1/activities/" + a), manager, "MANAGER", null).andExpect(status().isNoContent());
        call(get("/api/v1/activities/" + a), consultant, "CONSULTANT", null).andExpect(status().isNotFound());
    }

    @Test
    void nextActionsLifecycleAndMyDay() throws Exception {
        String n = id(call(post("/api/v1/next-actions"), consultant, "CONSULTANT",
                "{\"title\":\"Send shortlist\",\"dueOn\":\"" + LocalDate.now().minusDays(2)
                        + "\",\"accountId\":\"" + accountId + "\"}").andExpect(status().isCreated())
                .andExpect(jsonPath("$.owner.id").value(consultant.toString()))
                .andExpect(jsonPath("$.overdue").value(true)));
        call(get("/api/v1/home"), consultant, "CONSULTANT", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.overdueCount").value(1))
                .andExpect(jsonPath("$.nextActions[0].id").value(n))
                .andExpect(jsonPath("$.stats.activeAccounts").isNumber());
        call(get("/api/v1/accounts/" + accountId + "/dashboard"), consultant, "CONSULTANT", null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.nextAction.id").value(n))
                .andExpect(jsonPath("$.candidatesPerStage.SOURCED").value(0));
        call(patch("/api/v1/next-actions/" + n), consultant, "CONSULTANT", "{\"done\":true}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.done").value(true))
                .andExpect(jsonPath("$.overdue").value(false)).andExpect(jsonPath("$.doneAt").isNotEmpty());
        call(get("/api/v1/next-actions").param("mine", "true").param("done", "false"), consultant, "CONSULTANT",
                null).andExpect(jsonPath("$.content.length()").value(0));
        call(patch("/api/v1/next-actions/" + n), consultant, "CONSULTANT", "{\"done\":false}")
                .andExpect(jsonPath("$.doneAt").doesNotExist());
        call(delete("/api/v1/next-actions/" + n), consultant, "CONSULTANT", null).andExpect(status().isNoContent());
    }

    @Test
    void everyChangeAppearsInTheTimeline() throws Exception {
        String job = newJob();
        jobStatus(job, "SOURCING", null, consultant, "CONSULTANT", 200, null);
        String app = id(call(post("/api/v1/job-orders/" + job + "/applications"), consultant, "CONSULTANT",
                "{\"personId\":\"" + personA + "\"}").andExpect(status().isCreated()));
        stage(app, "SCREENED", null, 200, null);
        String p = id(call(post("/api/v1/proposals"), consultant, "CONSULTANT",
                "{\"accountId\":\"" + accountId + "\",\"title\":\"T\"}").andExpect(status().isCreated()));
        proposalStatus(p, "SENT", null, 200, null);
        call(post("/api/v1/activities"), consultant, "CONSULTANT",
                "{\"type\":\"CALL\",\"note\":\"Call\",\"accountId\":\"" + accountId + "\"}")
                .andExpect(status().isCreated());
        // job created + moved, candidate added + moved, proposal created + moved, activity = 7 entries
        await().atMost(java.time.Duration.ofSeconds(20)).untilAsserted(() -> {
            String json = call(get("/api/v1/accounts/" + accountId + "/timeline"), consultant, "CONSULTANT", null)
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat((Integer) JsonPath.read(json, "$.page.totalElements")).isEqualTo(7);
        });
        call(get("/api/v1/accounts/" + accountId + "/timeline").param("kind", "CANDIDATE"), consultant,
                "CONSULTANT", null).andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].toStatus").value("SCREENED"))
                .andExpect(jsonPath("$.content[0].fromStatus").value("SOURCED"));
    }

    @Test
    void retentionFlagCreatesFollowUpAndAnonymisationScrubsText() throws Exception {
        String a = id(call(post("/api/v1/activities"), consultant, "CONSULTANT",
                "{\"type\":\"CALL\",\"note\":\"Personal details\",\"accountId\":\"" + accountId
                        + "\",\"personId\":\"" + personA + "\"}").andExpect(status().isCreated()));
        UUID eventId = UUID.randomUUID();
        LocalDate until = LocalDate.now().plusDays(30);
        tx.executeWithoutResult(s -> {
            publisher.publishEvent(new RetentionFlagged(eventId, Instant.now(), null, personA, consultant, until));
            publisher.publishEvent(new PersonAnonymised(UUID.randomUUID(), Instant.now(), null, personA, false));
        });
        await().atMost(java.time.Duration.ofSeconds(20)).untilAsserted(
                () -> assertThat(nextActions.existsBySourceEventId(eventId)).isTrue());
        await().atMost(java.time.Duration.ofSeconds(20)).untilAsserted(() -> call(
                get("/api/v1/activities/" + a), consultant, "CONSULTANT", null)
                .andExpect(jsonPath("$.note").value("[removed]")));
    }

    @Test
    void anonymousAndWrongRoleAreRejected() throws Exception {
        mvc.perform(get("/api/v1/home")).andExpect(status().is4xxClientError());
        mvc.perform(get("/api/v1/home").with(as(consultant, "NOBODY")))
                .andExpect(status().isForbidden());
    }

    @Test
    void personExportIncludesActivitiesAndApplications() throws Exception {
        String job = newJob();
        call(post("/api/v1/job-orders/" + job + "/applications"), consultant, "CONSULTANT",
                "{\"personId\":\"" + personA + "\"}").andExpect(status().isCreated());
        call(post("/api/v1/activities"), consultant, "CONSULTANT",
                "{\"type\":\"CALL\",\"note\":\"Export me\",\"personId\":\"" + personA + "\"}")
                .andExpect(status().isCreated());
        call(get("/api/v1/people/" + personA + "/export"), manager, "MANAGER", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activities.length()").value(1))
                .andExpect(jsonPath("$.activities[0].note").value("Export me"))
                .andExpect(jsonPath("$.applications.length()").value(1))
                .andExpect(jsonPath("$.applications[0].jobOrder.id").value(job));
        call(get("/api/v1/people/" + personB + "/export"), manager, "MANAGER", null)
                .andExpect(jsonPath("$.activities.length()").value(0))
                .andExpect(jsonPath("$.applications.length()").value(0));
    }
}
