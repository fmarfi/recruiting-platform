package com.recruiting.platform.accounts.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonEducationRepository extends JpaRepository<PersonEducation, UUID> {

    List<PersonEducation> findByPersonIdOrderBySortOrder(UUID personId);

    void deleteByPersonId(UUID personId);
}
