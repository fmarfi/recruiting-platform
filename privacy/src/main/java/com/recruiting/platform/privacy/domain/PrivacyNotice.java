package com.recruiting.platform.privacy.domain;

import jakarta.persistence.*;
import com.recruiting.platform.contracts.Language;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Maps table {@code privacy_notice}. Other modules reference it by id only (ADR 0001). */
@Entity
@org.hibernate.annotations.Immutable
@Table(name = "privacy_notice")
@EntityListeners(AuditingEntityListener.class)
public class PrivacyNotice {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "version", length = 40)
    private String version;

    @Column(name = "language")
    @Enumerated(EnumType.STRING)
    private Language language;

    @Column(name = "notice_text")
    private String noticeText;

    @Column(name = "consent_text")
    private String consentText;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    public PrivacyNotice() {
        // for JPA
    }

    public UUID getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public String getNoticeText() {
        return noticeText;
    }

    public void setNoticeText(String noticeText) {
        this.noticeText = noticeText;
    }

    public String getConsentText() {
        return consentText;
    }

    public void setConsentText(String consentText) {
        this.consentText = consentText;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

}
