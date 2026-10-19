package com.recruiting.platform.security.user.web.dto;

import com.recruiting.platform.contracts.Language;
import com.recruiting.platform.contracts.Role;
import jakarta.validation.constraints.Size;

/** Contract schema UserUpdateRequest; only the sent fields change. */
public record UserUpdateRequest(@Size(min = 1, max = 120) String name, Role role, Boolean active,
        Language language, Long version) {
}
