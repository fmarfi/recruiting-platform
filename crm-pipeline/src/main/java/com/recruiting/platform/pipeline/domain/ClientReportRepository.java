package com.recruiting.platform.pipeline.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Spring Data repository for {@link ClientReport}. */
public interface ClientReportRepository extends JpaRepository<ClientReport, UUID>, JpaSpecificationExecutor<ClientReport> {
    List<ClientReport> findByJobOrderIdOrderByGeneratedAtDesc(UUID jobOrderId);

    /** Reports that include the person (erasure). */
    @org.springframework.data.jpa.repository.Query(value = "SELECT * FROM client_report WHERE person_ids @> ARRAY[:personId]::uuid[]", nativeQuery = true)
    List<ClientReport> findByPersonId(@org.springframework.data.repository.query.Param("personId") UUID personId);
}
