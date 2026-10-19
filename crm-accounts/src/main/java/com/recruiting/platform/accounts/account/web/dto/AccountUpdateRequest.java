package com.recruiting.platform.accounts.account.web.dto;

import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.hibernate.validator.constraints.URL;

/** PATCH body: absent field = unchanged, null = clear where nullable (see PatchBinder). */
public record AccountUpdateRequest(@Size(min = 1, max = 200) String name,
        @Size(min = 1, max = 100) String sector,
        @Size(max = 100) String city,
        @URL @Size(max = 300) String website,
        @Size(max = 4000) String notes,
        UUID ownerId,
        Long version) {
}
