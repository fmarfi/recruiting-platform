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

    /** CV push (1.5.0): the exposure of one activity. */
    java.util.Optional<CandidateExposure> findByActivityId(UUID activityId);

    /** Changing {@code occurred_at} of a CV-push activity moves its {@code presented_at} (same transaction). */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE CandidateExposure e SET e.presentedAt = :at WHERE e.activityId = :activityId")
    int updatePresentedAt(@org.springframework.data.repository.query.Param("activityId") UUID activityId,
            @org.springframework.data.repository.query.Param("at") java.time.Instant at);

    /** Home personal numbers cvsSent (PIPELINE) and cvsPushed (CV_PUSH): range [from, to). */
    long countByChannelAndPresentedByAndPresentedAtGreaterThanEqualAndPresentedAtLessThan(
            com.recruiting.platform.contracts.ExposureChannel channel, UUID presentedBy, java.time.Instant from,
            java.time.Instant to);

    boolean existsByApplicationIdAndChannel(UUID applicationId, com.recruiting.platform.contracts.ExposureChannel channel);
}
