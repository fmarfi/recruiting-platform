package com.recruiting.platform.privacy.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

/** Maps table {@code retention_run}. Other modules reference it by id only (ADR 0001). */
@Entity
@Table(name = "retention_run")
public class RetentionRun {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "triggered_by")
    private UUID triggeredBy;

    @Column(name = "dry_run")
    private boolean dryRun = false;

    @Column(name = "flagged_count")
    private Integer flaggedCount = 0;

    @Column(name = "anonymised_count")
    private Integer anonymisedCount = 0;

    @Column(name = "deleted_count")
    private Integer deletedCount = 0;

    @Column(name = "failure_reason")
    private String failureReason;

    public RetentionRun() {
        // for JPA
    }

    public UUID getId() {
        return id;
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

    public UUID getTriggeredBy() {
        return triggeredBy;
    }

    public void setTriggeredBy(UUID triggeredBy) {
        this.triggeredBy = triggeredBy;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public Integer getFlaggedCount() {
        return flaggedCount;
    }

    public void setFlaggedCount(Integer flaggedCount) {
        this.flaggedCount = flaggedCount;
    }

    public Integer getAnonymisedCount() {
        return anonymisedCount;
    }

    public void setAnonymisedCount(Integer anonymisedCount) {
        this.anonymisedCount = anonymisedCount;
    }

    public Integer getDeletedCount() {
        return deletedCount;
    }

    public void setDeletedCount(Integer deletedCount) {
        this.deletedCount = deletedCount;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

}
