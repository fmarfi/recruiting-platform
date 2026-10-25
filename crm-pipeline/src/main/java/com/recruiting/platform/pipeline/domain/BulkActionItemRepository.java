package com.recruiting.platform.pipeline.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface BulkActionItemRepository extends JpaRepository<BulkActionItem, BulkActionItem.Key> {

    List<BulkActionItem> findByBulkActionId(UUID bulkActionId);

    List<BulkActionItem> findByEntityId(UUID entityId);
}
