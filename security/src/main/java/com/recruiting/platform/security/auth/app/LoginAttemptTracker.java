package com.recruiting.platform.security.auth.app;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Brute-force protection (ADR 0007): 10 failed logins lock the e-mail for 15 minutes. In memory (one instance in
 * the pilot); a restart clears it. Unknown e-mails are counted too, so the lock does not reveal which exist.
 */
@Component
class LoginAttemptTracker {

    static final int MAX_FAILURES = 10;
    static final Duration LOCK = Duration.ofMinutes(15);

    private record State(int failures, Instant lockedUntil) {
    }

    private final Map<String, State> states = new ConcurrentHashMap<>();
    private final Clock clock;

    LoginAttemptTracker() {
        this(Clock.systemUTC());
    }

    LoginAttemptTracker(Clock clock) {
        this.clock = clock;
    }

    boolean isLocked(String email) {
        State state = states.get(key(email));
        if (state == null || state.lockedUntil() == null) {
            return false;
        }
        if (clock.instant().isAfter(state.lockedUntil())) {
            states.remove(key(email));
            return false;
        }
        return true;
    }

    void failed(String email) {
        Instant now = clock.instant();
        states.merge(key(email), new State(1, null), (old, fresh) -> {
            int failures = old.failures() + 1;
            return new State(failures, failures >= MAX_FAILURES ? now.plus(LOCK) : null);
        });
    }

    void succeeded(String email) {
        states.remove(key(email));
    }

    private static String key(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
