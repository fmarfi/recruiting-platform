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

    /** True when the user exists and is active (valid as an owner). */
    @Transactional(readOnly = true)
    public boolean isActive(UUID id) {
        return id != null && users.findById(id).map(AppUser::isActive).orElse(false);
    }

    /** Id of the active user with this e-mail (case-insensitive), for imports that name owners by e-mail. */
    @Transactional(readOnly = true)
    public Optional<UUID> activeIdByEmail(String email) {
        return email == null ? Optional.empty()
                : users.findByEmailIgnoreCase(email.trim()).filter(AppUser::isActive).map(AppUser::getId);
    }

    private static UserRef toRef(AppUser user) {
        return new UserRef(user.getId(), user.getName());
    }
}
