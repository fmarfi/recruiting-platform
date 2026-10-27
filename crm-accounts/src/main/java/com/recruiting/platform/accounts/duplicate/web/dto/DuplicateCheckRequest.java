package com.recruiting.platform.accounts.duplicate.web.dto;

import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Body of POST /duplicates/check; at least one member must be sent. */
public record DuplicateCheckRequest(@Size(max = 200) String name, @Size(max = 200) String company,
        @Size(max = 254) String email, @Size(max = 50) String phone, @Size(max = 300) String linkedinUrl,
        UUID excludePersonId) {

    public boolean isEmpty() {
        return blank(name) && blank(company) && blank(email) && blank(phone) && blank(linkedinUrl)
                && excludePersonId == null;
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
