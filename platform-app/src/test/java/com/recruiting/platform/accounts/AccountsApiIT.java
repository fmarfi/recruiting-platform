package com.recruiting.platform.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.security.domain.AppUser;
import com.recruiting.platform.security.domain.AppUserRepository;
import com.recruiting.platform.contracts.Language;
import com.recruiting.platform.contracts.Role;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * End-to-end test of the accounts endpoints (W1-10) on a real PostgreSQL: reference data, accounts and status,
 * people, contacts, network links, search and the CSV import. Security is the default chain with a mock user whose
 * name is the user id (the real SecurityFilterChain comes from security-privacy).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({AccountsApiIT.Postgres.class, AccountsApiIT.ProblemAdvice.class})
class AccountsApiIT {

    @TestConfiguration(proxyBeanMethods = false)
    static class Postgres {
        @Bean
        @ServiceConnection
        PostgreSQLContainer postgres() {
            return new PostgreSQLContainer(
                    DockerImageName.parse("pgvector/pgvector:0.8.7-pg17").asCompatibleSubstituteFor("postgres"));
        }
    }

    /** Stand-in for the platform-wide problem handler (ADR 0006), which lives in platform-app later. */
    @RestControllerAdvice
    static class ProblemAdvice {
        @ExceptionHandler(ApiException.class)
        ResponseEntity<Map<String, Object>> handle(ApiException e) {
            Map<String, Object> body = new java.util.HashMap<>(e.properties());
            body.put("code", e.code().name());
            return ResponseEntity.status(HttpStatus.valueOf(e.code().httpStatus())).body(body);
        }
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    AppUserRepository users;

    private final JsonMapper json = JsonMapper.builder().build();
    private UUID userId;
    private String userEmail;

    @BeforeEach
    void createUser() {
        AppUser u = new AppUser();
        userEmail = "u-" + UUID.randomUUID() + "@example.test";
        u.setEmail(userEmail);
        u.setName("Test Manager");
        u.setRole(Role.MANAGER);
        u.setActive(true);
        u.setLanguage(Language.EN);
        u.setMustChangePassword(false);
        userId = users.saveAndFlush(u).getId();
    }

    private RequestPostProcessor manager() {
        return user(userId.toString()).roles("MANAGER");
    }

    private RequestPostProcessor consultant() {
        return user(userId.toString()).roles("CONSULTANT");
    }

    private org.springframework.test.web.servlet.RequestBuilder asManager(
            org.springframework.test.web.servlet.RequestBuilder b) {
        return ctx -> csrf().postProcessRequest(manager().postProcessRequest(b.buildRequest(ctx)));
    }

    private JsonNode send(org.springframework.test.web.servlet.RequestBuilder b,
            int expected) throws Exception {
        String body = mvc.perform(asManager(b)).andExpect(status().is(expected)).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
        return body.isBlank() ? null : json.readTree(body);
    }

    private JsonNode postJson(String url, String body, int expected) throws Exception {
        return send(post(url).contentType(MediaType.APPLICATION_JSON).content(body), expected);
    }

