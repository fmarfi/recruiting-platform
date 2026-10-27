package com.recruiting.platform.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruiting.platform.contracts.FileStore;
import com.recruiting.platform.contracts.RetentionAction;
import com.recruiting.platform.contracts.Role;
import com.recruiting.platform.privacy.dsr.app.PersonErasureService;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * ADR 0009 / schema.md steps 0 to 4 for the Wave 2b tables: after ANONYMISE and DELETE no personal data remains in
 * any new _aud table, in merged ids' history, in intake items or in the file store.
 */
@Import(PurgeWave2bIT.Stores.class)
class PurgeWave2bIT extends AbstractApiIT {

    /** Records deleted keys. */
    static final List<String> DELETED = new CopyOnWriteArrayList<>();

    @TestConfiguration
    static class Stores {
        @Bean
        FileStore fileStore() {
            return new FileStore() {
                @Override
                public void put(String key, InputStream content, long size, String contentType) {
                }

                @Override
                public Optional<InputStream> get(String key) {
                    return Optional.empty();
                }

                @Override
                public void delete(String key) throws IOException {
                    DELETED.add(key);
                }
            };
        }
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PersonErasureService erasure;

    private long rev(long ts) {
        Long r = jdbc.queryForObject("select nextval('revinfo_seq')", Long.class);
        jdbc.update("insert into revinfo (rev, revtstmp) values (?, ?)", r, ts);
        return r;
    }

    /** A person with history, a merged predecessor, files, education, intake items and an application. */
    private Fixture fixture(Client c, String tag) {
        String email = tag + "-" + UUID.randomUUID() + "@example.test";
        UUID p = PrivacyTestSupport.insertPerson(jdbc, c.userId(), "Wave Two " + tag, email);
        UUID merged = UUID.randomUUID();
        UUID edu = UUID.randomUUID();
        UUID file = UUID.randomUUID();
        String key = "person/" + p + "/" + file;
        jdbc.update("insert into person_education (id, person_id, school, degree, field, sort_order) "
                + "values (?, ?, 'Secret University', 'MSc', 'Physics', 0)", edu, p);
        jdbc.update("insert into person_file (id, person_id, kind, version, is_current, file_name, content_type, "
                + "size_bytes, sha256, storage_key, uploaded_by) values (?, ?, 'CV', 1, true, 'cv-wave-two.pdf', "
                + "'application/pdf', 100, ?, ?, ?)", file, p, "a".repeat(64), key, c.userId());
        long r1 = rev(1);
        long r2 = rev(2);
        jdbc.update("update person_aud set tags = '{vip}', phone_normalized = '+905551112233', "
                + "linkedin_url_normalized = 'https://www.linkedin.com/in/secret', notice_period_days = 30, "
                + "mobility = 'LOCAL', languages = '[{\"language\":\"tr\",\"level\":\"NATIVE\"}]' where id = ?", p);
        jdbc.update("insert into person_education_aud (id, rev, revtype, person_id, school, degree, field) "
                + "values (?, ?, 0, ?, 'Old School', 'BSc', 'Maths')", edu, r1, p);
        jdbc.update("update person_education_aud set revend = ? where id = ?", r2, edu);
        jdbc.update("insert into person_education_aud (id, rev, revtype, person_id, school, degree, field) "
                + "values (?, ?, 1, ?, 'Secret University', 'MSc', 'Physics')", edu, r2, p);
        jdbc.update("insert into person_file_aud (id, rev, revtype, person_id, kind, file_name, sha256, storage_key) "
                + "values (?, ?, 0, ?, 'CV', 'cv-wave-two.pdf', ?, ?)", file, r2, p, "a".repeat(64), key);
        // the merged predecessor: gone from person, history still holds its personal data
        jdbc.update("insert into person_aud (id, rev, revtype, name, email) values (?, ?, 0, 'Merged Mike', "
                + "'mike@example.test')", merged, r1);
        jdbc.update("insert into person_file_aud (id, rev, revtype, person_id, file_name) values (?, ?, 0, ?, "
                + "'mike-cv.pdf')", UUID.randomUUID(), r1, merged);
        jdbc.update("insert into person_merge (id, survivor_id, merged_id, fields_taken, merged_at, merged_by) "
                + "values (?, ?, ?, '{}', now(), ?)", UUID.randomUUID(), p, merged, c.userId());
        jdbc.update("insert into application_aud (id, rev, revtype, person_id, rejection_reason, "
                + "off_limits_override_reason) values (?, ?, 0, ?, 'too senior', 'client agreed')", UUID.randomUUID(),
                r2, p);
        // intake: one attached to the person, one pending that mentions the e-mail in upper case in the CV
        UUID attached = UUID.randomUUID();
        UUID pending = UUID.randomUUID();
        String intakeKey = "intake/" + pending;
        jdbc.update("insert into cv_intake_item (id, batch_id, status, file_name, sha256, storage_key, "
                + "extracted_text, detected, attached_person_id, uploaded_by, uploaded_at, expires_at) values "
                + "(?, ?, 'ATTACHED', 'attached.pdf', ?, 'intake/x" + attached + "', 'cv text', "
                + "cast(? as jsonb), ?, ?, now(), now() + interval '30 days')", attached, UUID.randomUUID(),
                "b".repeat(64), "{\"emails\":[\"" + email + "\"],\"phones\":[\"+905551112233\"],\"linkedinUrls\":[]}",
                p, c.userId());
        jdbc.update("insert into cv_intake_item (id, batch_id, status, file_name, sha256, storage_key, "
                + "extracted_text, detected, uploaded_by, uploaded_at, expires_at) values (?, ?, 'PENDING', "
                + "'pending.pdf', ?, ?, 'cv text', cast(? as jsonb), ?, now(), now() + interval '14 days')", pending,
                UUID.randomUUID(), "c".repeat(64), intakeKey, "{\"emails\":[\"" + email.toLowerCase()
                        + "\"],\"phones\":[],\"linkedinUrls\":[]}", c.userId());
        return new Fixture(p, merged, edu, file, key, attached, pending, intakeKey);
    }

