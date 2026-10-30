package com.recruiting.platform.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruiting.platform.contracts.*;
import com.recruiting.platform.contracts.AccessAction;
import com.recruiting.platform.contracts.AccountStatus;
import com.recruiting.platform.contracts.ActivityType;
import com.recruiting.platform.contracts.ConsentChannel;
import com.recruiting.platform.contracts.ConsentDecision;
import com.recruiting.platform.contracts.ConsentRequestPurpose;
import com.recruiting.platform.contracts.CvIntakeStatus;
import com.recruiting.platform.contracts.DuplicateMatchReason;
import com.recruiting.platform.contracts.DuplicateStatus;
import com.recruiting.platform.contracts.ExposureChannel;
import com.recruiting.platform.contracts.Mobility;
import com.recruiting.platform.contracts.OffLimitsReason;
import com.recruiting.platform.contracts.PersonFileKind;
import com.recruiting.platform.contracts.SourceChannel;
import com.recruiting.platform.contracts.TextExtractionStatus;
import com.recruiting.platform.contracts.ConsentStatus;
import com.recruiting.platform.contracts.DataSubjectRequestStatus;
import com.recruiting.platform.contracts.DataSubjectRequestType;
import com.recruiting.platform.contracts.EmailDeliveryStatus;
import com.recruiting.platform.contracts.EntityType;
import com.recruiting.platform.contracts.ImportKind;
import com.recruiting.platform.contracts.ImportStatus;
import com.recruiting.platform.contracts.JobOrderStatus;
import com.recruiting.platform.contracts.Language;
import com.recruiting.platform.contracts.LinkType;
import com.recruiting.platform.contracts.PersonType;
import com.recruiting.platform.contracts.PipelineStage;
import com.recruiting.platform.contracts.ProposalStatus;
import com.recruiting.platform.contracts.RetentionAction;
import com.recruiting.platform.contracts.RetentionBasis;
import com.recruiting.platform.contracts.RetentionStatus;
import com.recruiting.platform.contracts.Role;
import com.recruiting.platform.contracts.TimelineKind;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Runs every Flyway migration on an empty database (the context start already validates all entities against the
 * schema with ddl-auto: validate) and checks the schema against contracts/enums and schema.md.
 */
class MigrationIT extends AbstractPersistenceIT {

    private static final Pattern LITERAL = Pattern.compile("'([A-Z][A-Z0-9_]*)'");

    @Autowired
    private JdbcTemplate jdbc;

    private record EnumColumn(String table, String column, Class<? extends Enum<?>> type) {}

