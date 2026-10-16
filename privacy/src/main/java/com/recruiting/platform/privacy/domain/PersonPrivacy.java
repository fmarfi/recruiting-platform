package com.recruiting.platform.privacy.domain;

import jakarta.persistence.*;
import com.recruiting.platform.contracts.ConsentStatus;
import com.recruiting.platform.contracts.RetentionStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.envers.Audited;

/** Maps table {@code person_privacy}. Other modules reference it by id only (ADR 0001). */
@Entity
@Table(name = "person_privacy")
@Audited
public class PersonPrivacy {

    @Id
    @Column(name = "person_id")
    private UUID personId;

    @Column(name = "consent_status")
    @Enumerated(EnumType.STRING)
    private ConsentStatus consentStatus = ConsentStatus.NOT_SENT;

    @Column(name = "notice_sent_at")
    private Instant noticeSentAt;

    @Column(name = "basis_date")
    private LocalDate basisDate = LocalDate.now();

    @Column(name = "retain_until")
    private LocalDate retainUntil;

    @Column(name = "retention_status")
    @Enumerated(EnumType.STRING)
    private RetentionStatus retentionStatus = RetentionStatus.ACTIVE;

    @Column(name = "flagged_at")
    private Instant flaggedAt;

    @Column(name = "import_id")
    private UUID importId;

    @Version
    private Long version;

    public PersonPrivacy() {
        // for JPA
    }

    public PersonPrivacy(UUID personId) {
        this.personId = personId;
    }

    public UUID getPersonId() {
        return personId;
    }

    public void setPersonId(UUID personId) {
        this.personId = personId;
    }

    public ConsentStatus getConsentStatus() {
        return consentStatus;
    }

    public void setConsentStatus(ConsentStatus consentStatus) {
        this.consentStatus = consentStatus;
    }

    public Instant getNoticeSentAt() {
        return noticeSentAt;
    }

    public void setNoticeSentAt(Instant noticeSentAt) {
        this.noticeSentAt = noticeSentAt;
    }

    public LocalDate getBasisDate() {
        return basisDate;
    }

    public void setBasisDate(LocalDate basisDate) {
        this.basisDate = basisDate;
    }

    public LocalDate getRetainUntil() {
        return retainUntil;
    }

    public void setRetainUntil(LocalDate retainUntil) {
        this.retainUntil = retainUntil;
    }

    public RetentionStatus getRetentionStatus() {
        return retentionStatus;
    }

    public void setRetentionStatus(RetentionStatus retentionStatus) {
        this.retentionStatus = retentionStatus;
    }

    public Instant getFlaggedAt() {
        return flaggedAt;
    }

    public void setFlaggedAt(Instant flaggedAt) {
        this.flaggedAt = flaggedAt;
    }

    public UUID getImportId() {
        return importId;
    }

    public void setImportId(UUID importId) {
        this.importId = importId;
    }

    public Long getVersion() {
        return version;
    }

}
