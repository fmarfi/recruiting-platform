package com.recruiting.platform.privacy.domain;

import jakarta.persistence.*;
import com.recruiting.platform.contracts.ConsentChannel;
import com.recruiting.platform.contracts.ConsentDecision;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

/** Maps table {@code consent_record}. Other modules reference it by id only (ADR 0001). */
@Entity
@Table(name = "consent_record")
public class ConsentRecord {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "person_id")
    private UUID personId;

    @Column(name = "decision")
    @Enumerated(EnumType.STRING)
    private ConsentDecision decision;

    @Column(name = "channel")
    @Enumerated(EnumType.STRING)
    private ConsentChannel channel;

    @Column(name = "notice_id")
    private UUID noticeId;

    @Column(name = "consent_request_id")
    private UUID consentRequestId;

    @Column(name = "recorded_at")
    private Instant recordedAt = Instant.now();

    @Column(name = "recorded_by")
    private UUID recordedBy;

    @Column(name = "evidence")
    private String evidence;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    public ConsentRecord() {
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

    public ConsentDecision getDecision() {
        return decision;
    }

    public void setDecision(ConsentDecision decision) {
        this.decision = decision;
    }

    public ConsentChannel getChannel() {
        return channel;
    }

    public void setChannel(ConsentChannel channel) {
        this.channel = channel;
    }

    public UUID getNoticeId() {
        return noticeId;
    }

    public void setNoticeId(UUID noticeId) {
        this.noticeId = noticeId;
    }

    public UUID getConsentRequestId() {
        return consentRequestId;
    }

    public void setConsentRequestId(UUID consentRequestId) {
        this.consentRequestId = consentRequestId;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }

    public UUID getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(UUID recordedBy) {
        this.recordedBy = recordedBy;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

}
