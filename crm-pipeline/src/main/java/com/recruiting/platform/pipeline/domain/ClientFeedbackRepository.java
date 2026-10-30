package com.recruiting.platform.pipeline.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Spring Data repository for {@link ClientFeedback}. */
public interface ClientFeedbackRepository extends JpaRepository<ClientFeedback, UUID>, JpaSpecificationExecutor<ClientFeedback> {
    List<ClientFeedback> findByApplicationIdOrderByRoundDesc(UUID applicationId);
}
