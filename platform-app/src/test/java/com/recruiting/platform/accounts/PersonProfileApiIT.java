package com.recruiting.platform.accounts;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruiting.platform.contracts.Role;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** Wave 2b slice 1: candidate profile, compensation access and encryption, pool filters on /people. */
class PersonProfileApiIT extends AccountsApiTestBase {

    private static final String COMP = ",\"compensation\":{\"currentBase\":250000,\"expected\":300000,"
            + "\"currency\":\"TRY\",\"period\":\"ANNUAL\",\"basis\":\"GROSS\",\"benefits\":\"Car\"}";

    private Client manager;
    private Client owner;
    private Client other;

    @BeforeEach
    void logIn() throws Exception {
        manager = login(Role.MANAGER);
        owner = login(Role.CONSULTANT);
        other = login(Role.CONSULTANT);
    }

    private static List<String> ids(JsonNode page) {
        List<String> out = new ArrayList<>();
        page.get("content").forEach(n -> out.add(n.get("id").asText()));
        return out;
    }

    @Test
    void profileFieldsRoundTripAndAreNormalised() throws Exception {
        String name = unique("Profil ");
        JsonNode p = createPerson(owner, name, ",\"phone\":\"0 (555) 123 45 67\","
                + "\"linkedinUrl\":\"https://tr.linkedin.com/in/Fictional-Person/?trk=x\","
                + "\"sourceChannel\":\"REFERRAL\",\"noticePeriodDays\":60,\"mobility\":\"WITHIN_COUNTRY\","
                + "\"languages\":[{\"language\":\"en\",\"level\":\"FLUENT\"},{\"language\":\"tr\",\"level\":\"NATIVE\"}],"
                + "\"educations\":[{\"school\":\"Boğaziçi Üniversitesi\",\"degree\":\"MBA\",\"startYear\":2010,"
                + "\"endYear\":2012}]");
        String id = p.get("id").asText();
        assertThat(p.get("sourceChannel").asText()).isEqualTo("REFERRAL");
        assertThat(p.get("noticePeriodDays").asInt()).isEqualTo(60);
        assertThat(p.get("languages")).hasSize(2);
        assertThat(p.get("educations").get(0).get("school").asText()).isEqualTo("Boğaziçi Üniversitesi");
        assertThat(jdbc.queryForObject("select phone_normalized from person where id = ?::uuid", String.class, id))
                .isEqualTo("+905551234567");
        assertThat(jdbc.queryForObject("select linkedin_url_normalized from person where id = ?::uuid", String.class,
                id)).isEqualTo("https://www.linkedin.com/in/fictional-person");

        JsonNode updated = patch(owner, "/api/v1/people/" + id, "{\"version\":" + p.get("version").asLong()
                + ",\"phone\":\"+44 20 7946 0958\",\"languages\":[{\"language\":\"de\",\"level\":\"BASIC\"}],"
                + "\"educations\":[],\"mobility\":null}", 200);
        assertThat(updated.get("languages")).hasSize(1);
        assertThat(updated.get("educations")).isEmpty();
        assertThat(updated.get("mobility").isNull()).isTrue();
        assertThat(jdbc.queryForObject("select phone_normalized from person where id = ?::uuid", String.class, id))
                .isEqualTo("+442079460958");
    }

    @Test
    void profileValidation() throws Exception {
        String bad = "{\"name\":\"X\",\"type\":\"CANDIDATE\",";
        post(owner, "/api/v1/people", bad + "\"noticePeriodDays\":400}", 400);
        post(owner, "/api/v1/people", bad + "\"languages\":[{\"language\":\"en\",\"level\":\"FLUENT\"},"
                + "{\"language\":\"en\",\"level\":\"BASIC\"}]}", 400);
        post(owner, "/api/v1/people", bad + "\"languages\":[{\"language\":\"english\",\"level\":\"FLUENT\"}]}", 400);
        post(owner, "/api/v1/people", bad + "\"educations\":[{\"school\":\"A\",\"startYear\":2012,\"endYear\":2010}]}",
                400);
        post(owner, "/api/v1/people", bad + "\"compensation\":{\"currency\":\"try\",\"period\":\"ANNUAL\","
                + "\"basis\":\"GROSS\"}}", 400);
        post(owner, "/api/v1/people", bad + "\"sourceChannel\":\"FAX\"}", 400);
    }

