package com.recruiting.platform.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.recruiting.platform.accounts.file.app.CvIntakeService;
import com.recruiting.platform.accounts.file.app.TextExtractionWorker;
import com.recruiting.platform.contracts.Role;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

/** Wave 2b slice 2: person files, Tika text, download log, CV intake queue, DSR lookup. */
class PersonFilesApiIT extends AccountsApiTestBase {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D',
        'R', 0, 0, 0, 1, 0, 0, 0, 1, 8, 6, 0, 0, 0};

    @Autowired
    TextExtractionWorker worker;
    @Autowired
    CvIntakeService intake;

    private Client consultant;
    private Client manager;

    @BeforeEach
    void logIn() throws Exception {
        consultant = login(Role.CONSULTANT);
        manager = login(Role.MANAGER);
    }

    private static byte[] text(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private JsonNode upload(Client c, String personId, String name, byte[] bytes, int expected) throws Exception {
        return send(c, multipart("/api/v1/people/" + personId + "/files")
                .file(new MockMultipartFile("file", name, "application/octet-stream", bytes)), expected);
    }

    private JsonNode intakeUpload(Client c, int expected, MockMultipartFile... files) throws Exception {
        var b = multipart("/api/v1/cv-intake");
        for (MockMultipartFile f : files) {
            b.file(f);
        }
        return send(c, b, expected);
    }

    private static MockMultipartFile part(String name, byte[] bytes) {
        return new MockMultipartFile("files", name, "application/octet-stream", bytes);
    }

    private static String word() {
        StringBuilder sb = new StringBuilder("kelime");
        long n = System.nanoTime();
        while (n > 0) {
            sb.append((char) ('a' + n % 26));
            n /= 26;
        }
        return sb.toString();
    }

    @Test
    void uploadVersionsListDownloadAndDelete() throws Exception {
        String personId = createPerson(consultant, unique("Dosya "), "").get("id").asText();
        String w = word();
        JsonNode v1 = upload(consultant, personId, "cv-2026.pdf", text("Kıdemli yönetici Şişli " + w), 201);
        assertThat(v1.get("version").asInt()).isEqualTo(1);
        assertThat(v1.get("current").asBoolean()).isTrue();
        assertThat(v1.get("kind").asText()).isEqualTo("CV");
        assertThat(v1.get("contentType").asText()).isEqualTo("text/plain"); // from the bytes, not the .pdf name
        assertThat(v1.get("textStatus").asText()).isEqualTo("PENDING");

        // the worker fills text and the Turkish, accent-free full text search finds the person
        worker.runOnce();
        assertThat(get(consultant, "/api/v1/people/" + personId + "/files", 200).get(0).get("textStatus").asText())
                .isEqualTo("DONE");
        JsonNode found = get(consultant, "/api/v1/people?cv=yonetici sisli " + w, 200);
        assertThat(found.get("content")).hasSize(1);
        assertThat(found.get("content").get(0).get("id").asText()).isEqualTo(personId);

        // same bytes again: 409 with the existing id
        JsonNode dup = upload(consultant, personId, "other-name.txt", text("Kıdemli yönetici Şişli " + w), 409);
        assertThat(dup.get("existingId").asText()).isEqualTo(v1.get("id").asText());

        // a new version becomes the current CV; documents stay version 1 and never current
        JsonNode v2 = upload(consultant, personId, "cv-2027.txt", text("Yeni sürüm " + w), 201);
        assertThat(v2.get("version").asInt()).isEqualTo(2);
        JsonNode doc = send(consultant, multipart("/api/v1/people/" + personId + "/files")
                .file(new MockMultipartFile("file", "diploma.txt", "text/plain", text("Diploma")))
                .param("kind", "DOCUMENT"), 201);
        assertThat(doc.get("current").asBoolean()).isFalse();
        assertThat(doc.get("version").asInt()).isEqualTo(1);
        JsonNode list = get(consultant, "/api/v1/people/" + personId + "/files", 200);
        assertThat(list).hasSize(3);
        assertThat(get(consultant, "/api/v1/people/" + personId, 200).get("currentCv").get("id").asText())
                .isEqualTo(v2.get("id").asText());

        // download: attachment, nosniff, logged
        MvcResult dl = mvc.perform(consultant.with(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/people/" + personId + "/files/" + v1.get("id").asText() + "/content")))
                .andExpect(status().isOk()).andReturn();
        assertThat(dl.getResponse().getHeader("Content-Disposition")).startsWith("attachment");
        assertThat(dl.getResponse().getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(dl.getResponse().getContentType()).isEqualTo("application/octet-stream");
        assertThat(dl.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains(w);
        assertThat(jdbc.queryForObject("select count(*) from access_log where entity_id = ?::uuid and action = "
                + "'DOWNLOAD' and entity_type = 'PERSON_FILE'", Integer.class, v1.get("id").asText())).isEqualTo(1);

        // deleting the current CV makes the older one current again
        delete(consultant, "/api/v1/people/" + personId + "/files/" + v2.get("id").asText(), 204);
        assertThat(get(consultant, "/api/v1/people/" + personId, 200).get("currentCv").get("id").asText())
                .isEqualTo(v1.get("id").asText());
        get(consultant, "/api/v1/people/" + personId + "/files/" + v2.get("id").asText() + "/content", 404);
        delete(consultant, "/api/v1/people/" + personId + "/files/" + v2.get("id").asText(), 404);
    }

    @Test
    void uploadValidation() throws Exception {
        String personId = createPerson(consultant, unique("Sınır "), "").get("id").asText();
        upload(consultant, personId, "cv.txt", PNG, 415); // bytes decide, not the name
        upload(consultant, personId, "cv.txt", new byte[0], 400);
        upload(consultant, personId, "cv.txt", new byte[10 * 1024 * 1024 + 1], 413);
        send(consultant, multipart("/api/v1/people/" + personId + "/files"), 400);
        upload(consultant, "00000000-0000-7000-8000-000000000001", "cv.txt", text("x cv"), 404);
        jdbc.update("update person set anonymised_at = now() where id = ?::uuid", personId);
        upload(consultant, personId, "cv.txt", text("hello"), 409);
    }

    @Test
    void intakeQueueFlow() throws Exception {
        String email = unique("aday") + "@example.test";
        JsonNode existing = createPerson(consultant, unique("Var "), ",\"email\":\"" + email + "\"");
        String cvText = "Fiktif Aday\nE-posta: " + email.toUpperCase() + "\nTel: 0555 123 45 67\n"
                + "https://tr.linkedin.com/in/fiktif-aday\nYönetici " + word();
        JsonNode batch = intakeUpload(consultant, 201, part("a.txt", text(cvText)), part("b.png", PNG),
                part("c.txt", text("İkinci özgeçmiş " + word())), part("big.txt", new byte[11 * 1024 * 1024]));
        assertThat(batch.get("items")).hasSize(2);
        assertThat(batch.get("rejected")).hasSize(2);
        List<String> codes = new ArrayList<>();
        batch.get("rejected").forEach(r -> codes.add(r.get("code").asText()));
        assertThat(codes).containsExactlyInAnyOrder("UNSUPPORTED_MEDIA_TYPE", "PAYLOAD_TOO_LARGE");
        JsonNode first = batch.get("items").get(0);
        String itemId = first.get("id").asText();
        assertThat(first.get("status").asText()).isEqualTo("PENDING");
        assertThat(first.get("textStatus").asText()).isEqualTo("PENDING");

        worker.runOnce();
        JsonNode item = get(consultant, "/api/v1/cv-intake/" + itemId, 200);
        assertThat(item.get("textStatus").asText()).isEqualTo("DONE");
        assertThat(item.get("detected").get("emails").get(0).asText()).isEqualTo(email); // lower-cased
        assertThat(item.get("detected").get("phones")).hasSize(1);
        assertThat(item.get("detected").get("linkedinUrls").get(0).asText()).contains("linkedin.com/in/fiktif-aday");
        assertThat(item.get("duplicates").get(0).get("person").get("id").asText())
                .isEqualTo(existing.get("id").asText());
        assertThat(item.get("duplicates").get(0).get("reasons").get(0).asText()).isEqualTo("EMAIL");
        assertThat(get(consultant, "/api/v1/cv-intake?batchId=" + batch.get("batchId").asText(), 200).get("content"))
                .hasSize(2);

        // DSR lookup by e-mail
        assertThat(intake.findPendingByEmail(email.toUpperCase())).extracting(p -> p.id().toString())
                .containsExactly(itemId);

        // download is logged and attachment
        MvcResult dl = mvc.perform(consultant.with(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/cv-intake/" + itemId + "/content"))).andExpect(status().isOk()).andReturn();
        assertThat(dl.getResponse().getHeader("Content-Disposition")).startsWith("attachment");
        assertThat(jdbc.queryForObject("select count(*) from access_log where entity_id = ?::uuid and action = "
                + "'DOWNLOAD' and entity_type = 'CV_INTAKE_ITEM'", Integer.class, itemId)).isEqualTo(1);

        // attach to the existing person: becomes the current CV, the item is minimised
        JsonNode attached = post(consultant, "/api/v1/cv-intake/" + itemId + "/attach",
                "{\"personId\":\"" + existing.get("id").asText() + "\"}", 201);
        assertThat(attached.get("file").get("current").asBoolean()).isTrue();
        assertThat(attached.get("file").get("textStatus").asText()).isEqualTo("DONE");
        JsonNode done = get(consultant, "/api/v1/cv-intake/" + itemId + "?x=1", 200);
        assertThat(done.get("status").asText()).isEqualTo("ATTACHED");
        assertThat(done.get("fileName").isNull()).isTrue();
        assertThat(done.get("detected").get("emails")).isEmpty();
        assertThat(done.get("attachedPerson").get("id").asText()).isEqualTo(existing.get("id").asText());
        post(consultant, "/api/v1/cv-intake/" + itemId + "/attach",
                "{\"personId\":\"" + existing.get("id").asText() + "\"}", 409);
        get(consultant, "/api/v1/cv-intake/" + itemId + "/content", 404);

        // second item: create a new person from it; a taken e-mail gives 409 DUPLICATE
        String second = batch.get("items").get(1).get("id").asText();
        JsonNode clash = post(consultant, "/api/v1/cv-intake/" + second + "/attach",
                "{\"person\":{\"name\":\"Yeni\",\"type\":\"CANDIDATE\",\"email\":\"" + email + "\"}}", 409);
        assertThat(clash.get("existingId").asText()).isEqualTo(existing.get("id").asText());
        post(consultant, "/api/v1/cv-intake/" + second + "/attach", "{}", 400);
        JsonNode created = post(consultant, "/api/v1/cv-intake/" + second + "/attach",
                "{\"person\":{\"name\":\"" + unique("Yeni ") + "\",\"type\":\"CANDIDATE\"}}", 201);
        assertThat(created.get("person").get("sourceChannel").asText()).isEqualTo("CV_UPLOAD");
        assertThat(created.get("person").get("currentCv").get("id").asText())
                .isEqualTo(created.get("file").get("id").asText());

        // discard: 204 once, then 409; DSR deletion path
        JsonNode third = intakeUpload(manager, 201, part("d.txt", text("Üçüncü " + email + " " + word())))
                .get("items").get(0);
        worker.runOnce();
        assertThat(intake.findPendingByEmail(email)).hasSize(1);
        assertThat(intake.discardPendingByEmail(email)).isEqualTo(1);
        assertThat(intake.findPendingByEmail(email)).isEmpty();
        assertThat(get(manager, "/api/v1/cv-intake/" + third.get("id").asText(), 200).get("status").asText())
                .isEqualTo("DISCARDED");
        delete(manager, "/api/v1/cv-intake/" + third.get("id").asText(), 409);
        JsonNode fourth = intakeUpload(manager, 201, part("e.txt", text("Dördüncü " + word()))).get("items").get(0);
        delete(manager, "/api/v1/cv-intake/" + fourth.get("id").asText(), 204);
        intakeUpload(consultant, 400); // no files
    }

    @Test
    void pendingItemsExpireAfterFourteenDays() throws Exception {
        JsonNode item = intakeUpload(consultant, 201, part("old.txt", text("Eski " + word()))).get("items").get(0);
        String id = item.get("id").asText();
        assertThat(item.get("expiresAt").asText()).isNotEmpty();
        jdbc.update("update cv_intake_item set expires_at = now() - interval '1 day' where id = ?::uuid", id);
        intake.expire();
        get(consultant, "/api/v1/cv-intake/" + id, 404);
    }
}
