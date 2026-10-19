package com.recruiting.platform.accounts.shared;

import java.util.UUID;

/** The user behind a request. {@code id} may be null until security publishes its CurrentUser API. */
public record Actor(UUID id, boolean elevated) {

    public static Actor system() {
        return new Actor(null, true);
    }
}
