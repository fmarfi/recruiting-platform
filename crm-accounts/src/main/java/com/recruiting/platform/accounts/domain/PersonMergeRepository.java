package com.recruiting.platform.accounts.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PersonMergeRepository extends JpaRepository<PersonMerge, UUID> {

    List<PersonMerge> findBySurvivorId(UUID survivorId);

    /** Purge step 0: the merged ids of a survivor, read before anything is deleted. */
    @Query("SELECT m.mergedId FROM PersonMerge m WHERE m.survivorId = :survivorId")
    List<UUID> findMergedIds(@Param("survivorId") UUID survivorId);
}
