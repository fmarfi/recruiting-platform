package com.recruiting.platform.accounts.account.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.hibernate.validator.constraints.URL;

/** Body of POST /accounts. */
public record AccountCreateRequest(@NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 100) String sector,
        @Size(max = 100) String city,
        @URL @Size(max = 300) String website,
        @Size(max = 4000) String notes,
        UUID ownerId) {
}
