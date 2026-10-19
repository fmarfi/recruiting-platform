package com.recruiting.platform.pipeline.shared.ref;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Port to the security module's user directory ({@code UserDirectory} / {@code UserRef} lookup). */
public interface UserDirectory {

    /**
     * Looks up users by id.
     *
     * @param ids user ids
     * @return the users that exist, by id
     */
    Map<UUID, UserInfo> users(Collection<UUID> ids);
}
