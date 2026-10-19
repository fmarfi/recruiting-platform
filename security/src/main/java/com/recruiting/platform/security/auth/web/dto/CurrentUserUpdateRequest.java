package com.recruiting.platform.security.auth.web.dto;

import com.recruiting.platform.contracts.Language;
import jakarta.validation.constraints.Size;

/** Contract schema CurrentUserUpdateRequest (at least one field). */
public record CurrentUserUpdateRequest(@Size(min = 1, max = 120) String name, Language language) {
}
