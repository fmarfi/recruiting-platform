package com.recruiting.platform.security.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppUserRepository extends JpaRepository<AppUser, UUID>, JpaSpecificationExecutor<AppUser> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /** Stamps the login time without touching audited columns (no Envers revision per login). */
    @Modifying(clearAutomatically = true)
    @Query("update AppUser u set u.lastLoginAt = :at where u.id = :id")
    int markLoggedIn(@Param("id") UUID id, @Param("at") Instant at);
}
