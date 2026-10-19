package com.recruiting.platform.security.user.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Contract schema PasswordResetRequest. */
public record PasswordResetRequest(@NotBlank @Size(min = 12, max = 200) String initialPassword) {
}
