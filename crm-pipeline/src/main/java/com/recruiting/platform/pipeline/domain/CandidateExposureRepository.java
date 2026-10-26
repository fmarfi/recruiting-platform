package com.recruiting.platform.pipeline.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateExposureRepository extends JpaRepository<CandidateExposure, UUID> {

    /** Exposure tab and merge: newest first. */
    List<CandidateExposure> findByPersonIdOrderByPresentedAtDesc(UUID personId);

    /** Re-presenting warning: has this person already been shown to this account? */
    boolean existsByPersonIdAndAccountId(UUID personId, UUID accountId);

    boolean existsByApplicationIdAndChannel(UUID applicationId, com.recruiting.platform.contracts.ExposureChannel channel);
}
