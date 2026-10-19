package com.recruiting.platform.privacy.domain;

import com.recruiting.platform.contracts.Language;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrivacyNoticeRepository extends JpaRepository<PrivacyNotice, UUID> {

    /** Current notice per language: the latest effective_from that is not after today. */
    Optional<PrivacyNotice> findFirstByLanguageAndEffectiveFromLessThanEqualOrderByEffectiveFromDescCreatedAtDesc(
            Language language, LocalDate today);

    Optional<PrivacyNotice> findFirstByLanguageAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
            Language language, LocalDate today);

    boolean existsByVersionAndLanguage(String version, Language language);

    boolean existsByLanguage(Language language);

    List<PrivacyNotice> findByLanguage(Language language);
}
