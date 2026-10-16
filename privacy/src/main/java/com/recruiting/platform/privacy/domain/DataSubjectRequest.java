package com.recruiting.platform.privacy.domain;

import jakarta.persistence.*;
import com.recruiting.platform.contracts.DataSubjectRequestStatus;
import com.recruiting.platform.contracts.DataSubjectRequestType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;

/** Maps table {@code data_subject_request}. Other modules reference it by id only (ADR 0001). */
@Entity
@Table(name = "data_subject_request")
@Audited
public class DataSubjectRequest extends AuditableEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "person_id")
    private UUID personId;

    @Column(name = "requester_name", length = 200)
    private String requesterName;

    @Column(name = "requester_email", length = 254)
    private String requesterEmail;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private DataSubjectRequestType type;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private DataSubjectRequestStatus status;

    @Column(name = "description")
    private String description;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "due_on")
    private LocalDate dueOn;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution")
    private String resolution;

    @Column(name = "handled_by")
    private UUID handledBy;

    public DataSubjectRequest() {
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

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public String getRequesterEmail() {
        return requesterEmail;
    }

    public void setRequesterEmail(String requesterEmail) {
        this.requesterEmail = requesterEmail;
    }

    public DataSubjectRequestType getType() {
        return type;
    }

    public void setType(DataSubjectRequestType type) {
        this.type = type;
    }

    public DataSubjectRequestStatus getStatus() {
        return status;
    }

    public void setStatus(DataSubjectRequestStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public LocalDate getDueOn() {
        return dueOn;
    }

    public void setDueOn(LocalDate dueOn) {
        this.dueOn = dueOn;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public UUID getHandledBy() {
        return handledBy;
    }

    public void setHandledBy(UUID handledBy) {
        this.handledBy = handledBy;
    }

}
