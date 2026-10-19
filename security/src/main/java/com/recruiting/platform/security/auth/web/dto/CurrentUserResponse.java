package com.recruiting.platform.security.auth.web.dto;

import com.recruiting.platform.contracts.Language;
import com.recruiting.platform.contracts.Role;
import com.recruiting.platform.security.domain.AppUser;
import java.util.UUID;

/** Contract schema CurrentUser. */
public record CurrentUserResponse(UUID id, String email, String name, Role role, Language language,
        boolean mustChangePassword) {

    public static CurrentUserResponse from(AppUser user) {
        return new CurrentUserResponse(user.getId(), user.getEmail(), user.getName(), user.getRole(),
                user.getLanguage(), user.isMustChangePassword());
    }
}
