package com.recruiting.platform.accounts.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NetworkLinkRepository extends JpaRepository<NetworkLink, UUID> {

    List<NetworkLink> findByFromPersonIdOrToPersonId(UUID fromPersonId, UUID toPersonId);

    java.util.Optional<NetworkLink> findByFromPersonIdAndToPersonIdAndType(UUID fromPersonId, UUID toPersonId,
            com.recruiting.platform.contracts.LinkType type);
}
