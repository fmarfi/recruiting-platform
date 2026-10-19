package com.recruiting.platform.accounts.contact.web.dto;

import jakarta.validation.constraints.Size;

/** PATCH body for a contact. */
public record ContactUpdateRequest(@Size(max = 200) String role, Boolean primary) {
}
