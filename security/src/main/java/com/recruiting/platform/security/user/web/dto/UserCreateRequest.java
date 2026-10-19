package com.recruiting.platform.security.user.web.dto;

import com.recruiting.platform.contracts.Language;
import com.recruiting.platform.contracts.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Contract schema UserCreateRequest. */
public record UserCreateRequest(@NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 120) String name, @NotNull Role role, Language language,
        @NotBlank @Size(min = 12, max = 200) String initialPassword) {
}
