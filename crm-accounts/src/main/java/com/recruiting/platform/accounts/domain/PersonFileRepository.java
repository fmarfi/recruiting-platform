package com.recruiting.platform.accounts.domain;

import com.recruiting.platform.contracts.PersonFileKind;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PersonFileRepository extends JpaRepository<PersonFile, UUID> {

    List<PersonFile> findByPersonIdOrderByKindAscVersionDesc(UUID personId);

    Optional<PersonFile> findByPersonIdAndKindAndCurrentTrue(UUID personId, PersonFileKind kind);

    Optional<PersonFile> findByPersonIdAndSha256(UUID personId, String sha256);

    @Query("SELECT COALESCE(MAX(f.version), 0) FROM PersonFile f WHERE f.personId = :personId AND f.kind = :kind")
    int maxVersion(@Param("personId") UUID personId, @Param("kind") PersonFileKind kind);

    /** Text extraction worker queue: oldest first. */
    @Query(value = "SELECT * FROM person_file WHERE text_status = 'PENDING' ORDER BY created_at LIMIT :limit",
            nativeQuery = true)
    List<PersonFile> findPendingText(@Param("limit") int limit);

    /**
     * Full-text search over the current CV of each person, Turkish stemming and accent-insensitive
     * ({@code tr_unaccent}), served by the partial GIN index {@code idx_person_file_tsv}.
     */
    @Query(value = "SELECT person_id FROM person_file WHERE kind = 'CV' AND is_current "
            + "AND text_tsv @@ plainto_tsquery('tr_unaccent', :q)", nativeQuery = true)
    List<UUID> findPersonIdsByCvText(@Param("q") String q);
}
