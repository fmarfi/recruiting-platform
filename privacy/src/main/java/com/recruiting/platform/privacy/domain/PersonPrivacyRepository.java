package com.recruiting.platform.privacy.domain;

import com.recruiting.platform.contracts.ConsentStatus;
import com.recruiting.platform.contracts.RetentionStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonPrivacyRepository extends JpaRepository<PersonPrivacy, UUID> {

    /** The row with a write lock, so two automatic e-mails for one person cannot both pass the pending check. */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from PersonPrivacy p where p.personId = :id")
    java.util.Optional<PersonPrivacy> lockById(@org.springframework.data.repository.query.Param("id") UUID id);

    List<PersonPrivacy> findByConsentStatusInAndRetentionStatusNot(Collection<ConsentStatus> statuses,
            RetentionStatus status);

    List<PersonPrivacy> findByRetainUntilBeforeAndRetentionStatus(LocalDate date, RetentionStatus status);

    List<PersonPrivacy> findByImportId(UUID importId);

    List<PersonPrivacy> findByConsentStatusIn(Collection<ConsentStatus> statuses);

    List<PersonPrivacy> findByRetentionStatusNot(RetentionStatus status);

    /** People whose period ends on or before the date and who are not anonymised yet. */
    Page<PersonPrivacy> findByRetentionStatusNotAndRetainUntilLessThanEqual(RetentionStatus status, LocalDate date,
            Pageable pageable);
}
