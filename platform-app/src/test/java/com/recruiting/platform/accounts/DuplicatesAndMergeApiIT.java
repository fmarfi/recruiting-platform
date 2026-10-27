package com.recruiting.platform.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.contracts.Role;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;

/** Wave 2b slice 3: duplicate check and queue, and the accounts steps of the audited merge (PersonMergeApi). */
class DuplicatesAndMergeApiIT extends AccountsApiTestBase {

    @Autowired
    PersonMergeApi mergeApi;
    @Autowired
    TransactionTemplate tx;

    private Client consultant;
    private Client manager;

    @BeforeEach
    void logIn() throws Exception {
        consultant = login(Role.CONSULTANT);
        manager = login(Role.MANAGER);
    }

    private static List<String> reasons(JsonNode match) {
        List<String> out = new ArrayList<>();
        match.get("reasons").forEach(r -> out.add(r.asText()));
        return out;
    }

    private static String digits() {
        return String.valueOf(1000000 + (System.nanoTime() % 8999999));
    }

    private JsonNode check(String body, int expected) throws Exception {
        return post(consultant, "/api/v1/duplicates/check", body, expected);
    }

    @Test
    void checkMatchesOnEveryCriterionStrongestFirst() throws Exception {
        String d = digits();
        String slug = "fiktif-" + UUID.randomUUID().toString().substring(0, 8);
        String email = slug + "@example.test";
        String name = "Zeynep Çelik " + slug;
        JsonNode a = createPerson(consultant, name, ",\"email\":\"" + email + "\",\"phone\":\"0555 " + d.substring(0, 3)
                + " " + d.substring(3, 5) + " " + d.substring(5, 7) + "\",\"company\":\"Güneş Enerji " + slug
                + "\",\"linkedinUrl\":\"https://www.linkedin.com/in/" + slug + "\"");
        String id = a.get("id").asText();

        JsonNode byEmail = check("{\"email\":\"" + email.toUpperCase() + "\"}", 200);
        assertThat(byEmail).hasSize(1);
        assertThat(reasons(byEmail.get(0))).containsExactly("EMAIL");
        assertThat(byEmail.get(0).get("person").get("id").asText()).isEqualTo(id);
        JsonNode byPhone = check("{\"phone\":\"+90 555 " + d.substring(0, 3) + " " + d.substring(3) + "\"}", 200);
        assertThat(reasons(byPhone.get(0))).containsExactly("PHONE");
        JsonNode byLinkedin = check("{\"linkedinUrl\":\"tr.linkedin.com/in/" + slug.toUpperCase() + "/?x=1\"}", 200);
        assertThat(reasons(byLinkedin.get(0))).containsExactly("LINKEDIN_URL");
        // name + company, accent- and case-insensitive
        JsonNode byName = check("{\"name\":\"ZEYNEP CELIK " + slug.toUpperCase() + "\",\"company\":\"GUNES ENERJI "
                + slug.toUpperCase() + "\"}", 200);
        assertThat(reasons(byName.get(0))).containsExactly("NAME_COMPANY");
        // name alone never matches; all four together: strongest first in one entry
        assertThat(check("{\"name\":\"" + name + "\"}", 200)).isEmpty();
        JsonNode all = check("{\"name\":\"" + name + "\",\"company\":\"Güneş Enerji " + slug + "\",\"email\":\""
                + email + "\",\"phone\":\"0555" + d + "\"}", 200);
        assertThat(all).hasSize(1);
        assertThat(reasons(all.get(0))).startsWith("EMAIL").contains("NAME_COMPANY");
        // exclude the person itself; an empty body is a 400
        assertThat(check("{\"email\":\"" + email + "\",\"excludePersonId\":\"" + id + "\"}", 200)).isEmpty();
        check("{}", 400);
        // anonymised people never match
        jdbc.update("update person set anonymised_at = now() where id = ?::uuid", id);
        assertThat(check("{\"email\":\"" + email + "\"}", 200)).isEmpty();
    }

