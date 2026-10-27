package com.recruiting.platform.accounts.domain;

import com.recruiting.platform.contracts.CvIntakeStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CvIntakeItemRepository extends JpaRepository<CvIntakeItem, UUID>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<CvIntakeItem> {

    List<CvIntakeItem> findByBatchId(UUID batchId);

    List<CvIntakeItem> findByStatusOrderByUploadedAt(CvIntakeStatus status);

    List<CvIntakeItem> findByStatusAndExpiresAtBefore(CvIntakeStatus status, Instant cutoff);

    List<CvIntakeItem> findByAttachedPersonId(UUID personId);

    /**
     * PENDING items whose detected e-mails contain {@code emailLower} (already lower-cased by the caller; the
     * extractor stores detected e-mails in lower case). Served by the GIN index {@code idx_cv_intake_detected}.
     * Used by {@code CvIntakeApi.findPendingByEmail} for data-subject requests.
     */
    @Query(value = "SELECT * FROM cv_intake_item WHERE status = 'PENDING' "
            + "AND detected @> jsonb_build_object('emails', jsonb_build_array(CAST(:email AS text)))",
            nativeQuery = true)
    List<CvIntakeItem> findPendingByDetectedEmail(@Param("email") String emailLower);

    List<CvIntakeItem> findTop10ByStatusAndTextStatusOrderByUploadedAt(CvIntakeStatus status,
            com.recruiting.platform.contracts.TextExtractionStatus textStatus);

    List<CvIntakeItem> findByStatusInAndUploadedAtBefore(java.util.Collection<CvIntakeStatus> statuses, Instant cutoff);
}
