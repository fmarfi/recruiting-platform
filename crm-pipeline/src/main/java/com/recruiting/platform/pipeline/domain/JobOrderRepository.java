package com.recruiting.platform.pipeline.domain;

import com.recruiting.platform.contracts.JobOrderStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface JobOrderRepository extends JpaRepository<JobOrder, UUID>, JpaSpecificationExecutor<JobOrder> {

    /** Row lock for every rank change (schema.md, application.rank). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT j FROM JobOrder j WHERE j.id = :id")
    Optional<JobOrder> lockById(@Param("id") UUID id);

    Page<JobOrder> findByAccountId(UUID accountId, Pageable pageable);

    Page<JobOrder> findByStatus(JobOrderStatus status, Pageable pageable);

    /** Accent- and case-insensitive title search (A-14), served by {@code idx_job_order_title_trgm}. */
    @Query(value = "SELECT * FROM job_order WHERE immutable_unaccent(lower(title)) LIKE '%' || immutable_unaccent(lower(:q)) || '%' "
            + "ORDER BY title LIMIT :limit", nativeQuery = true)
    List<JobOrder> searchByTitle(@Param("q") String q, @Param("limit") int limit);

    List<JobOrder> findByAccountIdAndStatusIn(UUID accountId, Collection<JobOrderStatus> statuses);

    List<JobOrder> findByOwnerIdAndStatusInOrderByOpenedOnDesc(UUID ownerId, Collection<JobOrderStatus> statuses,
            Pageable pageable);

    long countByStatusIn(Collection<JobOrderStatus> statuses);
}
