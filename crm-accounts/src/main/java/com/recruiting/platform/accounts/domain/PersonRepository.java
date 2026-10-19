package com.recruiting.platform.accounts.domain;

import com.recruiting.platform.contracts.PersonType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PersonRepository extends JpaRepository<Person, UUID>, JpaSpecificationExecutor<Person> {

    Optional<Person> findByEmailIgnoreCase(String email);

    Page<Person> findByType(PersonType type, Pageable pageable);

    /**
     * Accent- and case-insensitive substring search on name, company and e-mail (A-14), served by the trigram
     * indexes {@code idx_person_*_trgm}. The caller escapes LIKE wildcards in {@code q}.
     */
    @Query(value = "SELECT * FROM person WHERE immutable_unaccent(lower(name)) LIKE '%' || immutable_unaccent(lower(:q)) || '%' "
            + "OR immutable_unaccent(lower(company)) LIKE '%' || immutable_unaccent(lower(:q)) || '%' "
            + "OR immutable_unaccent(lower(email)) LIKE '%' || immutable_unaccent(lower(:q)) || '%' "
            + "ORDER BY name LIMIT :limit", nativeQuery = true)
    List<Person> searchText(@Param("q") String q, @Param("limit") int limit);

    @Query("SELECT p FROM Person p WHERE p.id IN :ids")
    List<Person> findAllByIdIn(@Param("ids") java.util.Collection<UUID> ids);

    @Modifying
    @Query("UPDATE Person p SET p.lastContactAt = :at WHERE p.id = :id AND (p.lastContactAt IS NULL OR p.lastContactAt < :at)")
    int bumpLastContact(@Param("id") UUID id, @Param("at") java.time.Instant at);

    @Modifying
    @Query("UPDATE Person p SET p.consentStatus = :status WHERE p.id = :id")
    int updateConsentStatus(@Param("id") UUID id, @Param("status") com.recruiting.platform.contracts.ConsentStatus status);

    @Modifying
    @Query("UPDATE Person p SET p.retentionStatus = :status WHERE p.id = :id AND p.retentionStatus <> com.recruiting.platform.contracts.RetentionStatus.ANONYMISED")
    int updateRetentionStatus(@Param("id") UUID id, @Param("status") com.recruiting.platform.contracts.RetentionStatus status);
}
