package com.recruiting.platform.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.recruiting.platform.contracts.Role;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** M2-AG: fee agreements, their rules, the signed PDF and the change history through /audit/revisions. */
class AgreementApiIT extends AbstractApiIT {

    private static final byte[] PDF = "%PDF-1.4\n% synthetic test file\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
    private final JsonMapper json = JsonMapper.builder().build();
    private Client consultant;
    private Client manager;

    @BeforeEach
    void logIn() throws Exception {
        consultant = login(Role.CONSULTANT);
        manager = login(Role.MANAGER);
    }

    private static org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder<?> auth(Client c,
            org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder<?> b) {
        return b.session(c.session()).cookie(new jakarta.servlet.http.Cookie("XSRF-TOKEN", c.csrf()))
                .header("X-XSRF-TOKEN", c.csrf());
    }

    private JsonNode call(Client c, MockHttpServletRequestBuilder b, String body, int expected) throws Exception {
        if (body != null) {
            b.contentType(MediaType.APPLICATION_JSON).content(body.getBytes(StandardCharsets.UTF_8));
        }
        String out = mvc.perform(auth(c, b)).andExpect(status().is(expected)).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return out.isBlank() ? null : json.readTree(out);
    }

    private String account() throws Exception {
        return call(consultant, post("/api/v1/accounts"), "{\"name\":\"Sample Holding " + UUID.randomUUID() + "\",\"sector\":\"Energy\"}", 201)
                .get("id").asText();
    }

    private String job(String accountId) throws Exception {
        return call(consultant, post("/api/v1/job-orders"),
                "{\"accountId\":\"" + accountId + "\",\"title\":\"Sample Director\"}", 201).get("id").asText();
    }

    private String contact(String accountId) throws Exception {
        return call(consultant, post("/api/v1/accounts/" + accountId + "/contacts"),
                "{\"person\":{\"name\":\"Sample Signer\",\"type\":\"CLIENT_CONTACT\",\"email\":\"s-"
                        + UUID.randomUUID() + "@example.test\"},\"role\":\"CEO\"}", 201).get("id").asText();
    }

    private static String terms(String accountId, String extra) {
        return "{\"accountId\":\"" + accountId + "\",\"kind\":\"TERMS_OF_BUSINESS\",\"feeModel\":\"PERCENT_FIRST_YEAR_CASH\","
                + "\"feePercent\":\"25\",\"feeBasis\":[\"BASE\",\"TARGET_BONUS\"],\"currency\":\"TRY\"" + extra + "}";
    }

    private JsonNode revisions(String type, String id) throws Exception {
        return call(manager, get("/api/v1/audit/revisions?entityType=" + type + "&entityId=" + id), null, 200);
    }

    @Test
    void createListFilterAndDefaults() throws Exception {
        String acc = account();
        String jo = job(acc);
        JsonNode t = call(consultant, post("/api/v1/agreements"), terms(acc, ""), 201);
        assertThat(t.get("status").asText()).isEqualTo("DRAFT");
        assertThat(t.get("billingAccount").get("id").asText()).isEqualTo(acc);
        assertThat(t.get("jobOrder").isNull()).isTrue();
        assertThat(t.get("feePercent").asText()).isEqualTo("25");

        String body = "{\"accountId\":\"" + acc + "\",\"jobOrderId\":\"" + jo + "\",\"kind\":\"ENGAGEMENT_LETTER\","
                + "\"feeModel\":\"RETAINED_THIRDS\",\"feePercent\":\"30\",\"feeBasis\":[\"BASE\"],\"currency\":\"TRY\","
                + "\"estimatedFee\":\"300000.00\",\"status\":\"SENT\",\"scheduleItems\":["
                + "{\"label\":\"Retainer 1/3\",\"trigger\":\"SIGNING\",\"percent\":\"33.33\"},"
                + "{\"label\":\"Retainer 2/3\",\"trigger\":\"SHORTLIST_DELIVERED\",\"percent\":\"33.33\"},"
                + "{\"label\":\"Retainer 3/3\",\"trigger\":\"OFFER_ACCEPTED\",\"percent\":\"33.34\"}]}";
        JsonNode e = call(consultant, post("/api/v1/agreements"), body, 201);
        assertThat(e.get("scheduleItems")).hasSize(3);
        assertThat(e.get("scheduleItems").get(0).get("seq").asInt()).isEqualTo(1);
        assertThat(e.get("scheduleItems").get(0).get("computedAmount").asText()).isEqualTo("99990.00");
        assertThat(e.get("scheduleItems").get(0).get("invoiceState").asText()).isEqualTo("NOT_DUE");

        assertThat(call(consultant, get("/api/v1/agreements?accountId=" + acc), null, 200).get("content")).hasSize(2);
        JsonNode byJob = call(consultant, get("/api/v1/agreements?jobOrderId=" + jo), null, 200);
        assertThat(byJob.get("content")).hasSize(1);
        assertThat(byJob.get("content").get(0).get("id").asText()).isEqualTo(e.get("id").asText());
        JsonNode sent = call(consultant, get("/api/v1/agreements?accountId=" + acc + "&status=SENT"), null, 200);
        assertThat(sent.get("content")).hasSize(1);
        assertThat(call(consultant, get("/api/v1/agreements?accountId=" + acc + "&status=SIGNED&status=EXPIRED"), null,
                200).get("content")).isEmpty();
        call(consultant, get("/api/v1/agreements?status=NOPE"), null, 400);
        call(consultant, get("/api/v1/agreements/" + e.get("id").asText()), null, 200);
        call(consultant, get("/api/v1/agreements/" + UUID.randomUUID()), null, 404);
    }

