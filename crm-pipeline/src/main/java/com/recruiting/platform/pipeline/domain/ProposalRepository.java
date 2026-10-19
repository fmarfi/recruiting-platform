package com.recruiting.platform.pipeline.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.recruiting.platform.contracts.ProposalStatus;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProposalRepository extends JpaRepository<Proposal, UUID>, JpaSpecificationExecutor<Proposal> {

    List<Proposal> findByAccountId(UUID accountId);

    long countByAccountIdAndStatusIn(UUID accountId, Collection<ProposalStatus> statuses);
}
