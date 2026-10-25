package com.recruiting.platform.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.recruiting.platform.api.AbstractApiIT;
import com.recruiting.platform.contracts.Role;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** /views on a real PostgreSQL: permissions, uniqueness, shared visibility, versions (ADR 0010). */
class SavedViewApiIT extends AbstractApiIT {

    private static final String DEF = "{\"filters\":{\"status\":[\"ACTIVE\"],\"ownerId\":\"me\"},"
            + "\"columns\":[\"name\",\"status\"],\"sort\":[\"name,asc\"],\"groupBy\":null}";

    private final JsonMapper json = JsonMapper.builder().build();
    private Client manager;
    private Client consultant;
    private Client otherConsultant;

    @BeforeEach
    void logIn() throws Exception {
        manager = login(Role.MANAGER);
        consultant = login(Role.CONSULTANT);
        otherConsultant = login(Role.CONSULTANT);
    }

    private JsonNode send(Client c, AbstractMockHttpServletRequestBuilder<?> b, int expected) throws Exception {
        String body = mvc.perform(b.session(c.session())
                .cookie(new jakarta.servlet.http.Cookie("XSRF-TOKEN", c.csrf())).header("X-XSRF-TOKEN", c.csrf())).andExpect(status().is(expected)).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return body.isBlank() ? null : json.readTree(body);
    }

    private JsonNode create(Client c, String entity, String name, String visibility, int expected) throws Exception {
        String vis = visibility == null ? "" : ",\"visibility\":\"" + visibility + "\"";
        return send(c, post("/api/v1/views").contentType(MediaType.APPLICATION_JSON).content(
                "{\"entity\":\"" + entity + "\",\"name\":\"" + name + "\"" + vis + ",\"definition\":" + DEF + "}"),
                expected);
    }

    private JsonNode patchView(Client c, String id, String body, int expected) throws Exception {
        return send(c, patch("/api/v1/views/" + id).contentType(MediaType.APPLICATION_JSON).content(body), expected);
    }

    private static String unique(String p) {
        return p + UUID.randomUUID().toString().substring(0, 8);
    }

