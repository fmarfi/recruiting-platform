package com.recruiting.platform.pipeline.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NextActionRepository extends JpaRepository<NextAction, UUID> {

    List<NextAction> findByOwnerIdAndDoneFalseOrderByDueOn(UUID ownerId);

    boolean existsBySourceEventId(UUID sourceEventId);
}
