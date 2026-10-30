package com.recruiting.platform.pipeline.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Spring Data repository for {@link CandidateEvaluation}. */
public interface CandidateEvaluationRepository extends JpaRepository<CandidateEvaluation, UUID>, JpaSpecificationExecutor<CandidateEvaluation> {
    List<CandidateEvaluation> findByPersonIdOrderByEvaluatedOnDesc(UUID personId);

    Optional<CandidateEvaluation> findByPersonIdAndJobOrderId(UUID personId, UUID jobOrderId);
}
