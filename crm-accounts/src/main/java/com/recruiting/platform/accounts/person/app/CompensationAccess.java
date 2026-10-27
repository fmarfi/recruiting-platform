package com.recruiting.platform.accounts.person.app;

import com.recruiting.platform.accounts.domain.Person;
import com.recruiting.platform.contracts.Role;
import com.recruiting.platform.security.CurrentUser;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Who may read or write compensation: MANAGER, ADMIN and the relationship owner of the person (ADR 0011). */
@Component
public class CompensationAccess {

    private final CurrentUser currentUser;

    public CompensationAccess(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    public boolean allowed(UUID ownerId) {
        return currentUser.idOrEmpty().map(id -> currentUser.hasAtLeast(Role.MANAGER) || id.equals(ownerId))
                .orElse(false);
    }

    public boolean allowed(Person person) {
        return allowed(person.getOwnerId());
    }
}
