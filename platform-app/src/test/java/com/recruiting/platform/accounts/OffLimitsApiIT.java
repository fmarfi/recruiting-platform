package com.recruiting.platform.accounts;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruiting.platform.contracts.Role;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

/** Wave 2b slice 4: /off-limits CRUD (MANAGER/ADMIN write, all read), person.offLimits and OffLimitsApi. */
class OffLimitsApiIT extends AccountsApiTestBase {

    @Autowired
    OffLimitsApi api;

    private Client manager;
    private Client consultant;

    @BeforeEach
    void logIn() throws Exception {
        manager = login(Role.MANAGER);
        consultant = login(Role.CONSULTANT);
    }

    private static List<String> ids(JsonNode page) {
        List<String> out = new ArrayList<>();
        page.get("content").forEach(n -> out.add(n.get("id").asText()));
        return out;
    }

    @Test
    void permissionsAndValidation() throws Exception {
        String company = unique("Rakip ");
        String body = "{\"companyName\":\"" + company + "\",\"reason\":\"CLIENT_AGREEMENT\"}";
        post(consultant, "/api/v1/off-limits", body, 403);
        JsonNode rule = post(manager, "/api/v1/off-limits", body, 201);
        String id = rule.get("id").asText();
        assertThat(rule.get("active").asBoolean()).isTrue();
        assertThat(rule.get("companyName").asText()).isEqualTo(company);
        assertThat(rule.get("account").isNull()).isTrue();
        assertThat(rule.get("startsOn").asText()).isEqualTo(LocalDate.now(java.time.ZoneId.of("Europe/Istanbul")).toString());
        assertThat(rule.get("createdBy").get("id").asText()).isEqualTo(manager.userId().toString());

        // everybody reads
        assertThat(get(consultant, "/api/v1/off-limits/" + id, 200).get("id").asText()).isEqualTo(id);
        assertThat(ids(get(consultant, "/api/v1/off-limits?size=100", 200))).contains(id);
        get(consultant, "/api/v1/off-limits/00000000-0000-7000-8000-000000000001", 404);

        // exactly one target, dates in order, known account, no overlap
        post(manager, "/api/v1/off-limits", "{\"reason\":\"OTHER\"}", 400);
        String accountId = post(manager, "/api/v1/accounts", "{\"name\":\"" + unique("Hesap ")
                + "\",\"sector\":\"Enerji\"}", 201).get("id").asText();
        post(manager, "/api/v1/off-limits", "{\"accountId\":\"" + accountId + "\",\"companyName\":\"X\",\"reason\":\"OTHER\"}", 400);
        post(manager, "/api/v1/off-limits", "{\"companyName\":\"" + unique("Y ") + "\",\"reason\":\"OTHER\","
                + "\"startsOn\":\"2026-05-01\",\"endsOn\":\"2026-04-01\"}", 400);
        post(manager, "/api/v1/off-limits", "{\"accountId\":\"00000000-0000-7000-8000-000000000001\",\"reason\":\"OTHER\"}", 400);
        post(manager, "/api/v1/off-limits", "{\"companyName\":\"" + unique("Z ") + "\",\"reason\":\"FAKE\"}", 400);
        JsonNode dup = post(manager, "/api/v1/off-limits", "{\"companyName\":\"" + company.toUpperCase()
                + "\",\"reason\":\"OTHER\"}", 409);
        assertThat(dup.get("existingId").asText()).isEqualTo(id);

        // PATCH: managers only, version checked, empty body refused, end the rule
        patch(consultant, "/api/v1/off-limits/" + id, "{\"note\":\"x\"}", 403);
        patch(manager, "/api/v1/off-limits/" + id, "{}", 400);
        patch(manager, "/api/v1/off-limits/" + id, "{\"note\":\"x\",\"version\":99}", 409);
        JsonNode noted = patch(manager, "/api/v1/off-limits/" + id, "{\"note\":\"Rahmetli sözleşme\",\"version\":"
                + rule.get("version").asLong() + "}", 200);
        assertThat(noted.get("note").asText()).isEqualTo("Rahmetli sözleşme");
        patch(manager, "/api/v1/off-limits/" + id, "{\"endsOn\":\"2000-01-01\"}", 400); // before startsOn
        JsonNode ended = patch(manager, "/api/v1/off-limits/" + id, "{\"startsOn\":\"2020-01-01\",\"endsOn\":\"2020-12-31\"}", 200);
        assertThat(ended.get("active").asBoolean()).isFalse();
        // an ended rule does not block a new one
        post(manager, "/api/v1/off-limits", body, 201);
    }

