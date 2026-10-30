package com.recruiting.platform.pipeline.domain;

import com.recruiting.platform.contracts.ActivityDirection;
import com.recruiting.platform.contracts.ActivityOutcome;
import com.recruiting.platform.contracts.ActivitySource;

import jakarta.persistence.*;
import com.recruiting.platform.contracts.ActivityType;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;

/** Maps table {@code activity}. Other modules reference it by id only (ADR 0001). */
@Entity
@Table(name = "activity")
@Audited
public class Activity extends AuditableEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private ActivityType type;

    @Column(name = "note")
    private String note;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "person_id")
    private UUID personId;

    @Column(name = "job_order_id")
    private UUID jobOrderId;

    @Column(name = "user_id")
    private UUID userId;

    public Activity() {
        // for JPA
    }

    public UUID getId() {
        return id;
    }

    public ActivityType getType() {
        return type;
    }

    public void setType(ActivityType type) {
        this.type = type;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public UUID getPersonId() {
        return personId;
    }

    public void setPersonId(UUID personId) {
        this.personId = personId;
    }

    public UUID getJobOrderId() {
        return jobOrderId;
    }

    public void setJobOrderId(UUID jobOrderId) {
        this.jobOrderId = jobOrderId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome")
    private ActivityOutcome outcome;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction")
    private ActivityDirection direction;

    @Column(name = "duration_min")
    private Short durationMin;

    @Enumerated(EnumType.STRING)
    @Column(name = "source")
    private ActivitySource source = ActivitySource.MANUAL;

    @Column(name = "external_message_id", length = 300)
    private String externalMessageId;

    public ActivityOutcome getOutcome() {
        return outcome;
    }
    public void setOutcome(ActivityOutcome outcome) {
        this.outcome = outcome;
    }

    public ActivityDirection getDirection() {
        return direction;
    }
    public void setDirection(ActivityDirection direction) {
        this.direction = direction;
    }

    public Short getDurationMin() {
        return durationMin;
    }
    public void setDurationMin(Short durationMin) {
        this.durationMin = durationMin;
    }

    public ActivitySource getSource() {
        return source;
    }
    public void setSource(ActivitySource source) {
        this.source = source;
    }

    public String getExternalMessageId() {
        return externalMessageId;
    }
    public void setExternalMessageId(String externalMessageId) {
        this.externalMessageId = externalMessageId;
    }
}
