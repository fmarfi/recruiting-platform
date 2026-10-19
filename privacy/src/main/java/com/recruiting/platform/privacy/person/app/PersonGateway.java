package com.recruiting.platform.privacy.person.app;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Everything privacy asks of the person records in accounts. One port so the module does not reach into another
 * module's tables from many places. The shipped adapter is {@link JdbcPersonGateway}; swap it for one built on
 * accounts' {@code PersonApi} / {@code PersonDataApi} when those are merged (see hand-off).
 */
public interface PersonGateway {

    Optional<PersonInfo> find(UUID id);

    Map<UUID, PersonInfo> findAll(Collection<UUID> ids);

    /** People that have no person_privacy row yet (seeded or imported without the PersonCreated event). */
    List<UUID> idsWithoutPrivacy(int limit);

    /** Replaces personal fields with placeholders (ADR 0009 step 2) and announces PersonAnonymised(deleted=false). */
    void anonymise(UUID personId, UUID actorId);

    /** Removes the person (ADR 0009 step 2) and announces PersonAnonymised(deleted=true). */
    void delete(UUID personId, UUID actorId);
}
