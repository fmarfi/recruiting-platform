package com.recruiting.platform.pipeline.domain;

import com.recruiting.platform.contracts.Language;
import com.recruiting.platform.contracts.ReportKind;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

/** Maps table {@code client_report} (1.3.0, SX-021/022): a generated PDF report. Immutable after insert except {@code sentOn} and {@code sentBy}; not audited. */
@Entity
@Table(name = "client_report")
public class ClientReport {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "job_order_id", updatable = false)
    private UUID jobOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", updatable = false)
    private ReportKind kind;

    @Column(name = "version", updatable = false)
    private Integer version;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", updatable = false)
    private Language language;

    @Column(name = "blind", updatable = false)
    private boolean blind;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "person_ids", updatable = false)
    private List<UUID> personIds = new ArrayList<>();

    @Column(name = "storage_key", length = 300, updatable = false)
    private String storageKey;

    @Column(name = "size_bytes", updatable = false)
    private Long sizeBytes;

    @Column(name = "generated_by", updatable = false)
    private UUID generatedBy;

    @Column(name = "generated_at", updatable = false)
    private Instant generatedAt;

    @Column(name = "sent_on")
    private LocalDate sentOn;

    @Column(name = "sent_by")
    private UUID sentBy;

    public ClientReport() {
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

    public ReportKind getKind() {
        return kind;
    }
    public void setKind(ReportKind kind) {
        this.kind = kind;
    }

    public Integer getVersion() {
        return version;
    }
    public void setVersion(Integer version) {
        this.version = version;
    }

    public Language getLanguage() {
        return language;
    }
    public void setLanguage(Language language) {
        this.language = language;
    }

    public boolean isBlind() {
        return blind;
    }
    public void setBlind(boolean blind) {
        this.blind = blind;
    }

    public List<UUID> getPersonIds() {
        return personIds;
    }
    public void setPersonIds(List<UUID> personIds) {
        this.personIds = personIds;
    }

    public String getStorageKey() {
        return storageKey;
    }
    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }
    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public UUID getGeneratedBy() {
        return generatedBy;
    }
    public void setGeneratedBy(UUID generatedBy) {
        this.generatedBy = generatedBy;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }
    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }

    public LocalDate getSentOn() {
        return sentOn;
    }
    public void setSentOn(LocalDate sentOn) {
        this.sentOn = sentOn;
    }

    public UUID getSentBy() {
        return sentBy;
    }
    public void setSentBy(UUID sentBy) {
        this.sentBy = sentBy;
    }

}
