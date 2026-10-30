package com.recruiting.platform.accounts.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link AccountMerge}. */
public interface AccountMergeRepository extends JpaRepository<AccountMerge, UUID> {
}
