package com.recruiting.platform.security;

import com.recruiting.platform.security.domain.AppUser;
import com.recruiting.platform.security.domain.AppUserRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Public API of the security module: resolve user ids to {@link UserRef}s. */
@Component
public class UserDirectory {

    private final AppUserRepository users;

    UserDirectory(AppUserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public Optional<UserRef> ref(UUID id) {
        return id == null ? Optional.empty() : users.findById(id).map(UserDirectory::toRef);
    }

    @Transactional(readOnly = true)
    public Map<UUID, UserRef> refs(Collection<UUID> ids) {
        Map<UUID, UserRef> result = new HashMap<>();
        if (ids == null || ids.isEmpty()) {
            return result;
        }
        for (AppUser user : users.findAllById(ids.stream().filter(Objects::nonNull).distinct().toList())) {
            result.put(user.getId(), toRef(user));
        }
        return result;
    }

    private static UserRef toRef(AppUser user) {
        return new UserRef(user.getId(), user.getName());
    }
}
