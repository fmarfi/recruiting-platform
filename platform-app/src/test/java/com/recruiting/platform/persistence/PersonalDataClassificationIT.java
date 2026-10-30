package com.recruiting.platform.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Plan rule (section 4, review 1.3): every column that can hold personal data is classified in
 * contracts/personal-data.md. The test reads the live schema after all migrations and fails with the list of
 * {@code table.column} that has no row, names stale rows, and checks the consistency rules of that document.
 */
class PersonalDataClassificationIT extends AbstractPersistenceIT {

    private static final Set<String> FIXED_SCOPE = Set.of("person", "account", "access_log", "timeline_entry",
            "import_job", "cv_intake_item", "person_merge", "client_report", "export_job");
    private static final Set<String> FK_TARGETS = Set.of("person", "contact", "application", "activity",
            "person_file");

    /**
     * Optimistic-lock columns named differently from the shared {@code version} (person_file.version is the CV
     * version), classified as the shared audit columns. Change request to the architect: list them in the document.
     */
    private static final Set<String> SHARED_ALIASES = Set.of("person_file.row_version");

    private record Row(String table, List<String> columns, String klass, String erasure, String audit) {}

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void everyColumnInScopeIsClassifiedAndTheDocumentIsConsistent() throws IOException {
        List<Row> rows = parse();
        assertThat(rows).isNotEmpty();

        Set<String> classified = new HashSet<>();
        Set<String> shared = new HashSet<>();
        for (Row r : rows) {
            for (String c : r.columns()) {
                if (r.table().equals("*")) {
                    shared.add(c);
                } else {
                    classified.add(r.table() + "." + c);
                }
            }
        }

        // 1. tables in scope
        Set<String> scope = new LinkedHashSet<>(FIXED_SCOPE);
        scope.addAll(jdbc.queryForList("""
                SELECT DISTINCT c.conrelid::regclass::text FROM pg_constraint c
                WHERE c.contype = 'f' AND c.confrelid::regclass::text = ANY (?)""", String.class,
                (Object) FK_TARGETS.toArray(String[]::new)));
        scope.removeIf(t -> t.endsWith("_aud"));

        // 2. every column of a table in scope has a row
        List<String> unclassified = new ArrayList<>();
        for (String table : scope) {
            for (String col : jdbc.queryForList("""
                    SELECT column_name FROM information_schema.columns
                    WHERE table_schema = 'public' AND table_name = ? ORDER BY ordinal_position""", String.class,
                    table)) {
                if (!shared.contains(col) && !classified.contains(table + "." + col)
                        && !SHARED_ALIASES.contains(table + "." + col)) {
                    unclassified.add(table + "." + col);
                }
            }
        }
        assertThat(unclassified).as("columns with no row in contracts/personal-data.md").isEmpty();

        // 3. no stale rows
        List<String> stale = new ArrayList<>();
        for (String tc : classified) {
            String[] p = tc.split("\\.");
            Integer n = jdbc.queryForObject("""
                    SELECT count(*) FROM information_schema.columns
                    WHERE table_schema = 'public' AND table_name = ? AND column_name = ?""", Integer.class, p[0], p[1]);
            if (n == null || n == 0) {
                stale.add(tc);
            }
        }
        assertThat(stale).as("rows naming a column that does not exist").isEmpty();

        // 4. consistency
        List<String> inconsistent = new ArrayList<>();
        for (Row r : rows) {
            boolean personal = List.of("PD", "SPD", "TP").contains(r.klass());
            if (personal && r.audit().startsWith("HIST")) {
                inconsistent.add(r.table() + " " + r.columns() + ": personal class with Audit HIST");
            }
            if (personal && r.erasure().startsWith("KEEP / KEEP") && !r.erasure().contains(";")) {
                inconsistent.add(r.table() + " " + r.columns() + ": KEEP / KEEP without a reason after ';'");
            }
        }
        assertThat(inconsistent).isEmpty();
    }

    private static List<Row> parse() throws IOException {
        Path file = Path.of("..", "contracts", "personal-data.md");
        if (!Files.exists(file)) {
            file = Path.of("contracts", "personal-data.md");
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<Row> rows = new ArrayList<>();
        boolean inColumns = false;
        for (String line : lines) {
            if (line.startsWith("## ")) {
                inColumns = line.trim().equals("## Columns");
                continue;
            }
            if (!inColumns || !line.startsWith("|")) {
                continue;
            }
            String[] cells = Arrays.stream(line.substring(1).split("\\|", -1)).map(String::trim)
                    .toArray(String[]::new);
            if (cells.length < 6 || cells[0].equals("Table") || cells[0].startsWith("---")) {
                continue;
            }
            List<String> cols = Arrays.stream(cells[1].split(",")).map(String::trim).filter(s -> !s.isEmpty())
                    .toList();
            rows.add(new Row(cells[0], cols, cells[2], cells[3], cells[4]));
        }
        return rows;
    }
}
