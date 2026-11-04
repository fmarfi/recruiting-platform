package com.recruiting.platform.pipeline.domain;

import com.recruiting.platform.contracts.CloseReason;
import com.recruiting.platform.contracts.OutreachStatus;

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
    private PipelineStage stage = PipelineStage.SOURCED;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "stage_changed_at")
    private Instant stageChangedAt = Instant.now();

    /** Set when a manager moved the person despite an off-limits rule (1.2.0); scrubbed like rejection_reason. */
    @Column(name = "off_limits_override_reason")
    private String offLimitsOverrideReason;

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

    public String getOffLimitsOverrideReason() {
        return offLimitsOverrideReason;
    }

    public void setOffLimitsOverrideReason(String offLimitsOverrideReason) {
        this.offLimitsOverrideReason = offLimitsOverrideReason;
    }

    @Column(name = "rank")
    private Integer rank;

    @Column(name = "rating")
    private Short rating;

    @Column(name = "rationale")
    private String rationale;

    @Enumerated(EnumType.STRING)
    @Column(name = "outreach_status")
    private OutreachStatus outreachStatus = OutreachStatus.TO_CALL;

    @Enumerated(EnumType.STRING)
    @Column(name = "close_reason")
    private CloseReason closeReason;

    public Integer getRank() {
        return rank;
    }
    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public Short getRating() {
        return rating;
    }
    public void setRating(Short rating) {
        this.rating = rating;
    }

    public String getRationale() {
        return rationale;
    }
    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public OutreachStatus getOutreachStatus() {
        return outreachStatus;
    }
    public void setOutreachStatus(OutreachStatus outreachStatus) {
        this.outreachStatus = outreachStatus;
    }

    public CloseReason getCloseReason() {
        return closeReason;
    }
    public void setCloseReason(CloseReason closeReason) {
        this.closeReason = closeReason;
    }
}
