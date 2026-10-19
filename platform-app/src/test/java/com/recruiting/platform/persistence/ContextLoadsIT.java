package com.recruiting.platform.persistence;

import org.junit.jupiter.api.Test;

/**
 * Context-loads smoke test on a real PostgreSQL. It replaces the database-less PlatformApplicationTests: feature
 * beans now need the JPA repositories, so the context cannot start without a database.
 */
class ContextLoadsIT extends AbstractPersistenceIT {

    @Test
    void contextLoads() {
        // the context starting is the assertion
    }
}
