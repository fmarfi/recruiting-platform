package com.recruiting.platform.security;

import com.recruiting.platform.contracts.Role;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Public API of the security module: who is logged in. Other modules inject this bean instead of touching
 * Spring Security. Outside a request (jobs, event listeners) there is no user: {@link #idOrEmpty()} is empty.
 */
@Component
public class CurrentUser {

    /** Id of the logged-in user, empty for system jobs and anonymous requests. */
    public Optional<UUID> idOrEmpty() {
        return currentId();
    }

    /** Id of the logged-in user; throws when nobody is logged in. */
    public UUID id() {
        return currentId().orElseThrow(() -> new IllegalStateException("No authenticated user"));
    }

    /** Highest role of the logged-in user, empty when nobody is logged in. */
    public Optional<Role> roleOrEmpty() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        Role best = null;
        for (var authority : auth.getAuthorities()) {
            String name = authority.getAuthority();
            if (name.startsWith("ROLE_")) {
                try {
                    Role role = Role.valueOf(name.substring(5));
                    if (best == null || role.ordinal() > best.ordinal()) {
                        best = role;
                    }
                } catch (IllegalArgumentException ignored) {
                    // not one of our roles
                }
            }
        }
        return Optional.ofNullable(best);
    }

    /** Role of the logged-in user; throws when nobody is logged in. */
    public Role role() {
        return roleOrEmpty().orElseThrow(() -> new IllegalStateException("No authenticated user"));
    }

    /** True when the user has the given role or a higher one (ADMIN > MANAGER > CONSULTANT). */
    public boolean hasAtLeast(Role minimum) {
        return roleOrEmpty().map(r -> r.ordinal() >= minimum.ordinal()).orElse(false);
    }

    /** Static variant for code that is not a Spring bean (Envers listener, auditing). */
    public static Optional<UUID> currentId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof IdentifiedUser identified) {
            return Optional.of(identified.userId());
        }
        return Optional.empty();
    }
}
