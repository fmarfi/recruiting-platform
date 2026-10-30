package com.recruiting.platform.pipeline.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link SearchCriterion}. */
public interface SearchCriterionRepository extends JpaRepository<SearchCriterion, UUID> {
    List<SearchCriterion> findByJobOrderIdOrderByKindAscSortOrderAsc(UUID jobOrderId);
}
