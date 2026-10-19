package com.recruiting.platform.pipeline.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NextActionRepository extends JpaRepository<NextAction, UUID>, JpaSpecificationExecutor<NextAction> {

    List<NextAction> findByOwnerIdAndDoneFalseOrderByDueOn(UUID ownerId);

    boolean existsBySourceEventId(UUID sourceEventId);

    Optional<NextAction> findFirstByAccountIdAndDoneFalseOrderByDueOnAsc(UUID accountId);

    List<NextAction> findByOwnerIdAndDoneFalseAndDueOnLessThanEqualOrderByDueOnAsc(UUID ownerId, LocalDate until,
            Pageable pageable);

    long countByOwnerIdAndDoneFalseAndDueOnBefore(UUID ownerId, LocalDate today);

    /** Free text of a person is replaced on anonymisation (PersonAnonymised). */
    @Modifying
    @Query("UPDATE NextAction n SET n.title = '[removed]' WHERE n.personId = :personId")
    int scrubTitles(@Param("personId") UUID personId);

    /** Removes the Envers history rows of the next actions of a person (schema.md, Anonymisation). */
    @Modifying
    @Query(value = "DELETE FROM next_action_aud WHERE person_id = :personId", nativeQuery = true)
    int purgeAudit(@Param("personId") UUID personId);
}