    @Test
    void compensationIsEncryptedAndRestrictedToManagersAndTheOwner() throws Exception {
        JsonNode p = createPerson(owner, unique("Maaş "), COMP);
        String id = p.get("id").asText();
        assertThat(p.get("compensationRestricted").asBoolean()).isFalse();
        assertThat(p.get("compensation").get("expected").asInt()).isEqualTo(300000);

        // stored as ciphertext, never as readable JSON
        byte[] stored = jdbc.queryForObject("select compensation_enc from person where id = ?::uuid", byte[].class, id);
        assertThat(stored).isNotNull();
        assertThat(new String(stored, java.nio.charset.StandardCharsets.ISO_8859_1)).doesNotContain("TRY")
                .doesNotContain("ANNUAL");

        JsonNode asOther = get(other, "/api/v1/people/" + id, 200);
        assertThat(asOther.get("compensation").isNull()).isTrue();
        assertThat(asOther.get("compensationRestricted").asBoolean()).isTrue();
        JsonNode asManager = get(manager, "/api/v1/people/" + id, 200);
        assertThat(asManager.get("compensationRestricted").asBoolean()).isFalse();
        assertThat(asManager.get("compensation").get("currency").asText()).isEqualTo("TRY");

        // never in lists
        JsonNode list = get(manager, "/api/v1/people?q=" + p.get("name").asText().replace(" ", "%20"), 200);
        assertThat(list.get("content").get(0).has("compensation")).isFalse();

        // 403 on write for everybody else, also when it is the only thing sent
        patch(other, "/api/v1/people/" + id, "{\"compensation\":null}", 403);
        patch(other, "/api/v1/people/" + id, "{\"notes\":\"fine\"}", 200);
        patch(manager, "/api/v1/people/" + id, "{\"compensation\":{\"currency\":\"EUR\",\"period\":\"MONTHLY\","
                + "\"basis\":\"NET\",\"expected\":5000}}", 200);
        assertThat(get(owner, "/api/v1/people/" + id, 200).get("compensation").get("currency").asText())
                .isEqualTo("EUR");
        patch(owner, "/api/v1/people/" + id, "{\"compensation\":null}", 200);
        assertThat(jdbc.queryForObject("select compensation_enc from person where id = ?::uuid", byte[].class, id))
                .isNull();

        // a consultant cannot create a person owned by somebody else with compensation
        post(other, "/api/v1/people", "{\"name\":\"Y\",\"type\":\"CANDIDATE\",\"ownerId\":\"" + owner.userId() + "\""
                + COMP + "}", 403);
    }

    @Test
    void viewIsLoggedWhenAPersonIsOpened() throws Exception {
        JsonNode p = createPerson(owner, unique("Görüntü "), "");
        String id = p.get("id").asText();
        Integer before = jdbc.queryForObject(
                "select count(*) from access_log where entity_id = ?::uuid and action = 'VIEW'", Integer.class, id);
        get(other, "/api/v1/people/" + id, 200);
        Integer after = jdbc.queryForObject(
                "select count(*) from access_log where entity_id = ?::uuid and action = 'VIEW'", Integer.class, id);
        assertThat(after).isEqualTo(before + 1);
    }

