package com.recruiting.platform.persistence;

import com.recruiting.platform.contracts.Language;
import com.recruiting.platform.contracts.Role;
import com.recruiting.platform.security.domain.AppUser;
import com.recruiting.platform.security.domain.AppUserRepository;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Base for repository tests: full application context on a real PostgreSQL (pgvector image) with all Flyway
 * migrations applied and {@code ddl-auto: validate}. Tests do not use the "test" profile (that one has no database).
 * Data is committed, so tests use unique values instead of cleaning up.
 */
@SpringBootTest
@Import(PostgresTestConfig.class)
abstract class AbstractPersistenceIT {

    @Autowired
    protected TransactionTemplate tx;

    @Autowired
    private AppUserRepository users;

    protected static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    protected AppUser newUser() {
        return tx.execute(status -> {
            AppUser u = new AppUser();
            u.setEmail(unique("user") + "@example.test");
            u.setName("Test User");
            u.setRole(Role.CONSULTANT);
            u.setActive(true);
            u.setLanguage(Language.TR);
            u.setMustChangePassword(true);
            return users.save(u);
        });
    }
}
