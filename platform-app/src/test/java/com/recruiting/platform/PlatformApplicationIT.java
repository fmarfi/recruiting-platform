package com.recruiting.platform;

import com.recruiting.platform.persistence.PostgresTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Context-loads smoke test on a real PostgreSQL (Testcontainers). Since the security and privacy modules own
 * repositories and the filter chain, the old database-free "test" profile can no longer start the context.
 * Runs with {@code mvn verify -Pintegration}.
 */
@SpringBootTest
@Import(PostgresTestConfig.class)
class PlatformApplicationIT {

    @Test
    void contextLoads() {
    }
}