    @Test
    void poolFilters() throws Exception {
        String marker = unique("pool");
        JsonNode a = createPerson(owner, unique("A "), ",\"city\":\"Şişli\",\"company\":\"Çelik " + marker
                + " A.Ş.\",\"sourceChannel\":\"LINKEDIN_AD\"");
        JsonNode b = createPerson(owner, unique("B "), ",\"city\":\"Ankara\",\"company\":\"Ceylan " + marker
                + "\",\"sourceChannel\":\"EMAIL\"");
        String idA = a.get("id").asText();
        String idB = b.get("id").asText();
        jdbc.update("update person set tags = '{cfo-network,istanbul}' where id = ?::uuid", idA);
        jdbc.update("update person set tags = '{istanbul}', consent_status = 'GIVEN' where id = ?::uuid", idB);

        // city: accent- and case-insensitive, several values
        assertThat(ids(get(manager, "/api/v1/people?city=sisli", 200))).contains(idA).doesNotContain(idB);
        assertThat(ids(get(manager, "/api/v1/people?city=SİŞLİ", 200))).contains(idA);
        assertThat(ids(get(manager, "/api/v1/people?city=sisli&city=ANKARA", 200))).contains(idA, idB);
        // company contains, accent-insensitive
        assertThat(ids(get(manager, "/api/v1/people?company=celik%20" + marker, 200))).containsExactly(idA);
        // tags (any of)
        assertThat(ids(get(manager, "/api/v1/people?tag=cfo-network", 200))).contains(idA).doesNotContain(idB);
        assertThat(ids(get(manager, "/api/v1/people?tag=CFO-network&tag=istanbul&size=100", 200))).contains(idA, idB);
        // source channel
        assertThat(ids(get(manager, "/api/v1/people?sourceChannel=EMAIL&company=" + marker, 200))).containsExactly(idB);
        // shareable
        assertThat(ids(get(manager, "/api/v1/people?shareable=true&company=" + marker, 200))).containsExactly(idB);
        assertThat(ids(get(manager, "/api/v1/people?shareable=false&company=" + marker, 200))).containsExactly(idA);
        JsonNode summary = get(manager, "/api/v1/people?shareable=true&company=" + marker, 200).get("content").get(0);
        assertThat(summary.get("shareable").asBoolean()).isTrue();
        assertThat(summary.get("tags")).hasSize(1);
        assertThat(summary.get("sourceChannel").asText()).isEqualTo("EMAIL");
        get(manager, "/api/v1/people?cv=a", 400);
        get(manager, "/api/v1/people?sourceChannel=FAX", 400);
    }

    @Test
    void cvFullTextIsTurkishAwareWithRelevanceAndExcerpt() throws Exception {
        JsonNode a = createPerson(owner, unique("CV A "), "");
        JsonNode b = createPerson(owner, unique("CV B "), "");
        String idA = a.get("id").asText();
        String idB = b.get("id").asText();
        String word = "mühendislik" + alpha();
        insertCv(idA, "Kıdemli " + word + " yöneticisi, Şişli ofisi. Finans ve " + word + " deneyimi.", true);
        insertCv(idB, "Pazarlama uzmanı. Yalnızca eski bir " + word, false); // not the current CV: never searched
        JsonNode r = get(manager, "/api/v1/people?cv=" + word.replace("ü", "u") + "&size=100", 200);
        assertThat(ids(r)).containsExactly(idA);
        assertThat(r.get("content").get(0).get("cvMatch").asText()).contains("«").contains("»");
        // web-search syntax: exclusion and phrase
        assertThat(ids(get(manager, "/api/v1/people?cv=" + word + "%20-finans", 200))).isEmpty();
        assertThat(ids(get(manager, "/api/v1/people?cv=%22isli%20ofisi%22", 200))).isEmpty();
        assertThat(ids(get(manager, "/api/v1/people?cv=%22sisli%20ofisi%22%20" + word, 200))).containsExactly(idA);
    }

    private void insertCv(String personId, String text, boolean current) {
        String key = "person/" + personId + "/" + java.util.UUID.randomUUID();
        jdbc.update("insert into person_file (id, person_id, kind, version, is_current, file_name, content_type, "
                + "size_bytes, sha256, storage_key, text_status, extracted_text, uploaded_by, created_at, updated_at, "
                + "row_version) values (?::uuid, ?::uuid, 'CV', 1, ?, 'cv.txt', 'text/plain', 10, ?, ?, 'DONE', ?, "
                + "?::uuid, now(), now(), 0)", java.util.UUID.randomUUID().toString(), personId, current,
                (java.util.UUID.randomUUID().toString() + java.util.UUID.randomUUID()).replace("-", "").substring(0, 64),
                key, text, owner.userId().toString());
    }

    /** Letters only: the text search configuration drops tokens that mix letters and digits. */
    private static String alpha() {
        StringBuilder sb = new StringBuilder();
        long n = System.nanoTime();
        while (n > 0) {
            sb.append((char) ('a' + n % 26));
            n /= 26;
        }
        return sb.toString();
    }
}
