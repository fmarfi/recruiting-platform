package com.recruiting.platform.security;

import java.util.UUID;

/** Id and display name of a user, as other modules show owners and actors. */
public record UserRef(UUID id, String name) {
}
