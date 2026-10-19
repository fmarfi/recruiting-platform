package com.recruiting.platform.privacy.domain;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RetentionRunRepository extends JpaRepository<RetentionRun, UUID> {

    Page<RetentionRun> findAllByOrderByStartedAtDesc(Pageable pageable);
}
