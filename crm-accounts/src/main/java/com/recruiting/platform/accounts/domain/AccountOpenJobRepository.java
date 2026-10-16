package com.recruiting.platform.accounts.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountOpenJobRepository extends JpaRepository<AccountOpenJob, UUID> {

    long countByAccountIdAndOpenTrue(UUID accountId);

    List<AccountOpenJob> findByAccountIdAndOpenTrue(UUID accountId);
}
