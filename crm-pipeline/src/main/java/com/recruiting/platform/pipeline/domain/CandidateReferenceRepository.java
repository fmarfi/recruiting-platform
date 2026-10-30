package com.recruiting.platform.pipeline.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Spring Data repository for {@link CandidateReference}. */
public interface CandidateReferenceRepository extends JpaRepository<CandidateReference, UUID>, JpaSpecificationExecutor<CandidateReference> {
    List<CandidateReference> findByPersonId(UUID personId);

    List<CandidateReference> findByRefereeEmailIgnoreCase(String refereeEmail);
}
