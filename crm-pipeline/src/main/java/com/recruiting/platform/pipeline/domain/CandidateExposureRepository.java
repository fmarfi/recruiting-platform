package com.recruiting.platform.pipeline.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateExposureRepository extends JpaRepository<CandidateExposure, UUID> {

    /** Merge: exposures of the merged person move to the survivor (ids and dates only, not audited). */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE CandidateExposure e SET e.personId = :to WHERE e.personId = :from")
    int movePerson(@org.springframework.data.repository.query.Param("from") UUID from,
            @org.springframework.data.repository.query.Param("to") UUID to);

    /** Merge: exposures of a removed duplicate application point at the survivor application. */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE CandidateExposure e SET e.applicationId = :to WHERE e.applicationId = :from")
    int moveApplication(@org.springframework.data.repository.query.Param("from") UUID from,
            @org.springframework.data.repository.query.Param("to") UUID to);

    /** Exposure tab and merge: newest first. */
    List<CandidateExposure> findByPersonIdOrderByPresentedAtDesc(UUID personId);

    /** Re-presenting warning: has this person already been shown to this account? */
    boolean existsByPersonIdAndAccountId(UUID personId, UUID accountId);

    boolean existsByApplicationIdAndChannel(UUID applicationId, com.recruiting.platform.contracts.ExposureChannel channel);
}
