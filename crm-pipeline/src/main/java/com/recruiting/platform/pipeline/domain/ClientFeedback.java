package com.recruiting.platform.pipeline.domain;

import com.recruiting.platform.contracts.ClientFeedbackOutcome;
import com.recruiting.platform.contracts.DeclineReason;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;

/** Maps table {@code client_feedback} (1.3.0, SX-019): client feedback per application and round. {@code participants} holds roles, never names. */
@Entity
@Table(name = "client_feedback")
@Audited
public class ClientFeedback extends AuditableEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "application_id", updatable = false)
    private UUID applicationId;

    @Column(name = "person_id", updatable = false)
    private UUID personId;

    @Column(name = "round")
    private Short round;

    @Column(name = "feedback_on")
    private LocalDate feedbackOn;

    @Column(name = "participants", length = 500)
    private String participants;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome")
    private ClientFeedbackOutcome outcome;

    @Enumerated(EnumType.STRING)
    @Column(name = "decline_reason")
    private DeclineReason declineReason;

    @Column(name = "reason_text", length = 1000)
    private String reasonText;

    @Column(name = "candidate_feedback", length = 2000)
    private String candidateFeedback;

    @Column(name = "recorded_by")
    private UUID recordedBy;

    @Column(name = "recorded_at")
    private Instant recordedAt;

    public ClientFeedback() {
        // for JPA
    }

    public UUID getId() {
        return id;
    }

    public UUID getApplicationId() {
        return applicationId;
    }
    public void setApplicationId(UUID applicationId) {
        this.applicationId = applicationId;
    }

    public UUID getPersonId() {
        return personId;
    }
    public void setPersonId(UUID personId) {
        this.personId = personId;
    }

    public Short getRound() {
        return round;
    }
    public void setRound(Short round) {
        this.round = round;
    }

    public LocalDate getFeedbackOn() {
        return feedbackOn;
    }
    public void setFeedbackOn(LocalDate feedbackOn) {
        this.feedbackOn = feedbackOn;
    }

    public String getParticipants() {
        return participants;
    }
    public void setParticipants(String participants) {
        this.participants = participants;
    }

    public ClientFeedbackOutcome getOutcome() {
        return outcome;
    }
    public void setOutcome(ClientFeedbackOutcome outcome) {
        this.outcome = outcome;
    }

    public DeclineReason getDeclineReason() {
        return declineReason;
    }
    public void setDeclineReason(DeclineReason declineReason) {
        this.declineReason = declineReason;
    }

    public String getReasonText() {
        return reasonText;
    }
    public void setReasonText(String reasonText) {
        this.reasonText = reasonText;
    }

    public String getCandidateFeedback() {
        return candidateFeedback;
    }
    public void setCandidateFeedback(String candidateFeedback) {
        this.candidateFeedback = candidateFeedback;
    }

    public UUID getRecordedBy() {
        return recordedBy;
    }
    public void setRecordedBy(UUID recordedBy) {
        this.recordedBy = recordedBy;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }

}
