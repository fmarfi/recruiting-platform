package com.recruiting.platform.pipeline.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.recruiting.platform.contracts.PipelineStage;
import com.recruiting.platform.contracts.JobOrderStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    List<Application> findByJobOrderId(UUID jobOrderId);

    List<Application> findByPersonId(UUID personId);

    boolean existsByJobOrderIdAndPersonId(UUID jobOrderId, UUID personId);

    /** Rows of {stage, count} for the stage counts of one job order. */
    @Query("SELECT a.stage, COUNT(a) FROM Application a WHERE a.jobOrderId = :jobOrderId GROUP BY a.stage")
    List<Object[]> countByStage(@Param("jobOrderId") UUID jobOrderId);

    Optional<Application> findByJobOrderIdAndPersonId(UUID jobOrderId, UUID personId);

    boolean existsByJobOrderIdAndStage(UUID jobOrderId, PipelineStage stage);

    List<Application> findByPersonIdOrderByStageChangedAtDesc(UUID personId);

    /** Rows of {jobOrderId, stage, count} for the stage counts of several job orders. */
    @Query("SELECT a.jobOrderId, a.stage, COUNT(a) FROM Application a WHERE a.jobOrderId IN :ids "
            + "GROUP BY a.jobOrderId, a.stage")
    List<Object[]> countByStageForJobOrders(@Param("ids") Collection<UUID> ids);

    /** Rows of {stage, count} over the job orders of an account that have one of the given statuses. */
    @Query("SELECT a.stage, COUNT(a) FROM Application a, JobOrder j WHERE a.jobOrderId = j.id "
            + "AND j.accountId = :accountId AND j.status IN :statuses GROUP BY a.stage")
    List<Object[]> countByStageForAccount(@Param("accountId") UUID accountId,
            @Param("statuses") Collection<JobOrderStatus> statuses);

    /** Applications in the given stage over all job orders of an account. */
    @Query("SELECT COUNT(a) FROM Application a, JobOrder j WHERE a.jobOrderId = j.id "
            + "AND j.accountId = :accountId AND a.stage = :stage")
    long countByAccountAndStage(@Param("accountId") UUID accountId, @Param("stage") PipelineStage stage);

    /** Candidates still in process (not placed, not rejected) on job orders with the given statuses. */
    @Query("SELECT COUNT(a) FROM Application a, JobOrder j WHERE a.jobOrderId = j.id "
            + "AND j.status IN :statuses AND a.stage NOT IN :done")
    long countInProcess(@Param("statuses") Collection<JobOrderStatus> statuses,
            @Param("done") Collection<PipelineStage> done);

    long countByStageAndStageChangedAtGreaterThanEqual(PipelineStage stage, Instant since);

    /** Free text of a person is replaced on anonymisation (PersonAnonymised). */
    @Modifying
    @Query("UPDATE Application a SET a.rejectionReason = '[removed]' "
            + "WHERE a.personId = :personId AND a.rejectionReason IS NOT NULL")
    int scrubRejectionReason(@Param("personId") UUID personId);

    /** Removes the Envers history rows of the applications of a person (schema.md, Anonymisation). */
    @Modifying
    @Query(value = "DELETE FROM application_aud WHERE person_id = :personId", nativeQuery = true)
    int purgeAudit(@Param("personId") UUID personId);
}