    record Fixture(UUID person, UUID merged, UUID edu, UUID file, String key, UUID attached, UUID pending,
            String intakeKey) {
    }

    @Test
    void anonymiseLeavesNoPersonalDataInTheNewHistoryIntakeOrFileStore() throws Exception {
        Client c = login(Role.ADMIN);
        Fixture f = fixture(c, "anon");
        DELETED.clear();
        erasure.erase(f.person(), RetentionAction.ANONYMISE, c.userId());

        Map<String, Object> aud = jdbc.queryForMap("select name, email, tags, compensation_enc, notice_period_days, "
                + "mobility, cast(languages as text) as languages, phone_normalized, linkedin_url_normalized "
                + "from person_aud where id = ? and revend is null", f.person());
        assertThat(aud.get("name")).isEqualTo("Anonymised person");
        assertThat(aud.get("email")).isNull();
        assertThat(aud.get("tags").toString()).isEqualTo("{}");
        assertThat(aud.get("notice_period_days")).isNull();
        assertThat(aud.get("mobility")).isNull();
        assertThat(aud.get("languages")).isEqualTo("[]");
        assertThat(aud.get("phone_normalized")).isNull();
        assertThat(aud.get("linkedin_url_normalized")).isNull();
        assertThat(count("person_education_aud where person_id = ? and revend is not null", f.person())).isZero();
        assertThat(jdbc.queryForMap("select school, degree, field from person_education_aud where person_id = ?",
                f.person())).containsEntry("school", "[removed]").containsEntry("degree", null)
                .containsEntry("field", null);
        assertThat(jdbc.queryForMap("select file_name, sha256, storage_key from person_file_aud where person_id = ?",
                f.person())).containsEntry("file_name", "[removed]").containsEntry("sha256", null)
                .containsEntry("storage_key", null);
        assertThat(jdbc.queryForObject("select off_limits_override_reason from application_aud where person_id = ?",
                String.class, f.person())).isEqualTo("[removed]");
        // merged id's history is gone (step 0 ids), the survivor's stays minimised
        assertThat(count("person_aud where id = ?", f.merged())).isZero();
        assertThat(count("person_file_aud where person_id = ?", f.merged())).isZero();
        // intake: attached item minimised, pending item with the e-mail discarded
        Map<String, Object> attached = jdbc.queryForMap("select file_name, sha256, storage_key, extracted_text, "
                + "cast(detected as text) as detected from cv_intake_item where id = ?", f.attached());
        assertThat(attached.get("file_name")).isNull();
        assertThat(attached.get("sha256")).isNull();
        assertThat(attached.get("storage_key")).isNull();
        assertThat(attached.get("extracted_text")).isNull();
        assertThat(attached.get("detected").toString()).doesNotContain("@").doesNotContain("+90");
        Map<String, Object> pending = jdbc.queryForMap("select status, file_name, sha256, storage_key, "
                + "cast(detected as text) as detected from cv_intake_item where id = ?", f.pending());
        assertThat(pending.get("status")).isEqualTo("DISCARDED");
        assertThat(pending.get("file_name")).isNull();
        assertThat(pending.get("detected").toString()).doesNotContain("@");
        assertThat(DELETED).contains(f.key(), f.intakeKey());
    }

    @Test
    void deleteLeavesOnlyTheIdOnlyDeleteRow() throws Exception {
        Client c = login(Role.ADMIN);
        Fixture f = fixture(c, "del");
        DELETED.clear();
        erasure.erase(f.person(), RetentionAction.DELETE, c.userId());

        assertThat(count("person where id = ?", f.person())).isZero();
        assertThat(count("person_education where id = ?", f.edu())).isZero();
        assertThat(count("person_file where id = ?", f.file())).isZero();
        assertThat(count("cv_intake_item where id = ?", f.attached())).isZero();
        assertThat(count("person_merge where survivor_id = ?", f.person())).isZero();
        assertThat(count("person_aud where id = ? and revtype <> 2", f.person())).isZero();
        assertThat(count("person_aud where id = ? and revtype = 2", f.person())).isEqualTo(1);
        assertThat(count("person_education_aud where person_id = ?", f.person())).isZero();
        assertThat(count("person_file_aud where person_id = ?", f.person())).isZero();
        assertThat(count("application_aud where person_id = ?", f.person())).isZero();
        assertThat(count("person_aud where id = ?", f.merged())).isZero();
        assertThat(count("person_file_aud where person_id = ?", f.merged())).isZero();
        assertThat(jdbc.queryForObject("select status from cv_intake_item where id = ?", String.class, f.pending()))
                .isEqualTo("DISCARDED");
        assertThat(DELETED).contains(f.key(), f.intakeKey());
    }

    private int count(String fromWhere, UUID id) {
        return jdbc.queryForObject("select count(*) from " + fromWhere, Integer.class, id);
    }
}
