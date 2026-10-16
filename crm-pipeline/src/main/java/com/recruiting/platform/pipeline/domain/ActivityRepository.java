package com.recruiting.platform.pipeline.domain;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {

    Page<Activity> findByAccountIdOrderByOccurredAtDesc(UUID accountId, Pageable pageable);

    Page<Activity> findByPersonIdOrderByOccurredAtDesc(UUID personId, Pageable pageable);
}
