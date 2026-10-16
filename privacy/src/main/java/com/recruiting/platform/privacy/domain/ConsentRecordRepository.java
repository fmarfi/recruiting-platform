package com.recruiting.platform.privacy.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentRecordRepository extends JpaRepository<ConsentRecord, UUID> {

    List<ConsentRecord> findByPersonIdOrderByRecordedAtDesc(UUID personId);
}
