package com.recruiting.platform.pipeline.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Spring Data repository for {@link Placement}. */
public interface PlacementRepository extends JpaRepository<Placement, UUID>, JpaSpecificationExecutor<Placement> {
    Optional<Placement> findByApplicationId(UUID applicationId);

    List<Placement> findByAccountId(UUID accountId);

    List<Placement> findByJobOrderId(UUID jobOrderId);
}
