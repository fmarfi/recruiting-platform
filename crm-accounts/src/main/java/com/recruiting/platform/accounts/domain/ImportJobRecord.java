package com.recruiting.platform.accounts.domain;

import com.recruiting.platform.contracts.EntityType;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

/** Maps table {@code import_job_record} (1.3.0, CO-014): one row per record created by an import commit, ids only, append-only, not audited. */
@Entity
@Table(name = "import_job_record")
public class ImportJobRecord implements Persistable<ImportJobRecord.Key> {

    @EmbeddedId
    private Key id;

    @Transient
    private boolean isNew = true;

    public ImportJobRecord() {
        // for JPA
    }

    public ImportJobRecord(UUID importId, EntityType entityType, UUID entityId) {
        this.id = new Key(importId, entityType, entityId);
    }

    @Override
    public Key getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    public UUID getImportId() {
        return id.importId;
    }

    public EntityType getEntityType() {
        return id.entityType;
    }

    public UUID getEntityId() {
        return id.entityId;
    }

    /** Composite primary key. */
    @Embeddable
    public static class Key implements Serializable {

        @Column(name = "import_id")
        private UUID importId;

        @Enumerated(EnumType.STRING)
        @Column(name = "entity_type")
        private EntityType entityType;

        @Column(name = "entity_id")
        private UUID entityId;

        public Key() {
            // for JPA
        }

        public Key(UUID importId, EntityType entityType, UUID entityId) {
            this.importId = importId;
            this.entityType = entityType;
            this.entityId = entityId;
        }

        public UUID getImportId() {
            return importId;
        }

        public EntityType getEntityType() {
            return entityType;
        }

        public UUID getEntityId() {
            return entityId;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key other
                    && Objects.equals(importId, other.importId)
                    && Objects.equals(entityType, other.entityType)
                    && Objects.equals(entityId, other.entityId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(importId, entityType, entityId);
        }
    }
}
