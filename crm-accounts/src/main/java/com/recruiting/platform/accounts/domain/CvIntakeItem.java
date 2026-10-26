package com.recruiting.platform.accounts.domain;

import com.recruiting.platform.contracts.CvIntakeStatus;
import com.recruiting.platform.contracts.SourceChannel;
import com.recruiting.platform.contracts.TextExtractionStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

/** Maps table {@code cv_intake_item} (1.2.0): a work queue, not audited; minimised on resolve and purged. */
@Entity
@Table(name = "cv_intake_item")
public class CvIntakeItem {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "batch_id")
    private UUID batchId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private CvIntakeStatus status = CvIntakeStatus.PENDING;

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

    @Column(name = "extracted_text")
    private String extractedText;

    /** {emails, phones, linkedinUrls}; reset to empty after resolve. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detected")
    private Map<String, List<String>> detected = emptyDetected();

    @Column(name = "source_channel")
    @Enumerated(EnumType.STRING)
    private SourceChannel sourceChannel;

    @Column(name = "attached_person_id")
    private UUID attachedPersonId;

    @Column(name = "uploaded_by")
    private UUID uploadedBy;

    @Column(name = "uploaded_at")
    private Instant uploadedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    public CvIntakeItem() {
        // for JPA
    }

    public static Map<String, List<String>> emptyDetected() {
        Map<String, List<String>> m = new HashMap<>();
        m.put("emails", List.of());
        m.put("phones", List.of());
        m.put("linkedinUrls", List.of());
        return m;
    }

    public UUID getId() {
        return id;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public void setBatchId(UUID batchId) {
        this.batchId = batchId;
    }

    public CvIntakeStatus getStatus() {
        return status;
    }

    public void setStatus(CvIntakeStatus status) {
        this.status = status;
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

    public Map<String, List<String>> getDetected() {
        return detected;
    }

    public void setDetected(Map<String, List<String>> detected) {
        this.detected = detected == null ? emptyDetected() : detected;
    }

    public SourceChannel getSourceChannel() {
        return sourceChannel;
    }

    public void setSourceChannel(SourceChannel sourceChannel) {
        this.sourceChannel = sourceChannel;
    }

    public UUID getAttachedPersonId() {
        return attachedPersonId;
    }

    public void setAttachedPersonId(UUID attachedPersonId) {
        this.attachedPersonId = attachedPersonId;
    }

    public UUID getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(UUID uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
