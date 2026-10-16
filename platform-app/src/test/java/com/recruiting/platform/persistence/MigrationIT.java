package com.recruiting.platform.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruiting.platform.contracts.AccessAction;
import com.recruiting.platform.contracts.AccountStatus;
import com.recruiting.platform.contracts.ActivityType;
import com.recruiting.platform.contracts.ConsentChannel;
import com.recruiting.platform.contracts.ConsentDecision;
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

    private static final Pattern LITERAL = Pattern.compile("'([A-Z_]+)'");

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
            new EnumColumn("data_subject_request", "status", DataSubjectRequestStatus.class));

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
            assertThat(actual).as("%s.%s", ec.table(), ec.column()).isEqualTo(expected);
        }
    }

    @Test
    void everyAuditedTableHasAnAuditTable() {
        List<String> audited = List.of("app_user", "account", "person", "person_employment", "contact", "network_link",
                "job_order", "application", "proposal", "activity", "next_action", "person_privacy",
                "retention_settings", "data_subject_request");
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
}
