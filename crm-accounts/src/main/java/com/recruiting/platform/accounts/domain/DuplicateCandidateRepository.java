package com.recruiting.platform.accounts.domain;

import com.recruiting.platform.contracts.DuplicateStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DuplicateCandidateRepository extends JpaRepository<DuplicateCandidate, UUID> {

    Optional<DuplicateCandidate> findByPersonAIdAndPersonBId(UUID personAId, UUID personBId);

    Page<DuplicateCandidate> findByStatusOrderByDetectedAtDesc(DuplicateStatus status, Pageable pageable);

    @Query("SELECT d FROM DuplicateCandidate d WHERE d.personAId = :id OR d.personBId = :id")
    List<DuplicateCandidate> findByPerson(@Param("id") UUID personId);
}
