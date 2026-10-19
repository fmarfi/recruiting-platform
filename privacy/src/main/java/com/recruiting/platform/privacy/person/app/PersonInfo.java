package com.recruiting.platform.privacy.person.app;

import com.recruiting.platform.contracts.PersonType;
import java.time.Instant;
import java.util.UUID;

/** What privacy needs to know about a person (owned by accounts). */
public record PersonInfo(UUID id, String name, String email, PersonType type, String title, String company,
        String city, UUID ownerId, Instant lastContactAt, Instant createdAt, boolean anonymised) {

    /** First name only, for the public consent page. */
    public String firstName() {
        String trimmed = name == null ? "" : name.trim();
        int space = trimmed.indexOf(' ');
        return space < 0 ? trimmed : trimmed.substring(0, space);
    }
}