    @Test
    void validationRules() throws Exception {
        String acc = account();
        String other = account();
        String otherJob = job(other);
        call(consultant, post("/api/v1/agreements"), terms(acc, "").replace("\"feePercent\":\"25\",", ""), 400);
        call(consultant, post("/api/v1/agreements"), terms(acc, ",\"feeMinimum\":\"9\",\"feeCap\":\"5\""), 400);
        call(consultant, post("/api/v1/agreements"),
                terms(acc, "").replace("PERCENT_FIRST_YEAR_CASH", "FLAT_FEE"), 400);
        call(consultant, post("/api/v1/agreements"), terms(acc, ",\"jobOrderId\":\"" + otherJob + "\"")
                .replace("TERMS_OF_BUSINESS", "AMENDMENT"), 400);
        call(consultant, post("/api/v1/agreements"), terms(acc, ",\"scheduleItems\":[{\"label\":\"x\","
                + "\"trigger\":\"SIGNING\",\"percent\":\"10\",\"amount\":\"5\"}]"), 400);
        call(consultant, post("/api/v1/agreements"), terms(acc, ",\"scheduleItems\":[{\"label\":\"x\","
                + "\"trigger\":\"DATE_OFFSET\",\"percent\":\"10\"}]"), 400);
        call(consultant, post("/api/v1/agreements"), terms(acc, ",\"scheduleItems\":[{\"label\":\"x\","
                + "\"trigger\":\"SIGNING\",\"percent\":\"10\"}]").replace("PERCENT_FIRST_YEAR_CASH", "RETAINED_THIRDS"),
                400);
        call(consultant, post("/api/v1/agreements"), terms(UUID.randomUUID().toString(), ""), 404);
        call(consultant, post("/api/v1/agreements"), terms(acc, ",\"status\":\"SIGNED\""), 400);
    }

    @Test
    void updateVersionSignedRulesAndSchedule() throws Exception {
        String acc = account();
        JsonNode a = call(consultant, post("/api/v1/agreements"), terms(acc, ",\"title\":\"Terms 2026\""), 201);
        String id = a.get("id").asText();
        long v = a.get("version").asLong();
        JsonNode upd = call(consultant, patch("/api/v1/agreements/" + id),
                "{\"title\":\"Terms 2027\",\"version\":" + v + "}", 200);
        assertThat(upd.get("title").asText()).isEqualTo("Terms 2027");
        assertThat(upd.get("version").asLong()).isGreaterThan(v);
        JsonNode stale = call(consultant, patch("/api/v1/agreements/" + id),
                "{\"title\":\"Late\",\"version\":" + v + "}", 409);
        assertThat(stale.get("code").asText()).isEqualTo("VERSION_CONFLICT");

        call(consultant, patch("/api/v1/agreements/" + id), "{\"status\":\"SIGNED\"}", 400);
        JsonNode signed = call(consultant, patch("/api/v1/agreements/" + id),
                "{\"status\":\"SIGNED\",\"signedOn\":\"2026-03-01\"}", 200);
        assertThat(signed.get("status").asText()).isEqualTo("SIGNED");
        call(consultant, patch("/api/v1/agreements/" + id), "{\"feePercent\":\"22\"}", 403);
        call(consultant, patch("/api/v1/agreements/" + id), "{\"title\":\"Renamed after signing\"}", 200);
        call(manager, patch("/api/v1/agreements/" + id), "{\"feePercent\":\"22\"}", 200);

        call(consultant, delete("/api/v1/agreements/" + id), null, 409);
    }

