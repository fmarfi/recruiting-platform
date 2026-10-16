package com.recruiting.platform.privacy.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentRequestRepository extends JpaRepository<ConsentRequest, UUID> {

    Optional<ConsentRequest> findByTokenHash(String tokenHash);

    List<ConsentRequest> findByPersonId(UUID personId);
}