    private static final List<EnumColumn> ENUM_COLUMNS = List.of(
            new EnumColumn("app_user", "role", Role.class),
            new EnumColumn("app_user", "language", Language.class),
            new EnumColumn("access_log", "action", AccessAction.class),
            new EnumColumn("access_log", "entity_type", EntityType.class),
            new EnumColumn("account", "status", AccountStatus.class),
            new EnumColumn("person", "type", PersonType.class),
            new EnumColumn("person", "consent_status", ConsentStatus.class),
            new EnumColumn("person", "retention_status", RetentionStatus.class),
            new EnumColumn("network_link", "type", LinkType.class),
            new EnumColumn("import_job", "kind", ImportKind.class),
            new EnumColumn("import_job", "status", ImportStatus.class),
            new EnumColumn("job_order", "status", JobOrderStatus.class),
            new EnumColumn("application", "stage", PipelineStage.class),
            new EnumColumn("proposal", "status", ProposalStatus.class),
            new EnumColumn("activity", "type", ActivityType.class),
            new EnumColumn("timeline_entry", "kind", TimelineKind.class),
            new EnumColumn("timeline_entry", "entity_type", EntityType.class),
            new EnumColumn("person_privacy", "consent_status", ConsentStatus.class),
            new EnumColumn("person_privacy", "retention_status", RetentionStatus.class),
            new EnumColumn("privacy_notice", "language", Language.class),
            new EnumColumn("consent_record", "decision", ConsentDecision.class),
            new EnumColumn("consent_record", "channel", ConsentChannel.class),
            new EnumColumn("consent_request", "language", Language.class),
            new EnumColumn("consent_request", "delivery_status", EmailDeliveryStatus.class),
            new EnumColumn("consent_request", "response", ConsentDecision.class),
            new EnumColumn("retention_settings", "basis", RetentionBasis.class),
            new EnumColumn("retention_settings", "action", RetentionAction.class),
            new EnumColumn("data_subject_request", "type", DataSubjectRequestType.class),
            new EnumColumn("data_subject_request", "status", DataSubjectRequestStatus.class),
            // 1.2.0
            new EnumColumn("person", "source_channel", SourceChannel.class),
            new EnumColumn("person", "mobility", Mobility.class),
            new EnumColumn("person_file", "kind", PersonFileKind.class),
            new EnumColumn("person_file", "text_status", TextExtractionStatus.class),
            new EnumColumn("cv_intake_item", "status", CvIntakeStatus.class),
            new EnumColumn("cv_intake_item", "text_status", TextExtractionStatus.class),
            new EnumColumn("cv_intake_item", "source_channel", SourceChannel.class),
            new EnumColumn("duplicate_candidate", "status", DuplicateStatus.class),
            new EnumColumn("off_limits_rule", "reason", OffLimitsReason.class),
            new EnumColumn("candidate_exposure", "channel", ExposureChannel.class),
            new EnumColumn("consent_request", "purpose", ConsentRequestPurpose.class),
            // 1.3.0 (schema.md line 19)
            new EnumColumn("account", "archive_reason", ArchiveReason.class),
            new EnumColumn("account", "einvoice_status", EInvoiceStatus.class),
            new EnumColumn("account", "employee_band", EmployeeBand.class),
            new EnumColumn("account", "confidentiality_level", ConfidentialityLevel.class),
            new EnumColumn("account", "preferred_language", Language.class),
            new EnumColumn("person", "archive_reason", ArchiveReason.class),
            new EnumColumn("person", "function_area", JobFunction.class),
            new EnumColumn("person", "seniority", Seniority.class),
            new EnumColumn("job_order", "archive_reason", ArchiveReason.class),
            new EnumColumn("job_order", "confidentiality_level", ConfidentialityLevel.class),
            new EnumColumn("tag_definition", "colour", TagColour.class),
            new EnumColumn("application", "outreach_status", OutreachStatus.class),
            new EnumColumn("application", "close_reason", CloseReason.class),
            new EnumColumn("activity", "outcome", ActivityOutcome.class),
            new EnumColumn("activity", "direction", ActivityDirection.class),
            new EnumColumn("activity", "source", ActivitySource.class),
            new EnumColumn("client_feedback", "outcome", ClientFeedbackOutcome.class),
            new EnumColumn("client_feedback", "decline_reason", DeclineReason.class),
            new EnumColumn("candidate_evaluation", "interview_type", InterviewType.class),
            new EnumColumn("candidate_evaluation", "recommendation", Recommendation.class),
            new EnumColumn("interview_note", "interview_type", InterviewType.class),
            new EnumColumn("candidate_reference", "status", ReferenceStatus.class),
            new EnumColumn("search_brief", "seniority_level", Seniority.class),
            new EnumColumn("search_brief", "function_area", JobFunction.class),
            new EnumColumn("search_criterion", "kind", CriterionKind.class),
            new EnumColumn("job_order_team_member", "role", SearchTeamRole.class),
            new EnumColumn("target_company", "tier", TargetTier.class),
            new EnumColumn("target_company", "status", TargetCompanyStatus.class),
            new EnumColumn("agreement", "kind", AgreementKind.class),
            new EnumColumn("agreement", "status", AgreementStatus.class),
            new EnumColumn("agreement", "language", Language.class),
            new EnumColumn("agreement", "exclusivity", Exclusivity.class),
            new EnumColumn("agreement", "fee_model", FeeModel.class),
            new EnumColumn("agreement", "guarantee_start_basis", GuaranteeStartBasis.class),
            new EnumColumn("fee_schedule_item", "trigger", FeeTrigger.class),
            new EnumColumn("fee_schedule_item", "invoice_state", InvoiceState.class),
            new EnumColumn("placement", "status", PlacementStatus.class),
            new EnumColumn("conflict_check", "decision", ConflictDecision.class),
            new EnumColumn("export_job", "entity", ListEntity.class),
            new EnumColumn("export_job", "status", ExportStatus.class),
            new EnumColumn("client_report", "kind", ReportKind.class),
            new EnumColumn("client_report", "language", Language.class),
            new EnumColumn("import_job_record", "entity_type", EntityType.class));

