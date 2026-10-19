package com.recruiting.platform.accounts.contact.web.dto;

import com.recruiting.platform.accounts.person.web.dto.PersonCreateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Body of POST /accounts/{id}/contacts: exactly one of personId and person. */
public record ContactCreateRequest(UUID personId, @Valid PersonCreateRequest person, @Size(max = 200) String role, Boolean primary) {
}
