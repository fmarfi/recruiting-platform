package com.recruiting.platform.accounts.shared;

import com.recruiting.platform.contracts.Role;
import com.recruiting.platform.security.CurrentUser;
import java.util.UUID;

/**
 * The user behind a request. {@code id} is null for system jobs. "Elevated" means MANAGER or ADMIN
 * (Lifecycle.requiresElevatedRole, ADR 0007).
 */
public record Actor(UUID id, boolean elevated) {

    public static Actor system() {
        return new Actor(null, true);
    }

    public static Actor of(CurrentUser user) {
        return new Actor(user.idOrEmpty().orElse(null), user.hasAtLeast(Role.MANAGER));
    }
}