    @Test
    void engagementScheduleReplaceAndSigningMakesItemsDue() throws Exception {
        String acc = account();
        String jo = job(acc);
        String body = "{\"accountId\":\"" + acc + "\",\"jobOrderId\":\"" + jo + "\",\"kind\":\"ENGAGEMENT_LETTER\","
                + "\"feeModel\":\"RETAINED_CUSTOM\",\"feePercent\":\"25\",\"feeBasis\":[\"BASE\"],\"currency\":\"TRY\","
                + "\"scheduleItems\":[{\"label\":\"A\",\"trigger\":\"SIGNING\",\"amount\":\"1000\"},"
                + "{\"label\":\"B\",\"trigger\":\"START_DATE\",\"amount\":\"2000\"}]}";
        JsonNode a = call(consultant, post("/api/v1/agreements"), body, 201);
        String id = a.get("id").asText();
        String first = a.get("scheduleItems").get(0).get("id").asText();
        String second = a.get("scheduleItems").get(1).get("id").asText();
        // reorder, drop none, add one
        JsonNode re = call(consultant, patch("/api/v1/agreements/" + id), "{\"scheduleItems\":["
                + "{\"id\":\"" + second + "\",\"label\":\"B2\",\"trigger\":\"START_DATE\",\"amount\":\"2000\"},"
                + "{\"id\":\"" + first + "\",\"label\":\"A\",\"trigger\":\"SIGNING\",\"amount\":\"1000\"},"
                + "{\"label\":\"C\",\"trigger\":\"EXPENSES\",\"amount\":\"50\"}]}", 200);
        assertThat(re.get("scheduleItems")).hasSize(3);
        assertThat(re.get("scheduleItems").get(0).get("id").asText()).isEqualTo(second);
        assertThat(re.get("scheduleItems").get(0).get("label").asText()).isEqualTo("B2");
        call(consultant, patch("/api/v1/agreements/" + id), "{\"scheduleItems\":["
                + "{\"id\":\"" + UUID.randomUUID() + "\",\"label\":\"Z\",\"trigger\":\"SIGNING\",\"amount\":\"1\"}]}", 400);

        JsonNode signed = call(consultant, patch("/api/v1/agreements/" + id),
                "{\"status\":\"SIGNED\",\"signedOn\":\"2026-03-01\"}", 200);
        JsonNode signingItem = null;
        for (JsonNode i : signed.get("scheduleItems")) {
            if (i.get("trigger").asText().equals("SIGNING")) {
                signingItem = i;
            }
        }
        assertThat(signingItem.get("invoiceState").asText()).isEqualTo("DUE");
        assertThat(signingItem.get("dueOn").isNull()).isFalse();
        // a due item freezes the schedule
        call(manager, patch("/api/v1/agreements/" + id), "{\"scheduleItems\":[]}", 409);
    }

    @Test
    void signatoryMustBeActiveContactOfTheAccount() throws Exception {
        String acc = account();
        String other = account();
        String ok = contact(acc);
        String foreign = contact(other);
        JsonNode a = call(consultant, post("/api/v1/agreements"), terms(acc, ",\"signatoryContactId\":\"" + ok + "\""),
                201);
        assertThat(a.get("signatory").get("name").asText()).isEqualTo("Sample Signer");
        call(consultant, post("/api/v1/agreements"), terms(acc, ",\"signatoryContactId\":\"" + foreign + "\""), 400);
        call(consultant, post("/api/v1/agreements"),
                terms(acc, ",\"signatoryContactId\":\"" + UUID.randomUUID() + "\""), 400);
        call(consultant, patch("/api/v1/agreements/" + a.get("id").asText()),
                "{\"signatoryContactId\":\"" + foreign + "\"}", 400);
        call(consultant, delete("/api/v1/accounts/" + acc + "/contacts/" + ok), null, 200);
        call(consultant, patch("/api/v1/agreements/" + a.get("id").asText()),
                "{\"signatoryContactId\":\"" + ok + "\"}", 400);
    }

    @Test
    void billingAccountAndDelete() throws Exception {
        String acc = account();
        String parent = account();
        JsonNode a = call(consultant, post("/api/v1/agreements"), terms(acc, ",\"billingAccountId\":\"" + parent + "\""),
                201);
        assertThat(a.get("billingAccount").get("id").asText()).isEqualTo(parent);
        call(consultant, post("/api/v1/agreements"), terms(acc, ",\"billingAccountId\":\"" + UUID.randomUUID() + "\""),
                404);
        call(consultant, delete("/api/v1/agreements/" + a.get("id").asText()), null, 204);
        call(consultant, get("/api/v1/agreements/" + a.get("id").asText()), null, 404);
    }

