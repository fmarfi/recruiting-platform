package com.recruiting.platform.pipeline.shared;

import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ErrorCode;
import java.util.UUID;
import org.springframework.security.core.Authentication;

/**
 * The calling user: id and whether the role is MANAGER or ADMIN (for manager-only moves, ADR 0007).
 * The id is read from the authentication name; this is the single place to switch to the security module's
 * {@code CurrentUser} once it is published.
 *
 * @param id       user id
 * @param elevated {@code true} for MANAGER and ADMIN
 */
public record Actor(UUID id, boolean elevated) {

    /**
     * Builds the actor from the Spring Security authentication.
     *
     * @param auth the authentication of the request
     * @return the actor
     * @throws ApiException UNAUTHENTICATED when there is no usable authentication
     */
    public static Actor of(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED, "Not logged in.");
        }
        UUID id;
        try {
            id = UUID.fromString(auth.getName());
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED, "Not logged in.");
        }
        boolean elevated = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER") || a.getAuthority().equals("ROLE_ADMIN"));
        return new Actor(id, elevated);
    }
}
