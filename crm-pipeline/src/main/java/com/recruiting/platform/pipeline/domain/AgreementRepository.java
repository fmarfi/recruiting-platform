package com.recruiting.platform.pipeline.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Spring Data repository for {@link Agreement}. */
public interface AgreementRepository extends JpaRepository<Agreement, UUID>, JpaSpecificationExecutor<Agreement> {
    List<Agreement> findByAccountId(UUID accountId);

    List<Agreement> findByJobOrderId(UUID jobOrderId);
}
