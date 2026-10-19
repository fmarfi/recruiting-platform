package com.recruiting.platform.pipeline.shared.ref;

import java.util.UUID;

/** A user as shown in {@code UserRef}. */
public record UserInfo(UUID id, String name) {
}