    private static String unique(String p) {
        return p + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void referenceDataComesFromTheLifecycleEnums() throws Exception {
        mvc.perform(get("/api/v1/reference-data").with(consultant()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lifecycles.account.initial").value("LEAD"))
                .andExpect(jsonPath("$.lifecycles.account.statuses[0].next[0].to").value("PROPOSAL_SENT"))
                .andExpect(jsonPath("$.stageOrder[0]").value("SOURCED"))
                .andExpect(jsonPath("$.vocabularies.linkType[0]").value("KNOWS"));
    }

    @Test
    void accountLifecycleContactsLinksAndSearch() throws Exception {
        String name = unique("Öztürk Holding ");
        JsonNode account = postJson("/api/v1/accounts",
                "{\"name\":\"" + name + "\",\"sector\":\"Energy\",\"city\":\"Ankara\"}", 201);
        String accountId = account.get("id").asText();
        assertThat(account.get("status").asText()).isEqualTo("LEAD");
        postJson("/api/v1/accounts", "{\"name\":\"" + name.toLowerCase() + "\",\"sector\":\"Energy\"}", 409);

        JsonNode moved = send(patch("/api/v1/accounts/" + accountId + "/status")
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PROPOSAL_SENT\"}"), 200);
        assertThat(moved.get("status").asText()).isEqualTo("PROPOSAL_SENT");
        send(patch("/api/v1/accounts/" + accountId + "/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CLOSED\"}"), 400);
        send(patch("/api/v1/accounts/" + accountId + "/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"LEAD\"}"), 200);

        String email = unique("a") + "@example.test";
        JsonNode contact = postJson("/api/v1/accounts/" + accountId + "/contacts",
                "{\"person\":{\"name\":\"Elif Yıldırım\",\"type\":\"CLIENT_CONTACT\",\"email\":\"" + email
                        + "\"},\"role\":\"CFO\",\"primary\":true}", 201);
        String personId = contact.get("person").get("id").asText();
        assertThat(send(get("/api/v1/accounts/" + accountId + "/contacts"), 200)).hasSize(1);
        postJson("/api/v1/accounts/" + accountId + "/contacts", "{\"personId\":\"" + personId + "\"}", 409);

        String otherId = postJson("/api/v1/people",
                "{\"name\":\"Can Aydın\",\"type\":\"EXECUTIVE\",\"email\":\"" + unique("b") + "@example.test\"}", 201)
                .get("id").asText();
        postJson("/api/v1/network-links", "{\"fromPersonId\":\"" + personId + "\",\"toPersonId\":\"" + personId
                + "\",\"type\":\"KNOWS\"}", 400);
        String linkId = postJson("/api/v1/network-links", "{\"fromPersonId\":\"" + personId + "\",\"toPersonId\":\""
                + otherId + "\",\"type\":\"KNOWS\"}", 201).get("id").asText();
        postJson("/api/v1/network-links", "{\"fromPersonId\":\"" + otherId + "\",\"toPersonId\":\"" + personId
                + "\",\"type\":\"KNOWS\"}", 409);
        JsonNode links = send(get("/api/v1/people/" + otherId + "/links"), 200);
        assertThat(links).hasSize(1);
        assertThat(links.get(0).get("direction").asText()).isEqualTo("BOTH");
        assertThat(send(get("/api/v1/accounts/" + accountId + "/connections"), 200)).hasSize(1);
        send(patch("/api/v1/network-links/" + linkId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":\"Dinner 2024\"}"), 200);
        send(delete("/api/v1/network-links/" + linkId), 204);
        send(get("/api/v1/network-links/" + linkId), 404);

        JsonNode found = send(get("/api/v1/search").param("q", "ozturk holding"), 200);
        assertThat(found.get("accounts")).hasSize(1);
        assertThat(found.get("totals").get("accounts").asInt()).isEqualTo(1);
        assertThat(send(get("/api/v1/search").param("q", "yildirim").param("types", "PERSON"), 200)
                .get("people").size()).isGreaterThanOrEqualTo(1);
        send(get("/api/v1/search").param("q", "x"), 400);
    }

    @Test
    void importValidatesThenCommitsAndSkipsDuplicatesAndBadRows() throws Exception {
        String existing = unique("Existing ");
        postJson("/api/v1/accounts", "{\"name\":\"" + existing + "\",\"sector\":\"Retail\"}", 201);
        String fresh = unique("Fresh ");
        String csv = "name,sector,city,website,ownerEmail\n"
                + fresh + ",Finance,Izmir,https://fresh.example.test," + userEmail + "\n"
                + existing + ",Retail,,,\n"
                + ",Retail,,,\n"
                + unique("NoOwner ") + ",Retail,,,nobody@example.test\n";
        MockMultipartFile file = new MockMultipartFile("file", "accounts.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));
        JsonNode job = send(multipart("/api/v1/imports").file(file).param("kind", "ACCOUNTS"), 201);
        assertThat(job.get("status").asText()).isEqualTo("VALIDATED");
        assertThat(job.get("totalRows").asInt()).isEqualTo(4);
        assertThat(job.get("validRows").asInt()).isEqualTo(1);
        assertThat(job.get("duplicateRows").asInt()).isEqualTo(1);
        assertThat(job.get("errors")).hasSize(2);

        String id = job.get("id").asText();
        JsonNode done = send(post("/api/v1/imports/" + id + "/commit"), 200);
        assertThat(done.get("status").asText()).isEqualTo("COMPLETED");
        assertThat(done.get("importedRows").asInt()).isEqualTo(1);
        send(post("/api/v1/imports/" + id + "/commit"), 409);
        send(delete("/api/v1/imports/" + id), 409);
        assertThat(send(get("/api/v1/accounts").param("q", fresh), 200).get("content")).hasSize(1);

        MockMultipartFile bad = new MockMultipartFile("file", "x.csv", "text/csv", "foo,bar\n1,2\n".getBytes());
        assertThat(send(multipart("/api/v1/imports").file(bad).param("kind", "ACCOUNTS"), 201).get("status")
                .asText()).isEqualTo("FAILED");
        MockMultipartFile pdf = new MockMultipartFile("file", "x.pdf", "application/pdf", new byte[] {1});
        send(multipart("/api/v1/imports").file(pdf).param("kind", "ACCOUNTS"), 415);
        mvc.perform(get("/api/v1/imports").with(consultant())).andExpect(status().isOk());
        assertThat(mvc.perform(get("/api/v1/import-templates/PEOPLE").with(manager())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).startsWith("name,type,");
    }
}
