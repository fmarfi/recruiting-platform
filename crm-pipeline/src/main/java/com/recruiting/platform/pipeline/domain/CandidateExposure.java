package com.recruiting.platform.pipeline.domain;

import com.recruiting.platform.contracts.ExposureChannel;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

/**
 * Maps table {@code candidate_exposure} (1.2.0, ADR 0011): append-only, ids and dates only, not audited. Kept on
 * anonymisation; cascades on person delete. {@code application_id} is set null if the application is deleted.
 */
@Entity
@Table(name = "candidate_exposure")
public class CandidateExposure {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "person_id", updatable = false)
    private UUID personId;

    @Column(name = "account_id", updatable = false)
    private UUID accountId;

    @Column(name = "job_order_id", updatable = false)
    private UUID jobOrderId;

    @Column(name = "application_id")
    private UUID applicationId;

    @Column(name = "channel", updatable = false)
    @Enumerated(EnumType.STRING)
    private ExposureChannel channel;

    @Column(name = "presented_at", updatable = false)
    private Instant presentedAt = Instant.now();

    /** Null for system writes. */
    @Column(name = "presented_by", updatable = false)
    private UUID presentedBy;

    public CandidateExposure() {
        // for JPA
    }

    public UUID getId() {
        return id;
    }

    public UUID getPersonId() {
        return personId;
    }

    public void setPersonId(UUID personId) {
        this.personId = personId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public UUID getJobOrderId() {
        return jobOrderId;
    }

    public void setJobOrderId(UUID jobOrderId) {
        this.jobOrderId = jobOrderId;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(UUID applicationId) {
        this.applicationId = applicationId;
    }

    public ExposureChannel getChannel() {
        return channel;
    }

    public void setChannel(ExposureChannel channel) {
        this.channel = channel;
    }

    public Instant getPresentedAt() {
        return presentedAt;
    }

    public void setPresentedAt(Instant presentedAt) {
        this.presentedAt = presentedAt;
    }

    public UUID getPresentedBy() {
        return presentedBy;
    }

    public void setPresentedBy(UUID presentedBy) {
        this.presentedBy = presentedBy;
    }
}
