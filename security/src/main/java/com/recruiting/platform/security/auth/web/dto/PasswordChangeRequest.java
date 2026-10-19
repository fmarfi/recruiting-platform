package com.recruiting.platform.security.auth.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Contract schema PasswordChangeRequest (new password: 12 to 200 characters, ADR 0007). */
public record PasswordChangeRequest(@NotBlank String currentPassword, @NotBlank @Size(min = 12, max = 200) String newPassword) {
}