    @Test
    void queueDismissAndNoReopen() throws Exception {
        String d = digits();
        String phone = "0532 " + d.substring(0, 3) + " " + d.substring(3, 5) + " " + d.substring(5, 7);
        String idA = createPerson(consultant, unique("Çift A "), ",\"phone\":\"" + phone + "\"").get("id").asText();
        String idB = createPerson(consultant, unique("Çift B "), ",\"phone\":\"" + phone + "\"").get("id").asText();

        JsonNode queue = get(consultant, "/api/v1/duplicates?personId=" + idA, 200);
        assertThat(queue.get("content")).hasSize(1);
        JsonNode pair = queue.get("content").get(0);
        assertThat(pair.get("status").asText()).isEqualTo("OPEN");
        assertThat(reasons(pair)).containsExactly("PHONE");
        List<String> both = List.of(pair.get("personA").get("id").asText(), pair.get("personB").get("id").asText());
        assertThat(both).containsExactlyInAnyOrder(idA, idB);

        JsonNode dismissed = post(consultant, "/api/v1/duplicates/" + pair.get("id").asText() + "/dismiss", "", 200);
        assertThat(dismissed.get("status").asText()).isEqualTo("DISMISSED");
        assertThat(dismissed.get("resolvedBy").get("id").asText()).isEqualTo(consultant.userId().toString());
        post(consultant, "/api/v1/duplicates/" + pair.get("id").asText() + "/dismiss", "", 409);
        post(consultant, "/api/v1/duplicates/00000000-0000-7000-8000-000000000001/dismiss", "", 404);
        // a later edit never re-opens a dismissed pair
        patch(consultant, "/api/v1/people/" + idB, "{\"notes\":\"again\"}", 200);
        assertThat(get(consultant, "/api/v1/duplicates?personId=" + idA, 200).get("content")).isEmpty();
        assertThat(get(consultant, "/api/v1/duplicates?personId=" + idA + "&status=DISMISSED", 200).get("content"))
                .hasSize(1);
    }

