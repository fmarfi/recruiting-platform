package com.recruiting.platform.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.recruiting.platform.accounts.domain.Account;
import com.recruiting.platform.accounts.domain.AccountRepository;
import com.recruiting.platform.contracts.AccountStatus;
import com.recruiting.platform.contracts.BulkOutcome;
import com.recruiting.platform.contracts.BulkResultCode;
import com.recruiting.platform.pipeline.domain.BulkActionItem;
import com.recruiting.platform.pipeline.domain.BulkActionItemRepository;
import com.recruiting.platform.pipeline.domain.BulkActionRepository;
import com.recruiting.platform.pipeline.domain.NextActionRepository;
import com.recruiting.platform.security.IdentifiedUser;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Slice 1 of Wave 2b: {@code POST /bulk-actions} dispatcher with the pipeline handlers (job orders, proposals,
 * SET_NEXT_ACTION): per-record results with the fixed code texts, one transaction per record, audit rows without free
 * text, manager rules and whole-request 400s.
 */
@AutoConfigureMockMvc
class BulkActionApiIT extends AbstractPersistenceIT {

    private record Principal(UUID userId) implements IdentifiedUser {
    }

    @Autowired
    private MockMvc mvc;
    @Autowired
    private AccountRepository accounts;
    @Autowired
    private BulkActionRepository bulkActions;
    @Autowired
    private BulkActionItemRepository bulkItems;
    @Autowired
    private NextActionRepository nextActions;

    private UUID consultant;
    private UUID manager;
    private UUID accountId;

    @BeforeEach
    void setUp() {
        consultant = newUser().getId();
        manager = newUser().getId();
        Account a = new Account();
        a.setName("Bulk Corp " + UUID.randomUUID());
        a.setSector("Retail");
        a.setOwnerId(consultant);
        a.setStatus(AccountStatus.ACTIVE);
        a.setStatusChangedAt(Instant.now());
        a.setOpenedOn(LocalDate.now());
        a.setOpenJobCount(0);
        accountId = tx.execute(s -> accounts.save(a)).getId();
    }

    private RequestPostProcessor as(UUID id, String role) {
        return authentication(new UsernamePasswordAuthenticationToken(new Principal(id), "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private ResultActions call(MockHttpServletRequestBuilder b, UUID who, String role, String body) throws Exception {
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
                "{\"accountId\":\"" + accountId + "\",\"title\":\"Bulk job " + UUID.randomUUID() + "\"}")
                .andExpect(status().isCreated()));
    }

    private String newProposal() throws Exception {
        return id(call(post("/api/v1/proposals"), consultant, "CONSULTANT",
                "{\"accountId\":\"" + accountId + "\",\"title\":\"Bulk proposal\"}").andExpect(status().isCreated()));
    }

    private String body(String entity, String action, List<String> ids, String params) {
        return "{\"entity\":\"" + entity + "\",\"action\":\"" + action + "\",\"ids\":[\""
                + String.join("\",\"", ids) + "\"],\"params\":" + params + "}";
    }

    private String run(String body, UUID who, String role) throws Exception {
        return call(post("/api/v1/bulk-actions"), who, role, body).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString();
    }

    @Test
    void changeStatusGivesAResultPerRecordAndRollsBackOnlyTheFailedOne() throws Exception {
        String ok = newJob();
        String same = newJob();
        String ghost = UUID.randomUUID().toString();
        run(body("JOB_ORDERS", "CHANGE_STATUS", List.of(same), "{\"status\":\"SOURCING\"}"), consultant, "CONSULTANT");
        String json = run(body("JOB_ORDERS", "CHANGE_STATUS", List.of(ok, same, ghost, newJob()),
                "{\"status\":\"SOURCING\"}"), consultant, "CONSULTANT");
        assertThat((Integer) JsonPath.read(json, "$.requested")).isEqualTo(4);
        assertThat((Integer) JsonPath.read(json, "$.ok")).isEqualTo(2);
        assertThat((Integer) JsonPath.read(json, "$.skipped")).isEqualTo(1);
        assertThat((Integer) JsonPath.read(json, "$.failed")).isEqualTo(1);
        assertThat((String) JsonPath.read(json, "$.results[0].outcome")).isEqualTo("OK");
        assertThat((Object) JsonPath.read(json, "$.results[0].code")).isNull();
        assertThat((Object) JsonPath.read(json, "$.results[0].message")).isNull();
        assertThat((String) JsonPath.read(json, "$.results[1].code")).isEqualTo("NO_CHANGE");
        assertThat((String) JsonPath.read(json, "$.results[1].message"))
                .isEqualTo(BulkResultCode.NO_CHANGE.message());
        assertThat((String) JsonPath.read(json, "$.results[2].code")).isEqualTo("NOT_FOUND");
        assertThat((String) JsonPath.read(json, "$.results[2].message"))
                .isEqualTo(BulkResultCode.NOT_FOUND.message());
    }

