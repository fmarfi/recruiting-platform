package com.recruiting.platform.accounts.shared;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolves the current user from the Spring Security context. TEMPORARY: security-privacy will publish
 * {@code CurrentUser}; until then the id is taken from the authentication name when that is a UUID.
 * "Elevated" means MANAGER or ADMIN (Lifecycle.requiresElevatedRole, ADR 0007).
 */
@Component
public class ActorResolver {

    public Actor current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return new Actor(null, false);
        }
        UUID id = null;
        try {
            id = UUID.fromString(auth.getName());
        } catch (IllegalArgumentException ignored) {
            // not a user id
        }
        boolean elevated = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_MANAGER".equals(a.getAuthority()) || "ROLE_ADMIN".equals(a.getAuthority()));
        return new Actor(id, elevated);
    }
}
