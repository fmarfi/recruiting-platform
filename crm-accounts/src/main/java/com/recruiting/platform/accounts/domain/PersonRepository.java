package com.recruiting.platform.accounts.domain;

import com.recruiting.platform.contracts.PersonType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PersonRepository extends JpaRepository<Person, UUID> {

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
}
