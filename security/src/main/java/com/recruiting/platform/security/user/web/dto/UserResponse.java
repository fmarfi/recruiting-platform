package com.recruiting.platform.security.user.web.dto;

import com.recruiting.platform.contracts.Language;
import com.recruiting.platform.contracts.Role;
import com.recruiting.platform.security.domain.AppUser;
import java.time.Instant;
import java.util.UUID;

/** Contract schema User. */
public record UserResponse(UUID id, String email, String name, Role role, boolean active, Language language,
        Instant lastLoginAt, Instant createdAt, long version) {

    public static UserResponse from(AppUser u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getName(), u.getRole(), u.isActive(), u.getLanguage(),
                u.getLastLoginAt(), u.getCreatedAt(), u.getVersion() == null ? 0 : u.getVersion());
    }
}