    private JsonNode upload(Client c, String id, String name, byte[] bytes, String type, int expected)
            throws Exception {
        var b = multipart(HttpMethod.PUT, "/api/v1/agreements/" + id + "/signed-file")
                .file(new MockMultipartFile("file", name, type, bytes));
        String out = mvc.perform(auth(c, b)).andExpect(status().is(expected)).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return out.isBlank() ? null : json.readTree(out);
    }

    @Test
    void signedFileUploadReplaceDownloadAndWrongType() throws Exception {
        String acc = account();
        JsonNode a = call(consultant, post("/api/v1/agreements"), terms(acc, ""), 201);
        String id = a.get("id").asText();
        call(consultant, get("/api/v1/agreements/" + id + "/signed-file"), null, 404);

        JsonNode up = upload(consultant, id, "tob-signed.pdf", PDF, "application/pdf", 200);
        assertThat(up.get("signedFile").get("fileName").asText()).isEqualTo("tob-signed.pdf");
        assertThat(up.get("signedFile").get("sizeBytes").asLong()).isEqualTo(PDF.length);
        assertThat(up.get("status").asText()).isEqualTo("DRAFT");
        String firstFileId = up.get("signedFile").get("id").asText();

        // a text file named .pdf is rejected from its bytes; a PDF with a wrong declared type is accepted
        JsonNode untouched = upload(consultant, id, "fake.pdf", "just text".getBytes(StandardCharsets.UTF_8),
                "application/pdf", 415);
        assertThat(untouched.get("code").asText()).isEqualTo("UNSUPPORTED_MEDIA_TYPE");
        upload(consultant, id, "empty.pdf", new byte[0], "application/pdf", 400);

        JsonNode replaced = upload(consultant, id, "tob-v2.pdf", PDF, "application/octet-stream", 200);
        assertThat(replaced.get("signedFile").get("id").asText()).isNotEqualTo(firstFileId);
        assertThat(replaced.get("signedFile").get("fileName").asText()).isEqualTo("tob-v2.pdf");

        MvcResult dl = mvc.perform(consultant.with(get("/api/v1/agreements/" + id + "/signed-file")))
                .andExpect(status().isOk()).andReturn();
        assertThat(dl.getResponse().getContentAsByteArray()).isEqualTo(PDF);
        assertThat(dl.getResponse().getHeader("Content-Disposition")).startsWith("attachment");
        assertThat(dl.getResponse().getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(dl.getResponse().getContentType()).isEqualTo("application/pdf");

        // stale version on upload
        var stale = multipart(HttpMethod.PUT, "/api/v1/agreements/" + id + "/signed-file")
                .file(new MockMultipartFile("file", "x.pdf", "application/pdf", PDF)).param("version", "0");
        mvc.perform(auth(consultant, stale)).andExpect(status().isConflict());
    }

    @Test
    void revisionsAreReadableForAgreementAndProposal() throws Exception {
        String acc = account();
        JsonNode a = call(consultant, post("/api/v1/agreements"), terms(acc, ",\"title\":\"First\""), 201);
        String id = a.get("id").asText();
        call(consultant, patch("/api/v1/agreements/" + id), "{\"title\":\"Second\"}", 200);
        JsonNode rev = revisions("AGREEMENT", id);
        assertThat(rev.get("content")).hasSize(2);
        assertThat(rev.get("content").get(0).get("type").asText()).isEqualTo("MOD");
        boolean titleChange = false;
        for (JsonNode ch : rev.get("content").get(0).get("changes")) {
            titleChange |= ch.get("field").asText().equals("title") && ch.get("to").asText().equals("Second");
        }
        assertThat(titleChange).isTrue();
        call(consultant, get("/api/v1/audit/revisions?entityType=AGREEMENT&entityId=" + id), null, 403);

        JsonNode p = call(consultant, post("/api/v1/proposals"),
                "{\"accountId\":\"" + acc + "\",\"title\":\"Sample proposal\"}", 201);
        String pid = p.get("id").asText();
        call(consultant, patch("/api/v1/proposals/" + pid), "{\"title\":\"Sample proposal 2\"}", 200);
        assertThat(revisions("PROPOSAL", pid).get("content")).hasSize(2);
    }
}
