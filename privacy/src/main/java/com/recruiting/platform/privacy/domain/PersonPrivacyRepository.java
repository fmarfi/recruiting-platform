package com.recruiting.platform.privacy.domain;

import com.recruiting.platform.contracts.RetentionStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonPrivacyRepository extends JpaRepository<PersonPrivacy, UUID> {

    List<PersonPrivacy> findByRetainUntilBeforeAndRetentionStatus(LocalDate date, RetentionStatus status);

    List<PersonPrivacy> findByImportId(UUID importId);
}
