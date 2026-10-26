package com.recruiting.platform.accounts.domain;

import com.recruiting.platform.contracts.PersonFileKind;
import com.recruiting.platform.contracts.TextExtractionStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Maps table {@code person_file} (1.2.0, ADR 0011). Audited except {@code extracted_text}. The generated column
 * {@code text_tsv} is not mapped (the database maintains it; search with
 * {@link PersonFileRepository#findPersonIdsByCvText}). The file version number is column {@code version}; the JPA
 * lock column is {@code row_version} (schema.md names both "version").
 */
@Entity
@Table(name = "person_file")
@EntityListeners(AuditingEntityListener.class)
@Audited
public class PersonFile {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "person_id")
    private UUID personId;

    @Column(name = "kind")
    @Enumerated(EnumType.STRING)
    private PersonFileKind kind;

    /** CV: max + 1 per person; DOCUMENT: 1. */
    @Column(name = "version")
    private Integer version;

    @Column(name = "is_current")
    private boolean current;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "sha256", length = 64)
    private String sha256;

    @Column(name = "storage_key", length = 300)
    private String storageKey;

    @Column(name = "text_status")
    @Enumerated(EnumType.STRING)
    private TextExtractionStatus textStatus = TextExtractionStatus.PENDING;

    @NotAudited
    @Column(name = "extracted_text")
    private String extractedText;

    @Column(name = "extracted_at")
    private Instant extractedAt;

    @Column(name = "uploaded_by")
    private UUID uploadedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by")
    private UUID updatedBy;

    @Version
    @Column(name = "row_version")
    private Long rowVersion;

    public PersonFile() {
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

    public PersonFileKind getKind() {
        return kind;
    }

    public void setKind(PersonFileKind kind) {
        this.kind = kind;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public boolean isCurrent() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public TextExtractionStatus getTextStatus() {
        return textStatus;
    }

    public void setTextStatus(TextExtractionStatus textStatus) {
        this.textStatus = textStatus;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }

    public Instant getExtractedAt() {
        return extractedAt;
    }

    public void setExtractedAt(Instant extractedAt) {
        this.extractedAt = extractedAt;
    }

    public UUID getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(UUID uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public Long getRowVersion() {
        return rowVersion;
    }
}
