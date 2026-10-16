package com.recruiting.platform.accounts.domain;

import com.recruiting.platform.contracts.AccountStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByNameIgnoreCase(String name);

    Page<Account> findByStatus(AccountStatus status, Pageable pageable);

    Page<Account> findByOwnerId(UUID ownerId, Pageable pageable);

    /**
     * Accent- and case-insensitive substring search on the name (A-14), served by the trigram index
     * {@code idx_account_name_trgm}. The caller escapes LIKE wildcards in {@code q}.
     */
    @Query(value = "SELECT * FROM account WHERE immutable_unaccent(lower(name)) LIKE '%' || immutable_unaccent(lower(:q)) || '%' "
            + "ORDER BY name LIMIT :limit", nativeQuery = true)
    List<Account> searchByName(@Param("q") String q, @Param("limit") int limit);
}
