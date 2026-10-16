package com.recruiting.platform.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * ADR 0009: deleting a person (retention action DELETE, S-03) must succeed and remove every row about the person,
 * except the data-subject request, which stays with {@code person_id = null}.
 */
class PersonDeleteIT extends AbstractPersistenceIT {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void deletingAPersonCascadesToTheirActivitiesAndKeepsTheDataSubjectRequest() {
        UUID user = newUser().getId();
        UUID account = UUID.randomUUID();
        UUID person = UUID.randomUUID();
        UUID personOnlyActivity = UUID.randomUUID();
        UUID accountAndPersonActivity = UUID.randomUUID();
        UUID accountOnlyActivity = UUID.randomUUID();
        UUID nextAction = UUID.randomUUID();
        UUID dsr = UUID.randomUUID();

        jdbc.update("INSERT INTO account (id, name, sector, owner_id) VALUES (?, ?, 'Retail', ?)",
                account, unique("Fictional Co"), user);
        jdbc.update("INSERT INTO person (id, name, type, owner_id) VALUES (?, 'Deleted Person', 'CANDIDATE', ?)",
                person, user);
        jdbc.update("INSERT INTO activity (id, type, note, occurred_at, person_id, user_id) "
                + "VALUES (?, 'CALL', 'Called the person', now(), ?, ?)", personOnlyActivity, person, user);
        jdbc.update("INSERT INTO activity (id, type, note, occurred_at, account_id, person_id, user_id) "
                + "VALUES (?, 'MEETING', 'Met at the client', now(), ?, ?, ?)",
                accountAndPersonActivity, account, person, user);
        jdbc.update("INSERT INTO activity (id, type, note, occurred_at, account_id, user_id) "
                + "VALUES (?, 'VISIT', 'Visited the client', now(), ?, ?)", accountOnlyActivity, account, user);
        jdbc.update("INSERT INTO next_action (id, title, due_on, owner_id, person_id) "
                + "VALUES (?, 'Call back', current_date, ?, ?)", nextAction, user, person);
        jdbc.update("INSERT INTO data_subject_request (id, person_id, requester_name, requester_email, type, due_on) "
                + "VALUES (?, ?, 'Deleted Person', 'deleted.person@example.test', 'DELETION', current_date + 30)",
                dsr, person);

        int deleted = jdbc.update("DELETE FROM person WHERE id = ?", person);

        assertThat(deleted).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM activity WHERE id IN (?, ?)", personOnlyActivity,
                accountAndPersonActivity)).isZero();
        assertThat(count("SELECT count(*) FROM activity WHERE id = ?", accountOnlyActivity)).isOne();
        assertThat(count("SELECT count(*) FROM next_action WHERE id = ?", nextAction)).isZero();
        assertThat(jdbc.queryForObject("SELECT person_id FROM data_subject_request WHERE id = ?", UUID.class, dsr))
                .isNull();
    }

    @Test
    void everyForeignKeyToPersonCascadesExceptTheDataSubjectRequest() {
        Map<String, String> rules = deleteRules("person");
        assertThat(rules).containsEntry("data_subject_request.person_id", "n");
        assertThat(rules.entrySet().stream().filter(e -> !e.getKey().startsWith("data_subject_request.")))
                .isNotEmpty()
                .allSatisfy(e -> assertThat(e.getValue()).as(e.getKey()).isEqualTo("c"));
    }

    @Test
    void noForeignKeyToAccountIsSetNullOrCascade() {
        // Accounts are never hard-deleted (closed by status), so references stay NO ACTION.
        assertThat(deleteRules("account").values()).isNotEmpty().allMatch("a"::equals);
    }

    private Map<String, String> deleteRules(String referencedTable) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT c.conrelid::regclass::text || '.' || a.attname AS fk, c.confdeltype::text AS rule
                FROM pg_constraint c
                JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = c.conkey[1]
                WHERE c.contype = 'f' AND c.confrelid = ?::regclass
                """, referencedTable);
        return rows.stream().collect(Collectors.toMap(r -> (String) r.get("fk"), r -> (String) r.get("rule")));
    }

    private int count(String sql, Object... args) {
        Integer n = jdbc.queryForObject(sql, Integer.class, args);
        return n == null ? 0 : n;
    }
}
