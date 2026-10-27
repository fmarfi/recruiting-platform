package com.recruiting.platform.privacy.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ConsentRequestRepository
        extends JpaRepository<ConsentRequest, UUID>, JpaSpecificationExecutor<ConsentRequest> {

    Optional<ConsentRequest> findByTokenHash(String tokenHash);

    List<ConsentRequest> findByPersonId(UUID personId);

    Optional<ConsentRequest> findFirstByPersonIdOrderByQueuedAtDesc(UUID personId);

    /** True when a request to this person has been waiting for an answer since after the given time. */
    boolean existsByPersonIdAndRespondedAtIsNullAndQueuedAtAfter(UUID personId, Instant after);

    /** An unanswered request with a link that is still valid; failed deliveries are not counted. */
    boolean existsByPersonIdAndRespondedAtIsNullAndExpiresAtAfterAndDeliveryStatusNot(UUID personId, Instant now,
            com.recruiting.platform.contracts.EmailDeliveryStatus status);

    @org.springframework.data.jpa.repository.Query("select count(r) > 0 from ConsentRequest r where r.personId = :personId "
            + "and r.reminderForRetainUntil = :retainUntil and r.reminderDaysBefore = :days")
    boolean reminderExists(@org.springframework.data.repository.query.Param("personId") UUID personId,
            @org.springframework.data.repository.query.Param("retainUntil") java.time.LocalDate retainUntil,
            @org.springframework.data.repository.query.Param("days") Short days);
}