    @Test
    void listFilters() throws Exception {
        String marker = unique("Güneş ");
        String open = post(manager, "/api/v1/off-limits", "{\"companyName\":\"" + marker
                + " Enerji\",\"reason\":\"GUARANTEE_PERIOD\"}", 201).get("id").asText();
        String ended = post(manager, "/api/v1/off-limits", "{\"companyName\":\"" + marker
                + " Gıda\",\"reason\":\"OTHER\",\"startsOn\":\"2020-01-01\",\"endsOn\":\"2020-12-31\"}", 201)
                .get("id").asText();
        String future = post(manager, "/api/v1/off-limits", "{\"companyName\":\"" + marker
                + " Tekstil\",\"reason\":\"OTHER\",\"startsOn\":\"2099-01-01\"}", 201).get("id").asText();
        String q = marker.toUpperCase(java.util.Locale.ROOT).replace("Ü", "U").replace("Ş", "S");
        assertThat(ids(get(manager, "/api/v1/off-limits?size=100&q=" + q, 200))).containsExactlyInAnyOrder(open, ended, future);
        assertThat(ids(get(manager, "/api/v1/off-limits?size=100&active=true&q=" + marker, 200))).containsExactly(open);
        assertThat(ids(get(manager, "/api/v1/off-limits?size=100&active=false&q=" + marker, 200)))
                .containsExactlyInAnyOrder(ended, future);
        get(manager, "/api/v1/off-limits?sort=bogus", 400);
        get(manager, "/api/v1/off-limits?active=maybe", 400);
    }

    @Test
    void personOffLimitsAndTheApiForPipeline() throws Exception {
        String slug = unique("Anadolu ");
        String accountName = slug + " Lojistik";
        String accountId = post(manager, "/api/v1/accounts", "{\"name\":\"" + accountName + "\",\"sector\":\"Lojistik\"}",
                201).get("id").asText();
        JsonNode accountRule = post(manager, "/api/v1/off-limits", "{\"accountId\":\"" + accountId
                + "\",\"reason\":\"CLIENT_REQUEST\"}", 201);
        assertThat(accountRule.get("account").get("name").asText()).isEqualTo(accountName);

        // current company equals the account name (accent- and case-insensitive)
        JsonNode p = createPerson(consultant, unique("Aday "), ",\"company\":\"" + accountName.toUpperCase() + "\"");
        String pid = p.get("id").asText();
        JsonNode detail = get(consultant, "/api/v1/people/" + pid, 200);
        assertThat(detail.get("offLimits")).hasSize(1);
        assertThat(detail.get("offLimits").get(0).get("id").asText()).isEqualTo(accountRule.get("id").asText());
        assertThat(api.findForPerson(UUID.fromString(pid), null)).hasSize(1);
        assertThat(api.isBlocked(UUID.fromString(pid), null)).isTrue();
        // the job order's own account is ignored
        assertThat(api.findForPerson(UUID.fromString(pid), UUID.fromString(accountId))).isEmpty();
        assertThat(api.findForCompany(accountName, null)).hasSize(1);
        assertThat(api.findForCompany("Başka şirket", null)).isEmpty();

        // an open employment counts, an ended one does not; a free company name works too
        String freeCompany = unique("Çelik Dış Ticaret ");
        post(manager, "/api/v1/off-limits", "{\"companyName\":\"" + freeCompany + "\",\"reason\":\"OTHER\"}", 201);
        String q = createPerson(consultant, unique("Çalışan "), ",\"company\":\"Başka\",\"employments\":[{\"company\":\""
                + freeCompany.toLowerCase(java.util.Locale.forLanguageTag("tr")) + "\"}]").get("id").asText();
        assertThat(api.findForPerson(UUID.fromString(q), null)).hasSize(1);
        String r = createPerson(consultant, unique("Eski "), ",\"employments\":[{\"company\":\"" + freeCompany
                + "\",\"endDate\":\"2020-01-01\"}]").get("id").asText();
        assertThat(api.findForPerson(UUID.fromString(r), null)).isEmpty();

        // ending the rule lifts the block; anonymised people never match
        patch(manager, "/api/v1/off-limits/" + accountRule.get("id").asText(), "{\"startsOn\":\"2020-01-01\",\"endsOn\":\"2020-12-31\"}", 200);
        assertThat(api.findForPerson(UUID.fromString(pid), null)).isEmpty();
        assertThat(get(consultant, "/api/v1/people/" + pid, 200).get("offLimits")).isEmpty();
        jdbc.update("update person set anonymised_at = now() where id = ?::uuid", q);
        assertThat(api.findForPerson(UUID.fromString(q), null)).isEmpty();
    }
}
