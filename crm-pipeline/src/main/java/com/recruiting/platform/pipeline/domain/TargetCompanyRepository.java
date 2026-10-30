package com.recruiting.platform.pipeline.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Spring Data repository for {@link TargetCompany}. */
public interface TargetCompanyRepository extends JpaRepository<TargetCompany, UUID>, JpaSpecificationExecutor<TargetCompany> {
    List<TargetCompany> findByJobOrderId(UUID jobOrderId);

    long countByJobOrderId(UUID jobOrderId);
}
