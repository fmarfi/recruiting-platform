package com.recruiting.platform.security.audit.app;

import com.recruiting.platform.security.CurrentUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

/** Fills created_by / updated_by with the logged-in user (null for system jobs). */
@Component
class SecurityAuditorAware implements AuditorAware<UUID> {

    @Override
    public Optional<UUID> getCurrentAuditor() {
        return CurrentUser.currentId();
    }
}
