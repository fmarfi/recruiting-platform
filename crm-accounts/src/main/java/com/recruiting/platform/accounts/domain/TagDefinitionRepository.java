package com.recruiting.platform.accounts.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Spring Data repository for {@link TagDefinition}. */
public interface TagDefinitionRepository extends JpaRepository<TagDefinition, UUID>, JpaSpecificationExecutor<TagDefinition> {
    Optional<TagDefinition> findByName(String name);

    List<TagDefinition> findAllByOrderByNameAsc();
}
