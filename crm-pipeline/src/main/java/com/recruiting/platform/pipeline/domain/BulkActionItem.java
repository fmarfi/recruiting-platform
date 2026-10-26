package com.recruiting.platform.pipeline.domain;

import com.recruiting.platform.contracts.BulkOutcome;
import com.recruiting.platform.contracts.BulkResultCode;
import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;
import java.io.Serializable;
import java.util.UUID;

/** Maps table {@code bulk_action_item} (ADR 0010): one row per requested id. Append-only, not audited. */
@Entity
@Table(name = "bulk_action_item")
@IdClass(BulkActionItem.Key.class)
public class BulkActionItem implements Persistable<BulkActionItem.Key> {

    @Id
    @Column(name = "bulk_action_id")
    private UUID bulkActionId;

    /** No FK: the record may be deleted or anonymised later. */
    @Id
    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "outcome")
    @Enumerated(EnumType.STRING)
    private BulkOutcome outcome;

    /** Null when OK, set otherwise. */
    @Column(name = "code")
    @Enumerated(EnumType.STRING)
    private BulkResultCode code;

    @Transient
    private boolean isNew = true;

    public BulkActionItem() {
        // for JPA
    }

    public BulkActionItem(UUID bulkActionId, UUID entityId, BulkOutcome outcome, BulkResultCode code) {
        this.bulkActionId = bulkActionId;
        this.entityId = entityId;
        this.outcome = outcome;
        this.code = code;
    }

    @Override
    public Key getId() {
        return new Key(bulkActionId, entityId);
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

    public UUID getBulkActionId() {
        return bulkActionId;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public BulkOutcome getOutcome() {
        return outcome;
    }

    public BulkResultCode getCode() {
        return code;
    }

    public record Key(UUID bulkActionId, UUID entityId) implements Serializable {
    }
}
