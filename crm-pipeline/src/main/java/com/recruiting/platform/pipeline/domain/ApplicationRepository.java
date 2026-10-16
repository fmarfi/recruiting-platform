package com.recruiting.platform.pipeline.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    List<Application> findByJobOrderId(UUID jobOrderId);

    List<Application> findByPersonId(UUID personId);

    boolean existsByJobOrderIdAndPersonId(UUID jobOrderId, UUID personId);

    /** Rows of {stage, count} for the stage counts of one job order. */
    @Query("SELECT a.stage, COUNT(a) FROM Application a WHERE a.jobOrderId = :jobOrderId GROUP BY a.stage")
    List<Object[]> countByStage(@Param("jobOrderId") UUID jobOrderId);
}
