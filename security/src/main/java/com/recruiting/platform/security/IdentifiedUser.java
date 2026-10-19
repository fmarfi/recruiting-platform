package com.recruiting.platform.security;

import java.util.UUID;

/** Marker for the session principal that carries our user id (implemented in auth/app). */
public interface IdentifiedUser {

    UUID userId();
}
