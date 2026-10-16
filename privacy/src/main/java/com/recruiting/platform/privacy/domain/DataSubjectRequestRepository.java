package com.recruiting.platform.privacy.domain;

import com.recruiting.platform.contracts.DataSubjectRequestStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataSubjectRequestRepository extends JpaRepository<DataSubjectRequest, UUID> {

    List<DataSubjectRequest> findByStatus(DataSubjectRequestStatus status);

    List<DataSubjectRequest> findByPersonId(UUID personId);
}
