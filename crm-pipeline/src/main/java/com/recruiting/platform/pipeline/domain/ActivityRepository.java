package com.recruiting.platform.pipeline.domain;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityRepository extends JpaRepository<Activity, UUID>, JpaSpecificationExecutor<Activity> {

    Page<Activity> findByAccountIdOrderByOccurredAtDesc(UUID accountId, Pageable pageable);

    Page<Activity> findByPersonIdOrderByOccurredAtDesc(UUID personId, Pageable pageable);

    /** All activities of a person, newest first (person export). */
    List<Activity> findByPersonIdOrderByOccurredAtDesc(UUID personId);

    /** Account ids with the latest activity, newest first (My day, recent accounts). */
    @Query("SELECT a.accountId FROM Activity a WHERE a.accountId IS NOT NULL "
            + "GROUP BY a.accountId ORDER BY MAX(a.occurredAt) DESC")
    List<UUID> recentAccountIds(Pageable pageable);

    /** Free text of a person is replaced on anonymisation (PersonAnonymised). */
    @Modifying
    @Query("UPDATE Activity a SET a.note = '[removed]' WHERE a.personId = :personId")
    int scrubNotes(@Param("personId") UUID personId);

    /** Removes the Envers history rows of the activities of a person (schema.md, Anonymisation). */
    @Modifying
    @Query(value = "DELETE FROM activity_aud WHERE person_id = :personId", nativeQuery = true)
    int purgeAudit(@Param("personId") UUID personId);
}
