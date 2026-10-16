package com.recruiting.platform.accounts.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonEmploymentRepository extends JpaRepository<PersonEmployment, UUID> {

    List<PersonEmployment> findByPersonIdOrderBySortOrder(UUID personId);

    void deleteByPersonId(UUID personId);
}