    private boolean listed(Client c, String entity, String id) throws Exception {
        JsonNode list = send(c, get("/api/v1/views?entity=" + entity), 200);
        for (JsonNode v : list) {
            if (v.get("id").asText().equals(id)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void privateViewIsVisibleOnlyToItsOwner() throws Exception {
        JsonNode v = create(consultant, "ACCOUNTS", unique("Mine "), null, 201);
        String id = v.get("id").asText();
        assertThat(v.get("visibility").asText()).isEqualTo("PRIVATE");
        assertThat(v.get("editable").asBoolean()).isTrue();
        assertThat(v.get("owner").get("id").asText()).isEqualTo(consultant.userId().toString());
        assertThat(v.get("definition").get("filters").get("ownerId").asText()).isEqualTo("me");
        assertThat(listed(consultant, "ACCOUNTS", id)).isTrue();
        assertThat(listed(otherConsultant, "ACCOUNTS", id)).isFalse();
        assertThat(listed(consultant, "PEOPLE", id)).isFalse();
        patchView(otherConsultant, id, "{\"name\":\"Stolen\"}", 404);
        send(otherConsultant, delete("/api/v1/views/" + id), 404);
        send(manager, delete("/api/v1/views/" + id), 404);
        send(consultant, delete("/api/v1/views/" + id), 204);
        send(consultant, delete("/api/v1/views/" + id), 404);
    }

    @Test
    void sharedViewsNeedManagerAndAreVisibleToEveryone() throws Exception {
        create(consultant, "ACCOUNTS", unique("Shared "), "SHARED", 403);
        JsonNode own = create(consultant, "ACCOUNTS", unique("Own "), null, 201);
        patchView(consultant, own.get("id").asText(), "{\"visibility\":\"SHARED\"}", 403);

        JsonNode shared = create(manager, "ACCOUNTS", unique("Team "), "SHARED", 201);
        String id = shared.get("id").asText();
        assertThat(listed(consultant, "ACCOUNTS", id)).isTrue();
        assertThat(listed(otherConsultant, "ACCOUNTS", id)).isTrue();

        // consultant sees it read-only
        JsonNode seen = null;
        for (JsonNode v : send(consultant, get("/api/v1/views"), 200)) {
            if (v.get("id").asText().equals(id)) {
                seen = v;
            }
        }
        assertThat(seen).isNotNull();
        assertThat(seen.get("editable").asBoolean()).isFalse();
        patchView(consultant, id, "{\"name\":\"Hijack\"}", 403);
        send(consultant, delete("/api/v1/views/" + id), 403);

        // another manager may change and delete it
        Client manager2 = login(Role.MANAGER);
        JsonNode changed = patchView(manager2, id, "{\"name\":\"" + unique("Renamed ") + "\"}", 200);
        assertThat(changed.get("editable").asBoolean()).isTrue();
        assertThat(changed.get("version").asLong()).isGreaterThan(shared.get("version").asLong());
        send(manager2, delete("/api/v1/views/" + id), 204);
        assertThat(listed(consultant, "ACCOUNTS", id)).isFalse();
    }

    @Test
    void nameIsUniquePerOwnerAndEntityIgnoringCase() throws Exception {
        String name = unique("Dup ");
        create(consultant, "ACCOUNTS", name, null, 201);
        JsonNode dup = create(consultant, "ACCOUNTS", name.toUpperCase(), null, 409);
        assertThat(dup.get("code").asText()).isEqualTo("DUPLICATE");
        send(consultant, post("/api/v1/views").contentType(MediaType.APPLICATION_JSON).content("{\"entity\":\"PEOPLE\",\"name\":\"" + name + "\",\"definition\":{\"filters\":{},\"columns\":[],\"sort\":[]}}"), 201);          // other entity
        create(otherConsultant, "ACCOUNTS", name, null, 201);   // other owner
        JsonNode other = create(consultant, "ACCOUNTS", unique("Other "), null, 201);
        patchView(consultant, other.get("id").asText(), "{\"name\":\"" + name + "\"}", 409);
    }

    @Test
    void optimisticVersionAndDefinitionReplacement() throws Exception {
        JsonNode v = create(consultant, "ACCOUNTS", unique("Ver "), null, 201);
        String id = v.get("id").asText();
        long version = v.get("version").asLong();
        JsonNode updated = patchView(consultant, id, "{\"version\":" + version + ",\"definition\":"
                + "{\"filters\":{\"q\":\"energy\"},\"columns\":[\"name\"],\"sort\":[],\"groupBy\":\"sector\"}}", 200);
        assertThat(updated.get("definition").get("groupBy").asText()).isEqualTo("sector");
        assertThat(updated.get("definition").get("filters").has("ownerId")).isFalse();
        JsonNode conflict = patchView(consultant, id, "{\"version\":" + version + ",\"name\":\"X\"}", 409);
        assertThat(conflict.get("code").asText()).isEqualTo("VERSION_CONFLICT");
    }

    @Test
    void invalidRequestsGive400() throws Exception {
        JsonNode bad = send(consultant, post("/api/v1/views").contentType(MediaType.APPLICATION_JSON).content(
                "{\"entity\":\"ACCOUNTS\",\"name\":\"" + unique("Bad ") + "\",\"definition\":{\"filters\":"
                        + "{\"bogus\":1},\"columns\":[],\"sort\":[]}}"), 400);
        assertThat(bad.get("errors").get(0).get("field").asText()).isEqualTo("definition.filters.bogus");
        send(consultant, post("/api/v1/views").contentType(MediaType.APPLICATION_JSON).content(
                "{\"entity\":\"ACCOUNTS\",\"name\":\"\",\"definition\":" + DEF + "}"), 400);
        send(consultant, post("/api/v1/views").contentType(MediaType.APPLICATION_JSON).content(
                "{\"entity\":\"NOPE\",\"name\":\"x\",\"definition\":" + DEF + "}"), 400);
        JsonNode v = create(consultant, "ACCOUNTS", unique("Ok "), null, 201);
        patchView(consultant, v.get("id").asText(), "{}", 400);
        patchView(consultant, v.get("id").asText(), "{\"definition\":{\"filters\":{\"status\":\"NOPE\"},"
                + "\"columns\":[],\"sort\":[]}}", 400);
    }

    @Test
    void anonymousIsRejected() throws Exception {
        mvc.perform(get("/api/v1/views")).andExpect(status().isUnauthorized());
    }
}
