package com.recruiting.platform.pipeline.shared.ref;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Port to the accounts module's people (the network). The adapter wraps {@code PersonApi}. */
public interface PersonDirectory {

    /**
     * Looks up people by id.
     *
     * @param ids person ids
     * @return the people that exist, by id
     */
    Map<UUID, PersonInfo> people(Collection<UUID> ids);

    /**
     * Candidate picker: people not in {@code excludeIds}, candidates and executives first.
     *
     * @param q          optional free-text filter
     * @param excludeIds people already on the job order
     * @param pageable   paging
     * @return a page of people
     */
    Page<PersonInfo> suggestions(String q, Collection<UUID> excludeIds, Pageable pageable);
}
