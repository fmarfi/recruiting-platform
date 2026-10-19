package com.recruiting.platform.accounts.shared;

import java.util.UUID;
import org.hibernate.id.uuid.UuidVersion7Strategy;

/** Event header helpers (contracts/events.md rule 5): UUID v7 event ids. */
public final class Events {

    private Events() {
    }

    public static UUID newId() {
        return UuidVersion7Strategy.INSTANCE.generateUuid(null);
    }
}
