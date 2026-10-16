package com.recruiting.platform.privacy.domain;

import jakarta.persistence.*;
import com.recruiting.platform.contracts.ConsentDecision;
import com.recruiting.platform.contracts.EmailDeliveryStatus;
import com.recruiting.platform.contracts.Language;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

/** Maps table {@code consent_request}. Other modules reference it by id only (ADR 0001). */
@Entity
@Table(name = "consent_request")
public class ConsentRequest {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "person_id")
    private UUID personId;

    @Column(name = "notice_id")
    private UUID noticeId;

    @Column(name = "language")
    @Enumerated(EnumType.STRING)
    private Language language;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "token_hash", length = 64)
    private String tokenHash;

    @Column(name = "delivery_status")
    @Enumerated(EnumType.STRING)
    private EmailDeliveryStatus deliveryStatus;

    @Column(name = "queued_at")
    private Instant queuedAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "response")
    @Enumerated(EnumType.STRING)
    private ConsentDecision response;

    @Column(name = "requested_by")
    private UUID requestedBy;

    public ConsentRequest() {
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

    public UUID getNoticeId() {
        return noticeId;
    }

    public void setNoticeId(UUID noticeId) {
        this.noticeId = noticeId;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public EmailDeliveryStatus getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(EmailDeliveryStatus deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public Instant getQueuedAt() {
        return queuedAt;
    }

    public void setQueuedAt(Instant queuedAt) {
        this.queuedAt = queuedAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(Instant respondedAt) {
        this.respondedAt = respondedAt;
    }

    public ConsentDecision getResponse() {
        return response;
    }

    public void setResponse(ConsentDecision response) {
        this.response = response;
    }

    public UUID getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(UUID requestedBy) {
        this.requestedBy = requestedBy;
    }

}
