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

    /** Duplicate check (B-04): same normalised phone (1.2.0), served by idx_person_phone_normalized. */
    List<Person> findByPhoneNormalized(String phoneNormalized);

    /** Duplicate check (B-04): same normalised LinkedIn URL (1.2.0), served by idx_person_linkedin_normalized. */
    List<Person> findByLinkedinUrlNormalized(String linkedinUrlNormalized);

    /** Duplicate check (B-04): same name and company, accent- and case-insensitive (idx_person_name_company_norm). */
    @Query(value = "SELECT * FROM person WHERE immutable_unaccent(lower(trim(name))) = immutable_unaccent(lower(trim(:name))) "
            + "AND immutable_unaccent(lower(trim(company))) = immutable_unaccent(lower(trim(:company)))",
            nativeQuery = true)
    List<Person> findByNameAndCompanyNormalized(@Param("name") String name, @Param("company") String company);

    /** The {@code city} filter (1.1.1), served by idx_person_city. */
    @Query(value = "SELECT * FROM person WHERE immutable_unaccent(lower(trim(city))) = immutable_unaccent(lower(trim(:city)))",
            nativeQuery = true)
    List<Person> findByCityNormalized(@Param("city") String city);

    /** The {@code city} filter: ids of people whose normalised city is one of the values (unit separator delimited). */
    @Query(value = "SELECT p.id FROM person p WHERE immutable_unaccent(lower(translate(trim(p.city), 'İı', 'Ii'))) IN "
            + "(SELECT immutable_unaccent(lower(translate(trim(x), 'İı', 'Ii'))) FROM unnest(string_to_array(:csv, chr(31))) AS x)",
            nativeQuery = true)
    List<UUID> findIdsByCities(@Param("csv") String csv);

    /** The {@code tag} filter: at least one of the (lower-case, comma delimited) tags; served by the GIN index. */
    @Query(value = "SELECT p.id FROM person p WHERE p.tags && CAST(string_to_array(:csv, ',') AS varchar(50)[])",
            nativeQuery = true)
    List<UUID> findIdsByAnyTag(@Param("csv") String csv);

    /** The {@code skill} filter (1.3.0): at least one of the (lower-case, comma delimited) skills; GIN index. */
    @Query(value = "SELECT p.id FROM person p WHERE p.skills && CAST(string_to_array(:csv, ',') AS varchar(50)[])",
            nativeQuery = true)
    List<UUID> findIdsByAnySkill(@Param("csv") String csv);

    /** The {@code language} filter (1.3.0): speaks all of the (comma delimited ISO 639-1) languages, any level. */
    @Query(value = "SELECT p.id FROM person p WHERE NOT EXISTS (SELECT 1 FROM unnest(string_to_array(:csv, ',')) AS x "
            + "WHERE NOT EXISTS (SELECT 1 FROM jsonb_array_elements(p.languages) e WHERE lower(e->>'language') = x))",
            nativeQuery = true)
    List<UUID> findIdsByAllLanguages(@Param("csv") String csv);
}
