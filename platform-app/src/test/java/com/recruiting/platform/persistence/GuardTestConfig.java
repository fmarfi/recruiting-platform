package com.recruiting.platform.persistence;

import com.recruiting.platform.accounts.OffLimitsApi;
import com.recruiting.platform.contracts.OffLimitsReason;
import com.recruiting.platform.contracts.ShareBlockReason;
import com.recruiting.platform.pipeline.ShareGuard;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test doubles for the SPIs that other modules implement (ShareGuard by privacy, OffLimitsApi by accounts) until
 * they are merged. Everything is allowed unless a test blocks a person; tests use unique person ids.
 */
@TestConfiguration
public class GuardTestConfig {

    /** Blocks people by id with a reason. */
    public static class StubShareGuard implements ShareGuard {
        private final Map<UUID, ShareBlockReason> blocked = new ConcurrentHashMap<>();

        public void block(UUID person, ShareBlockReason reason) {
            blocked.put(person, reason);
        }

        public void unblock(UUID person) {
            blocked.remove(person);
        }

        @Override
        public ShareDecision check(UUID personId) {
            ShareBlockReason r = blocked.get(personId);
            return r == null ? ShareDecision.allowed() : ShareDecision.blocked(r);
        }
    }

    /** Off-limits rules by person id. */
    public static class StubOffLimits implements OffLimitsApi {
        private final Map<UUID, Match> rules = new ConcurrentHashMap<>();

        public void put(UUID person, String company) {
            rules.put(person, new Match(UUID.randomUUID(), null, company, OffLimitsReason.CLIENT_AGREEMENT));
        }

        public void clear(UUID person) {
            rules.remove(person);
        }

        @Override
        public List<Match> match(UUID personId, UUID ownAccountId) {
            Match m = rules.get(personId);
            return m == null ? List.of() : List.of(m);
        }
    }

    @Bean
    @Primary
    StubShareGuard stubShareGuard() {
        return new StubShareGuard();
    }

    @Bean
    @Primary
    StubOffLimits stubOffLimits() {
        return new StubOffLimits();
    }
}