    @Test
    void extensionsAndSearchHelperExist() {
        List<String> ext = jdbc.queryForList("SELECT extname FROM pg_extension", String.class);
        assertThat(ext).contains("unaccent", "pg_trgm").doesNotContain("vector");
        String volatility = jdbc.queryForObject(
                "SELECT provolatile::text FROM pg_proc WHERE proname = 'immutable_unaccent'", String.class);
        assertThat(volatility).isEqualTo("i");
        assertThat(jdbc.queryForObject("SELECT immutable_unaccent(lower('Öztürk'))", String.class))
                .isEqualTo("ozturk");
    }

    @Test
    void checkConstraintsMatchTheContractEnums() {
        org.assertj.core.api.SoftAssertions soft = new org.assertj.core.api.SoftAssertions();
        for (EnumColumn ec : ENUM_COLUMNS) {
            List<String> defs = jdbc.queryForList(
                    "SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE contype = 'c' AND conrelid = ?::regclass",
                    String.class, ec.table());
            String def = defs.stream()
                    .filter(d -> d.contains("(" + ec.column() + ")::text = ANY"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no CHECK on " + ec.table() + "." + ec.column()));
            Set<String> actual = new TreeSet<>();
            Matcher m = LITERAL.matcher(def);
            while (m.find()) {
                actual.add(m.group(1));
            }
            Set<String> expected = new TreeSet<>();
            Arrays.stream(ec.type().getEnumConstants()).forEach(e -> expected.add(e.name()));
            soft.assertThat(actual).as("%s.%s", ec.table(), ec.column()).isEqualTo(expected);
        }
        soft.assertAll();
    }

    @Test
    void everyAuditedTableHasAnAuditTable() {
        List<String> audited = List.of("app_user", "account", "person", "person_employment", "contact", "network_link",
                "job_order", "application", "proposal", "activity", "next_action", "person_privacy",
                "retention_settings", "data_subject_request", "person_education", "person_file", "off_limits_rule",
                // 1.3.0
                "activity_attendee", "tag_definition", "client_feedback", "candidate_evaluation", "interview_note",
                "candidate_reference", "search_brief", "search_criterion", "job_order_team_member", "target_company",
                "agreement", "fee_schedule_item", "placement");
        for (String table : audited) {
            Integer n = jdbc.queryForObject(
                    "SELECT count(*) FROM information_schema.tables WHERE table_name = ?", Integer.class, table + "_aud");
            assertThat(n).as(table + "_aud").isEqualTo(1);
        }
    }

    @Test
    void settingsRowAndEventRegistryExist() {
        assertThat(jdbc.queryForObject("SELECT retention_months FROM retention_settings WHERE id = 1", Integer.class))
                .isEqualTo(24);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM event_publication", Integer.class)).isZero();
    }

    @Test
    void arrayCheckConstraintsMatchTheContractEnums() {
        assertThat(arrayCheckLiterals("duplicate_candidate", "ck_duplicate_reasons"))
                .isEqualTo(names(DuplicateMatchReason.class));
        assertThat(arrayCheckLiterals("candidate_evaluation", "ck_candidate_evaluation_motivation_tags"))
                .isEqualTo(names(MotivationTag.class));
        assertThat(arrayCheckLiterals("candidate_evaluation", "ck_candidate_evaluation_report_blocks"))
                .isEqualTo(names(EvaluationBlock.class));
        assertThat(arrayCheckLiterals("agreement", "ck_agreement_fee_basis"))
                .isEqualTo(names(FeeBasisComponent.class));
    }

    @Test
    void contract13IndexesAndConstraintsExist() {
        List<String> indexes = jdbc.queryForList("SELECT indexname FROM pg_indexes WHERE schemaname = 'public'",
                String.class);
        assertThat(indexes).contains("uq_account_normalized_name", "idx_account_aliases_norm",
                "idx_application_job_close_reason", "uq_activity_external_message_id", "uq_placement_application",
                "idx_candidate_reference_referee_email", "idx_agreement_signatory", "idx_person_employment_organisation");
        assertThat(jdbc.queryForObject(
                "SELECT indexdef FROM pg_indexes WHERE indexname = 'uq_account_normalized_name'", String.class))
                .contains("UNIQUE").contains("archived_at IS NULL");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pg_indexes WHERE indexname = 'uq_account_name'",
                Integer.class)).isZero();
        // normalized_name is generated, so it is not in account_aud
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.columns
                WHERE table_name = 'account_aud' AND column_name = 'normalized_name'""", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("""
                SELECT is_generated FROM information_schema.columns
                WHERE table_name = 'account' AND column_name = 'normalized_name'""", String.class))
                .isEqualTo("ALWAYS");
        // the 1.3.0 set-null FKs
        List<String> setNull = jdbc.queryForList("""
                SELECT c.conrelid::regclass::text || '.' || a.attname
                FROM pg_constraint c JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = c.conkey[1]
                WHERE c.contype = 'f' AND c.confdeltype = 'n'""", String.class);
        assertThat(setNull).contains("agreement.signatory_contact_id", "placement.person_id",
                "placement.application_id", "candidate_evaluation.application_id", "interview_note.application_id",
                "candidate_exposure.application_id", "data_subject_request.person_id");
    }

    @Test
    void retentionBasisDefaultIsFromLastConsent() {
        String def = jdbc.queryForObject("""
                SELECT column_default FROM information_schema.columns
                WHERE table_name = 'retention_settings' AND column_name = 'basis'""", String.class);
        assertThat(def).contains("FROM_LAST_CONSENT");
        assertThat(jdbc.queryForObject("SELECT renewal_reminder_days::text FROM retention_settings WHERE id = 1",
                String.class)).isEqualTo("{60,14}");
    }

    @Test
    void turkishTextSearchConfigurationFoldsAccentsAndStems() {
        assertThat(jdbc.queryForObject(
                "SELECT to_tsvector('tr_unaccent', 'Mühendis İstanbul')::text", String.class))
                .doesNotContain("ü");
        assertThat(jdbc.queryForObject(
                "SELECT to_tsvector('tr_unaccent', 'Şişli müdürü') @@ plainto_tsquery('tr_unaccent', 'sisli')",
                Boolean.class)).isTrue();
    }

    @Test
    void everyPersonForeignKeyCascadesInclusiveOfNewTables() {
        List<String> fks = jdbc.queryForList("""
                SELECT c.conrelid::regclass::text || '.' || a.attname
                FROM pg_constraint c JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = c.conkey[1]
                WHERE c.contype = 'f' AND c.confrelid = 'person'::regclass AND c.confdeltype = 'c'""", String.class);
        assertThat(fks).contains("person_education.person_id", "person_file.person_id",
                "cv_intake_item.attached_person_id", "duplicate_candidate.person_a_id",
                "duplicate_candidate.person_b_id", "person_merge.survivor_id", "candidate_exposure.person_id");
    }

    private static Set<String> names(Class<? extends Enum<?>> type) {
        Set<String> s = new TreeSet<>();
        Arrays.stream(type.getEnumConstants()).forEach(e -> s.add(e.name()));
        return s;
    }

    private Set<String> arrayCheckLiterals(String table, String constraint) {
        String def = jdbc.queryForObject(
                "SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conrelid = ?::regclass AND conname = ?",
                String.class, table, constraint);
        Set<String> actual = new TreeSet<>();
        Matcher m = LITERAL.matcher(def);
        while (m.find()) {
            actual.add(m.group(1));
        }
        return actual;
    }
}
