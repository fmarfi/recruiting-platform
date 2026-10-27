package com.recruiting.platform.accounts.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OffLimitsRuleRepository extends JpaRepository<OffLimitsRule, UUID> {

    /** Rules in force on {@code day} for an account id or a company name (accent- and case-insensitive). */
    @Query(value = "SELECT * FROM off_limits_rule WHERE starts_on <= :day AND (ends_on IS NULL OR ends_on >= :day) "
            + "AND (account_id = :accountId OR (company_name IS NOT NULL AND "
            + "immutable_unaccent(lower(trim(company_name))) = immutable_unaccent(lower(trim(:company)))))",
            nativeQuery = true)
    List<OffLimitsRule> findActive(@Param("accountId") UUID accountId, @Param("company") String company,
            @Param("day") LocalDate day);

    List<OffLimitsRule> findByAccountId(UUID accountId);

    /** Rules in force on {@code day} whose target (account name or company name) equals {@code company}. */
    @Query(value = "SELECT r.* FROM off_limits_rule r LEFT JOIN account a ON a.id = r.account_id "
            + "WHERE r.starts_on <= :day AND (r.ends_on IS NULL OR r.ends_on >= :day) "
            + "AND immutable_unaccent(lower(translate(trim(coalesce(r.company_name, a.name)), 'İı', 'Ii'))) "
            + "= immutable_unaccent(lower(translate(trim(:company), 'İı', 'Ii')))", nativeQuery = true)
    List<OffLimitsRule> findActiveByCompany(@Param("company") String company, @Param("day") LocalDate day);
}
