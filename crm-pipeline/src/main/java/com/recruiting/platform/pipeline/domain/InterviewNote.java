package com.recruiting.platform.pipeline.domain;

import com.recruiting.platform.contracts.InterviewType;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;

/** Maps table {@code interview_note} (1.3.0, SX-009): free-text note of one interview. */
@Entity
@Table(name = "interview_note")
@Audited
public class InterviewNote extends AuditableEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "person_id", updatable = false)
    private UUID personId;

    @Column(name = "job_order_id")
    private UUID jobOrderId;

    @Column(name = "application_id")
    private UUID applicationId;

    @Column(name = "interviewer_id")
    private UUID interviewerId;

    @Column(name = "held_on")
    private LocalDate heldOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "interview_type")
    private InterviewType interviewType;

    @Column(name = "body")
    private String body;

    @Column(name = "include_in_report")
    private boolean includeInReport;

    public InterviewNote() {
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

    public UUID getInterviewerId() {
        return interviewerId;
    }
    public void setInterviewerId(UUID interviewerId) {
        this.interviewerId = interviewerId;
    }

    public LocalDate getHeldOn() {
        return heldOn;
    }
    public void setHeldOn(LocalDate heldOn) {
        this.heldOn = heldOn;
    }

    public InterviewType getInterviewType() {
        return interviewType;
    }
    public void setInterviewType(InterviewType interviewType) {
        this.interviewType = interviewType;
    }

    public String getBody() {
        return body;
    }
    public void setBody(String body) {
        this.body = body;
    }

    public boolean isIncludeInReport() {
        return includeInReport;
    }
    public void setIncludeInReport(boolean includeInReport) {
        this.includeInReport = includeInReport;
    }

}
