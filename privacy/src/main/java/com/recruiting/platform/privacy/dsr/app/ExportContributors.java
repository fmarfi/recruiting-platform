package com.recruiting.platform.privacy.dsr.app;

import com.recruiting.platform.accounts.PersonExportContributor;
import com.recruiting.platform.privacy.person.app.PersonGateway;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Adds {@code files} (metadata only, no bytes or text) to GET /people/{id}/export (S-04, ADR 0011). */
@Component
class FilesExportContributor implements PersonExportContributor {

    private final JdbcTemplate jdbc;

    FilesExportContributor(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public String section() {
        return "files";
    }

    @Override
    public Object collect(UUID personId) {
        return jdbc.queryForList("select id, kind, version, is_current as \"isCurrent\", file_name as \"fileName\", "
                + "content_type as \"contentType\", size_bytes as \"sizeBytes\", created_at as \"uploadedAt\" "
                + "from person_file where person_id = ? order by kind, version desc", personId);
    }
}

/** Adds {@code exposure} (account, job order, time, channel) to the export: which clients saw this person. */
@Component
class ExposureExportContributor implements PersonExportContributor {

    private final JdbcTemplate jdbc;

    ExposureExportContributor(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public String section() {
        return "exposure";
    }

    @Override
    public Object collect(UUID personId) {
        return jdbc.queryForList("select e.id, e.account_id as \"accountId\", a.name as \"accountName\", "
                + "e.job_order_id as \"jobOrderId\", e.channel, e.presented_at as \"presentedAt\" "
                + "from candidate_exposure e join account a on a.id = e.account_id where e.person_id = ? "
                + "order by e.presented_at desc", personId);
    }
}

/** Adds {@code pendingIntake}: unresolved CV intake items that mention the person's e-mail (lower-cased match). */
@Component
class PendingIntakeExportContributor implements PersonExportContributor {

    private final JdbcTemplate jdbc;
    private final PersonGateway people;

    PendingIntakeExportContributor(JdbcTemplate jdbc, PersonGateway people) {
        this.jdbc = jdbc;
        this.people = people;
    }

    @Override
    public String section() {
        return "pendingIntake";
    }

    @Override
    public Object collect(UUID personId) {
        String email = people.find(personId).map(p -> p.email()).orElse(null);
        if (email == null || email.isBlank()) {
            return List.<Map<String, Object>>of();
        }
        return jdbc.queryForList("select id, file_name as \"fileName\", uploaded_at as \"uploadedAt\", "
                + "cast(detected as text) as detected from cv_intake_item where status = 'PENDING' "
                + "and detected @> jsonb_build_object('emails', jsonb_build_array(cast(? as text)))",
                email.trim().toLowerCase(java.util.Locale.ROOT));
    }
}
