package com.recruiting.platform.persistence;

import com.recruiting.platform.accounts.domain.Person;
import com.recruiting.platform.accounts.domain.PersonRepository;
import com.recruiting.platform.contracts.PersonType;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** Seed helpers shared by the Wave 2b repository tests. Data is committed, so every value is unique. */
abstract class Wave2bSupport extends AbstractPersistenceIT {

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected PersonRepository people;

    /** A person saved through JPA (Envers writes its revision). */
    protected UUID person(UUID owner) {
        Person p = new Person();
        p.setName(unique("Person"));
        p.setType(PersonType.CANDIDATE);
        p.setOwnerId(owner);
        return tx.execute(s -> people.save(p)).getId();
    }

    protected UUID account(UUID owner) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO account (id, name, sector, owner_id) VALUES (?, ?, 'Retail', ?)",
                id, unique("Fictional Co"), owner);
        return id;
    }

    protected UUID jobOrder(UUID account, UUID owner) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO job_order (id, account_id, title, owner_id) VALUES (?, ?, 'CFO', ?)",
                id, account, owner);
        return id;
    }

    protected UUID application(UUID jobOrder, UUID person, String stage) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO application (id, job_order_id, person_id, stage) VALUES (?, ?, ?, ?)",
                id, jobOrder, person, stage);
        return id;
    }

    protected int count(String sql, Object... args) {
        Integer n = jdbc.queryForObject(sql, Integer.class, args);
        return n == null ? 0 : n;
    }

    protected static String sha(char c) {
        return String.valueOf(c).repeat(64);
    }
}