    @Test
    void mergeProfileThenFinishIsAuditedAndMovesWhatBelongsToTheSurvivor() throws Exception {
        String w = unique("x");
        JsonNode s = createPerson(consultant, unique("Hayatta "), ",\"title\":\"Direktör\",\"languages\":[{\"language\":"
                + "\"en\",\"level\":\"FLUENT\"}],\"employments\":[{\"company\":\"Alfa " + w + "\"}]");
        JsonNode m = createPerson(consultant, unique("Silinen "), ",\"email\":\"" + w + "@example.test\","
                + "\"title\":\"Müdür\",\"city\":\"İzmir\",\"noticePeriodDays\":30,\"languages\":[{\"language\":\"en\","
                + "\"level\":\"BASIC\"},{\"language\":\"de\",\"level\":\"BASIC\"}],\"educations\":[{\"school\":\"ODTÜ\"}],"
                + "\"employments\":[{\"company\":\"alfa " + w + "\"},{\"company\":\"Beta " + w + "\"}],"
                + "\"compensation\":{\"currency\":\"TRY\",\"period\":\"ANNUAL\",\"basis\":\"GROSS\",\"expected\":9}");
        String sid = s.get("id").asText();
        String mid = m.get("id").asText();
        jdbc.update("update person set tags = '{a,b}' where id = ?::uuid", sid);
        jdbc.update("update person set tags = '{b,c}' where id = ?::uuid", mid);
        // same CV bytes on both (dropped), a different CV on the merged one, a document
        file(sid, "cv.txt", "ortak özgeçmiş " + w, "CV");
        file(mid, "cv.txt", "ortak özgeçmiş " + w, "CV");
        file(mid, "yeni.txt", "daha yeni özgeçmiş " + w, "CV");
        file(mid, "diploma.txt", "diploma " + w, "DOCUMENT");
        // links: S -> M becomes a self-link (dropped), M -> other moves
        String other = createPerson(consultant, unique("Diğer "), "").get("id").asText();
        post(consultant, "/api/v1/network-links", "{\"fromPersonId\":\"" + sid + "\",\"toPersonId\":\"" + mid
                + "\",\"type\":\"KNOWS\"}", 201);
        post(consultant, "/api/v1/network-links", "{\"fromPersonId\":\"" + mid + "\",\"toPersonId\":\"" + other
                + "\",\"type\":\"KNOWS\"}", 201);
        // a duplicate pair between them, then the merge
        long version = s.get("version").asLong();

        // guards
        assertThatThrownBy(() -> tx.executeWithoutResult(t -> mergeApi.mergeProfile(sid(sid), sid(sid), Set.of(), version,
                null))).isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.STATE_CONFLICT));
        assertThatThrownBy(() -> tx.executeWithoutResult(t -> mergeApi.mergeProfile(sid(sid), sid(mid), Set.of(),
                version + 5, null))).isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.VERSION_CONFLICT));
        assertThatThrownBy(() -> tx.executeWithoutResult(t -> mergeApi.mergeProfile(sid(sid), sid(mid), Set.of("bogus"),
                version, null))).isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_FAILED));

        Set<String> take = Set.of("title", "compensation");
        UUID actor = manager.userId();
        tx.executeWithoutResult(t -> {
            assertThat(mergeApi.mergedIds(sid(sid))).isEmpty();
            mergeApi.mergeProfile(sid(sid), sid(mid), take, version, actor);
            mergeApi.finish(sid(sid), sid(mid), take, actor);
        });

        JsonNode merged = get(manager, "/api/v1/people/" + sid, 200);
        assertThat(merged.get("title").asText()).isEqualTo("Müdür"); // taken
        assertThat(merged.get("city").asText()).isEqualTo("İzmir"); // filled
        assertThat(merged.get("email").asText()).isEqualTo(w + "@example.test"); // filled from merged
        assertThat(merged.get("noticePeriodDays").asInt()).isEqualTo(30);
        assertThat(merged.get("compensation").get("expected").asInt()).isEqualTo(9);
        assertThat(merged.get("tags")).hasSize(3);
        assertThat(merged.get("languages")).hasSize(2);
        assertThat(merged.get("employments")).hasSize(2); // "alfa" equals "Alfa", Beta appended
        assertThat(merged.get("educations")).hasSize(1);
        get(manager, "/api/v1/people/" + mid, 404);
        // files: identical bytes dropped, versions renumbered, newest CV current, document moved
        JsonNode files = get(manager, "/api/v1/people/" + sid + "/files", 200);
        assertThat(files).hasSize(3);
        List<Integer> cvVersions = new ArrayList<>();
        files.forEach(f -> {
            if (f.get("kind").asText().equals("CV")) {
                cvVersions.add(f.get("version").asInt());
            }
        });
        assertThat(cvVersions).containsExactlyInAnyOrder(1, 2);
        assertThat(merged.get("currentCv").get("version").asInt()).isEqualTo(2);
        // links: the self-link is gone, the other one moved
        assertThat(jdbc.queryForObject("select count(*) from network_link where from_person_id = ?::uuid and "
                + "to_person_id = ?::uuid", Integer.class, sid, other)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from network_link where from_person_id = ?::uuid or "
                + "to_person_id = ?::uuid", Integer.class, sid, sid)).isEqualTo(1);
        // audit: person_merge row, DEL revision for the merged person, merged ids readable (step 0)
        assertThat(mergeApi.mergedIds(sid(sid))).containsExactly(sid(mid));
        assertThat(jdbc.queryForObject("select count(*) from person_merge where survivor_id = ?::uuid and merged_id = "
                + "?::uuid and merged_by = ?::uuid and 'compensation' = any(fields_taken)", Integer.class, sid, mid,
                manager.userId().toString())).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from person_aud where id = ?::uuid and revtype = 2",
                Integer.class, mid)).isEqualTo(1);
    }

    private static UUID sid(String s) {
        return UUID.fromString(s);
    }

    private void file(String personId, String name, String content, String kind) throws Exception {
        send(consultant, multipart("/api/v1/people/" + personId + "/files")
                .file(new MockMultipartFile("file", name, "text/plain", content.getBytes(StandardCharsets.UTF_8)))
                .param("kind", kind), 201);
    }
}