    @Test
    void illegalMoveGuardsAndManagerOnlyMovesAreFailedPerRecord() throws Exception {
        String job = newJob();
        // OPEN -> FILLED is not in the table
        String json = run(body("JOB_ORDERS", "CHANGE_STATUS", List.of(job), "{\"status\":\"FILLED\"}"), consultant,
                "CONSULTANT");
        assertThat((String) JsonPath.read(json, "$.results[0].code")).isEqualTo("ILLEGAL_TRANSITION");
        // CANCELLED needs a reason
        json = run(body("JOB_ORDERS", "CHANGE_STATUS", List.of(job), "{\"status\":\"CANCELLED\"}"), consultant,
                "CONSULTANT");
        assertThat((String) JsonPath.read(json, "$.results[0].code")).isEqualTo("REASON_REQUIRED");
        json = run(body("JOB_ORDERS", "CHANGE_STATUS", List.of(job),
                "{\"status\":\"CANCELLED\",\"reason\":\"Client cancelled the search\"}"), consultant, "CONSULTANT");
        assertThat((String) JsonPath.read(json, "$.results[0].outcome")).isEqualTo("OK");
        // reopening is manager-only
        json = run(body("JOB_ORDERS", "CHANGE_STATUS", List.of(job), "{\"status\":\"OPEN\"}"), consultant,
                "CONSULTANT");
        assertThat((String) JsonPath.read(json, "$.results[0].code")).isEqualTo("FORBIDDEN");
        json = run(body("JOB_ORDERS", "CHANGE_STATUS", List.of(job), "{\"status\":\"OPEN\"}"), manager, "MANAGER");
        assertThat((String) JsonPath.read(json, "$.results[0].outcome")).isEqualTo("OK");
        // FILLED needs a placed candidate: data guard
        run(body("JOB_ORDERS", "CHANGE_STATUS", List.of(job), "{\"status\":\"SOURCING\"}"), consultant, "CONSULTANT");
        run(body("JOB_ORDERS", "CHANGE_STATUS", List.of(job), "{\"status\":\"SHORTLIST_SENT\"}"), consultant,
                "CONSULTANT");
        run(body("JOB_ORDERS", "CHANGE_STATUS", List.of(job), "{\"status\":\"INTERVIEWING\"}"), consultant,
                "CONSULTANT");
        run(body("JOB_ORDERS", "CHANGE_STATUS", List.of(job), "{\"status\":\"OFFER\"}"), consultant, "CONSULTANT");
        json = run(body("JOB_ORDERS", "CHANGE_STATUS", List.of(job), "{\"status\":\"FILLED\"}"), consultant,
                "CONSULTANT");
        assertThat((String) JsonPath.read(json, "$.results[0].code")).isEqualTo("TRANSITION_GUARD_FAILED");
        call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/job-orders/" + job),
                consultant, "CONSULTANT", null).andExpect(jsonPath("$.status").value("OFFER"));
    }

    @Test
    void proposalsReasonAndTagsAndOwner() throws Exception {
        String p = newProposal();
        String json = run(body("PROPOSALS", "CHANGE_STATUS", List.of(p), "{\"status\":\"SENT\"}"), consultant,
                "CONSULTANT");
        assertThat((String) JsonPath.read(json, "$.results[0].outcome")).isEqualTo("OK");
        json = run(body("PROPOSALS", "CHANGE_STATUS", List.of(p), "{\"status\":\"REJECTED\"}"), consultant,
                "CONSULTANT");
        assertThat((String) JsonPath.read(json, "$.results[0].code")).isEqualTo("REASON_REQUIRED");
        json = run(body("PROPOSALS", "ADD_TAG", List.of(p), "{\"tag\":\"x\"}"), consultant,
                "CONSULTANT");
        assertThat((String) JsonPath.read(json, "$.results[0].outcome")).isEqualTo("OK");
        json = run(body("PROPOSALS", "ADD_TAG", List.of(p), "{\"tag\":\"X\"}"), consultant, "CONSULTANT");
        assertThat((String) JsonPath.read(json, "$.results[0].code")).isEqualTo("NO_CHANGE");
        json = run(body("PROPOSALS", "REMOVE_TAG", List.of(p), "{\"tag\":\"x\"}"), consultant, "CONSULTANT");
        assertThat((String) JsonPath.read(json, "$.results[0].outcome")).isEqualTo("OK");
        json = run(body("PROPOSALS", "REMOVE_TAG", List.of(p), "{\"tag\":\"x\"}"), consultant, "CONSULTANT");
        assertThat((String) JsonPath.read(json, "$.results[0].code")).isEqualTo("NO_CHANGE");
        json = run(body("PROPOSALS", "REASSIGN_OWNER", List.of(p), "{\"ownerId\":\"" + manager + "\"}"), consultant,
                "CONSULTANT");
        assertThat((String) JsonPath.read(json, "$.results[0].outcome")).isEqualTo("OK");
        json = run(body("PROPOSALS", "REASSIGN_OWNER", List.of(p), "{\"ownerId\":\"" + manager + "\"}"), consultant,
                "CONSULTANT");
        assertThat((String) JsonPath.read(json, "$.results[0].code")).isEqualTo("NO_CHANGE");
    }

    @Test
    void tooManyTagsFailsOnlyThatRecord() throws Exception {
        String full = newJob();
        String empty = newJob();
        for (int i = 0; i < 20; i++) {
            run(body("JOB_ORDERS", "ADD_TAG", List.of(full), "{\"tag\":\"t" + i + "\"}"), consultant, "CONSULTANT");
        }
        String json = run(body("JOB_ORDERS", "ADD_TAG", List.of(full, empty), "{\"tag\":\"extra\"}"), consultant,
                "CONSULTANT");
        assertThat((String) JsonPath.read(json, "$.results[0].code")).isEqualTo("TOO_MANY_TAGS");
        assertThat((String) JsonPath.read(json, "$.results[1].outcome")).isEqualTo("OK");
    }

    @Test
    void setNextActionCreatesOnePerRecordAndNeverStoresTheTitleInTheAudit() throws Exception {
        String job = newJob();
        String title = "Call Mehmet Yilmaz about " + UUID.randomUUID();
        String due = LocalDate.now().plusDays(3).toString();
        String json = run(body("JOB_ORDERS", "SET_NEXT_ACTION", List.of(job),
                "{\"title\":\"" + title + "\",\"dueOn\":\"" + due + "\"}"), consultant, "CONSULTANT");
        assertThat((String) JsonPath.read(json, "$.results[0].outcome")).isEqualTo("OK");
        json = run(body("ACCOUNTS", "SET_NEXT_ACTION", List.of(accountId.toString()),
                "{\"title\":\"" + title + "\",\"dueOn\":\"" + due + "\",\"ownerId\":\"" + manager + "\"}"),
                consultant, "CONSULTANT");
        assertThat((String) JsonPath.read(json, "$.results[0].outcome")).isEqualTo("OK");
        var created = nextActions.findAll().stream().filter(n -> title.equals(n.getTitle())).toList();
        assertThat(created).hasSize(2);
        assertThat(created).anyMatch(n -> manager.equals(n.getOwnerId()) && accountId.equals(n.getAccountId()));
        assertThat(created).anyMatch(n -> consultant.equals(n.getOwnerId()) && job.equals(String.valueOf(n.getJobOrderId())));
        // the audit rows hold no free text
        UUID auditId = UUID.fromString(JsonPath.read(json, "$.id"));
        var header = bulkActions.findById(auditId).orElseThrow();
        assertThat(header.getParams()).doesNotContainKeys("title", "reason").containsKeys("dueOn", "ownerId");
        assertThat(header.getRequestedCount()).isEqualTo(1);
        assertThat(header.getOkCount()).isEqualTo(1);
        assertThat(header.getFinishedAt()).isNotNull();
        assertThat(header.getActorId()).isEqualTo(consultant);
    }

    @Test
    void closedAccountAndUnknownPersonAreFailedPerRecord() throws Exception {
        String due = LocalDate.now().plusDays(1).toString();
        String ghost = UUID.randomUUID().toString();
        String json = run(body("PEOPLE", "SET_NEXT_ACTION", List.of(ghost),
                "{\"title\":\"Follow up\",\"dueOn\":\"" + due + "\"}"), consultant, "CONSULTANT");
        assertThat((String) JsonPath.read(json, "$.results[0].code")).isEqualTo("NOT_FOUND");
        Account closed = new Account();
        closed.setName("Closed Corp " + UUID.randomUUID());
        closed.setSector("Retail");
        closed.setOwnerId(consultant);
        closed.setStatus(AccountStatus.CLOSED);
        closed.setStatusChangedAt(Instant.now());
        closed.setOpenedOn(LocalDate.now());
        closed.setOpenJobCount(0);
        UUID closedId = tx.execute(s -> accounts.save(closed)).getId();
        json = run(body("ACCOUNTS", "SET_NEXT_ACTION", List.of(closedId.toString()),
                "{\"title\":\"Follow up\",\"dueOn\":\"" + due + "\"}"), consultant, "CONSULTANT");
        assertThat((String) JsonPath.read(json, "$.results[0].code")).isEqualTo("STATE_CONFLICT");
    }

    @Test
    void everyRequestedIdHasAnAuditItemEvenWhenSkippedOrFailed() throws Exception {
        String a = newJob();
        String ghost = UUID.randomUUID().toString();
        String json = run(body("JOB_ORDERS", "ADD_TAG", List.of(a, ghost), "{\"tag\":\"audit\"}"), consultant,
                "CONSULTANT");
        UUID auditId = UUID.fromString(JsonPath.read(json, "$.id"));
        List<BulkActionItem> items = bulkItems.findByBulkActionId(auditId);
        assertThat(items).hasSize(2);
        assertThat(items).anyMatch(i -> i.getEntityId().toString().equals(a) && i.getOutcome() == BulkOutcome.OK
                && i.getCode() == null);
        assertThat(items).anyMatch(i -> i.getEntityId().toString().equals(ghost)
                && i.getOutcome() == BulkOutcome.FAILED && i.getCode() == BulkResultCode.NOT_FOUND);
        assertThat(bulkActions.findById(auditId).orElseThrow().getFailedCount()).isEqualTo(1);
    }

    @Test
    void requestConsentIsManagerOnlyAndBadRequestsAre400() throws Exception {
        String id = UUID.randomUUID().toString();
        call(post("/api/v1/bulk-actions"), consultant, "CONSULTANT", body("PEOPLE", "REQUEST_CONSENT", List.of(id), "{}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        // entity/action matrix
        call(post("/api/v1/bulk-actions"), consultant, "CONSULTANT",
                body("PEOPLE", "CHANGE_STATUS", List.of(id), "{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isBadRequest());
        call(post("/api/v1/bulk-actions"), consultant, "CONSULTANT",
                body("PROPOSALS", "SET_NEXT_ACTION", List.of(id), "{\"title\":\"x\",\"dueOn\":\"2030-01-01\"}"))
                .andExpect(status().isBadRequest());
        // status of another lifecycle, unknown param, reason too short, unknown owner, duplicate ids, empty ids
        call(post("/api/v1/bulk-actions"), consultant, "CONSULTANT",
                body("JOB_ORDERS", "CHANGE_STATUS", List.of(id), "{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isBadRequest());
        call(post("/api/v1/bulk-actions"), consultant, "CONSULTANT",
                body("JOB_ORDERS", "ADD_TAG", List.of(id), "{\"tag\":\"a\",\"extra\":1}"))
                .andExpect(status().isBadRequest());
        call(post("/api/v1/bulk-actions"), consultant, "CONSULTANT",
                body("JOB_ORDERS", "CHANGE_STATUS", List.of(id), "{\"status\":\"CANCELLED\",\"reason\":\"ab\"}"))
                .andExpect(status().isBadRequest());
        call(post("/api/v1/bulk-actions"), consultant, "CONSULTANT",
                body("JOB_ORDERS", "REASSIGN_OWNER", List.of(id), "{\"ownerId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest());
        call(post("/api/v1/bulk-actions"), consultant, "CONSULTANT",
                body("JOB_ORDERS", "ADD_TAG", List.of(id, id), "{\"tag\":\"a\"}")).andExpect(status().isBadRequest());
        call(post("/api/v1/bulk-actions"), consultant, "CONSULTANT",
                "{\"entity\":\"JOB_ORDERS\",\"action\":\"ADD_TAG\",\"ids\":[],\"params\":{\"tag\":\"a\"}}")
                .andExpect(status().isBadRequest());
        List<String> many = java.util.stream.IntStream.range(0, 501).mapToObj(i -> UUID.randomUUID().toString())
                .toList();
        call(post("/api/v1/bulk-actions"), consultant, "CONSULTANT",
                body("JOB_ORDERS", "ADD_TAG", many, "{\"tag\":\"a\"}")).andExpect(status().isBadRequest());
        call(patch("/api/v1/bulk-actions"), consultant, "CONSULTANT", "{}").andExpect(status().is4xxClientError());
    }
}
