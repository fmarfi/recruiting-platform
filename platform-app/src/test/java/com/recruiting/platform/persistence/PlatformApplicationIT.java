package com.recruiting.platform.persistence;

import org.junit.jupiter.api.Test;

/**
 * Context-loads smoke test. It runs on PostgreSQL (Testcontainers) because the feature modules now have services
 * and controllers that need the repositories; the former database-free "test" profile can no longer start them.
 */
class PlatformApplicationIT extends AbstractPersistenceIT {

    @Test
    void contextLoads() {
    }
}
