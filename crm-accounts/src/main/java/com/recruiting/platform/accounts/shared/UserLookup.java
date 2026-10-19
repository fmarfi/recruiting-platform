package com.recruiting.platform.accounts.shared;

import com.recruiting.platform.accounts.shared.dto.UserRef;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Looks up user names and ids. TEMPORARY adapter: reads {@code app_user} directly because security's
 * {@code UserDirectory} is not published yet (ADR 0001). Replace the bodies with UserDirectory calls; callers stay.
 */
@Component
public class UserLookup {

    private final JdbcClient jdbc;

    public UserLookup(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Map<UUID, UserRef> refs(Collection<UUID> ids) {
        Map<UUID, UserRef> out = new HashMap<>();
        var wanted = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (wanted.isEmpty()) {
            return out;
        }
        jdbc.sql("SELECT id, name FROM app_user WHERE id IN (:ids)").param("ids", wanted)
                .query((rs, n) -> new UserRef(rs.getObject("id", UUID.class), rs.getString("name")))
                .list().forEach(u -> out.put(u.id(), u));
        return out;
    }

    public boolean exists(UUID id) {
        return jdbc.sql("SELECT count(*) FROM app_user WHERE id = :id AND active").param("id", id)
                .query(Long.class).single() > 0;
    }

    public Optional<UUID> idByEmail(String email) {
        return jdbc.sql("SELECT id FROM app_user WHERE lower(email) = lower(:e) AND active").param("e", email)
                .query(UUID.class).optional();
    }
}
