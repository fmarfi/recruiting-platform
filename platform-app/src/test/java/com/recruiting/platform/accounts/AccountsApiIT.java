package com.recruiting.platform.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.recruiting.platform.api.AbstractApiIT;
import com.recruiting.platform.contracts.Role;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * End-to-end test of the accounts endpoints (W1-10) on a real PostgreSQL: reference data, accounts and status,
 * people, contacts, network links, search, the CSV import, the person data export and PersonDataApi. Users log in
 * through the real filter chain, so method security (the MANAGER-only import rule) is enforced.
 */
class AccountsApiIT extends AbstractApiIT {

    @Autowired
    PersonDataApi personData;
    @Autowired
    org.springframework.jdbc.core.JdbcTemplate jdbc;

    private final JsonMapper json = JsonMapper.builder().build();
    private Client manager;
    private Client consultant;
    private String userEmail;

    @BeforeEach
    void logIn() throws Exception {
        var user = createUser(Role.MANAGER);
        userEmail = user.getEmail();
        manager = login(user);
        consultant = login(Role.CONSULTANT);
    }

    private static AbstractMockHttpServletRequestBuilder<?> as(Client client, AbstractMockHttpServletRequestBuilder<?> b) {
        return b.session(client.session()).cookie(new jakarta.servlet.http.Cookie("XSRF-TOKEN", client.csrf()))
                .header("X-XSRF-TOKEN", client.csrf());
    }

    private JsonNode send(AbstractMockHttpServletRequestBuilder<?> b, int expected) throws Exception {
        return send(manager, b, expected);
    }

    private JsonNode send(Client client, AbstractMockHttpServletRequestBuilder<?> b, int expected) throws Exception {
        String body = mvc.perform(as(client, b)).andExpect(status().is(expected)).andReturn()
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
        mvc.perform(as(consultant, get("/api/v1/reference-data")))
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
        send(consultant, get("/api/v1/imports"), 403);
        send(consultant, get("/api/v1/import-templates/PEOPLE"), 403);
        send(consultant, multipart("/api/v1/imports").file(file).param("kind", "ACCOUNTS"), 403);
        send(consultant, post("/api/v1/imports/" + id + "/commit"), 403);
        send(consultant, delete("/api/v1/imports/" + id), 403);
        assertThat(mvc.perform(as(manager, get("/api/v1/import-templates/PEOPLE"))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).startsWith("name,type,");
    }

    @Test
    void exportIsManagerOnlyAndLoggedAndAnonymiseAndDeleteWork() throws Exception {
        String email = unique("e") + "@example.test";
        JsonNode created = postJson("/api/v1/people",
                "{\"name\":\"Deniz Kara\",\"type\":\"EXECUTIVE\",\"email\":\"" + email + "\",\"notes\":\"private\"}", 201);
        UUID id = UUID.fromString(created.get("id").asText());
        String other = postJson("/api/v1/people",
                "{\"name\":\"Mert Ak\",\"type\":\"EXECUTIVE\"}", 201).get("id").asText();
        postJson("/api/v1/network-links", "{\"fromPersonId\":\"" + id + "\",\"toPersonId\":\"" + other
                + "\",\"type\":\"KNOWS\"}", 201);

        send(consultant, get("/api/v1/people/" + id + "/export"), 403);
        mvc.perform(get("/api/v1/people/" + id + "/export")).andExpect(status().isUnauthorized());
        send(get("/api/v1/people/" + UUID.randomUUID() + "/export"), 404);
        var res = mvc.perform(as(manager, get("/api/v1/people/" + id + "/export"))).andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Content-Disposition", "attachment; filename=\"person-" + id + ".json\""))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode export = json.readTree(res);
        assertThat(export.get("person").get("email").asText()).isEqualTo(email);
        assertThat(export.get("exportedBy").get("id").asText()).isEqualTo(manager.userId().toString());
        assertThat(export.get("links")).hasSize(1);
        assertThat(export.get("activities")).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from access_log where entity_id = ? and action = 'EXPORT'",
                Long.class, id)).isEqualTo(1L);

        personData.anonymise(id, manager.userId());
        JsonNode anon = send(get("/api/v1/people/" + id), 200);
        assertThat(anon.get("name").asText()).isEqualTo("Anonymised person");
        assertThat(anon.get("email").isNull()).isTrue();
        assertThat(send(get("/api/v1/people/" + other + "/links"), 200)).isEmpty();

        personData.delete(id, manager.userId());
        send(get("/api/v1/people/" + id), 404);
    }
}
