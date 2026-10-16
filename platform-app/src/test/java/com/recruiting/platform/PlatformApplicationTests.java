package com.recruiting.platform;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Context-loads smoke test. Runs on the "test" profile (application-test.yml),
 * which switches off DataSource/JPA/Flyway so no database is needed.
 * Tests that need PostgreSQL use Testcontainers and must not activate the "test" profile.
 */
@SpringBootTest
@ActiveProfiles("test")
class PlatformApplicationTests {

    @Test
    void contextLoads() {
    }
}
