package com.recruiting.platform.pipeline.domain;

import jakarta.persistence.*;
import com.recruiting.platform.contracts.PipelineStage;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;

/** Maps table {@code application}. Other modules reference it by id only (ADR 0001). */
@Entity
@Table(name = "application")
@Audited
public class Application extends AuditableEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "job_order_id")
    private UUID jobOrderId;

    @Column(name = "person_id")
    private UUID personId;

    @Column(name = "stage")
    @Enumerated(EnumType.STRING)
    private PipelineStage stage;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "stage_changed_at")
    private Instant stageChangedAt;

    public Application() {
        // for JPA
    }

    public UUID getId() {
        return id;
    }

    public UUID getJobOrderId() {
        return jobOrderId;
    }

    public void setJobOrderId(UUID jobOrderId) {
        this.jobOrderId = jobOrderId;
    }

    public UUID getPersonId() {
        return personId;
    }

    public void setPersonId(UUID personId) {
        this.personId = personId;
    }

    public PipelineStage getStage() {
        return stage;
    }

    public void setStage(PipelineStage stage) {
        this.stage = stage;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Instant getStageChangedAt() {
        return stageChangedAt;
    }

    public void setStageChangedAt(Instant stageChangedAt) {
        this.stageChangedAt = stageChangedAt;
    }

}
