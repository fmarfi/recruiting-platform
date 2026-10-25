package com.recruiting.platform.pipeline.domain;

import com.recruiting.platform.contracts.BulkActionType;
import com.recruiting.platform.contracts.ListEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

/** Maps table {@code bulk_action} (ADR 0010). Append-only, not audited. */
@Entity
@Table(name = "bulk_action")
public class BulkAction {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "entity")
    @Enumerated(EnumType.STRING)
    private ListEntity entity;

    @Column(name = "action")
    @Enumerated(EnumType.STRING)
    private BulkActionType action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "params")
    private Map<String, Object> params;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "requested_count")
    private int requestedCount;

    @Column(name = "ok_count")
    private int okCount;

    @Column(name = "skipped_count")
    private int skippedCount;

    @Column(name = "failed_count")
    private int failedCount;

    public BulkAction() {
        // for JPA
    }

    public UUID getId() {
        return id;
    }

    public ListEntity getEntity() {
        return entity;
    }

    public void setEntity(ListEntity entity) {
        this.entity = entity;
    }

    public BulkActionType getAction() {
        return action;
    }

    public void setAction(BulkActionType action) {
        this.action = action;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public UUID getActorId() {
        return actorId;
    }

    public void setActorId(UUID actorId) {
        this.actorId = actorId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public int getRequestedCount() {
        return requestedCount;
    }

    public void setRequestedCount(int requestedCount) {
        this.requestedCount = requestedCount;
    }

    public int getOkCount() {
        return okCount;
    }

    public void setOkCount(int okCount) {
        this.okCount = okCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

}
