package com.recruiting.platform.accounts.domain;

import com.recruiting.platform.contracts.ListEntity;
import com.recruiting.platform.contracts.ViewVisibility;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SavedViewRepository extends JpaRepository<SavedView, UUID> {

    /** Own views of any visibility plus every shared view of the entity. */
    @Query("select v from SavedView v where v.entity = :entity and (v.ownerId = :ownerId or v.visibility = :shared) "
            + "order by lower(v.name)")
    List<SavedView> findVisible(@Param("entity") ListEntity entity, @Param("ownerId") UUID ownerId,
            @Param("shared") ViewVisibility shared);

    long countByOwnerIdAndEntityAndVisibility(UUID ownerId, ListEntity entity, ViewVisibility visibility);

    @Query("select v from SavedView v where v.ownerId = :ownerId and v.entity = :entity and lower(v.name) = lower(:name)")
    Optional<SavedView> findByOwnerAndName(@Param("ownerId") UUID ownerId, @Param("entity") ListEntity entity,
            @Param("name") String name);
}
