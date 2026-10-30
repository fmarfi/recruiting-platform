package com.recruiting.platform.pipeline.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Spring Data repository for {@link FeeScheduleItem}. */
public interface FeeScheduleItemRepository extends JpaRepository<FeeScheduleItem, UUID>, JpaSpecificationExecutor<FeeScheduleItem> {
    List<FeeScheduleItem> findByAgreementIdOrderBySeqAsc(UUID agreementId);
}
