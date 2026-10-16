package com.recruiting.platform.accounts.domain;

import jakarta.persistence.*;
import com.recruiting.platform.contracts.ImportKind;
import com.recruiting.platform.contracts.ImportStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

/** Maps table {@code import_job}. Other modules reference it by id only (ADR 0001). */
@Entity
@Table(name = "import_job")
public class ImportJob extends AuditableEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "kind")
    @Enumerated(EnumType.STRING)
    private ImportKind kind;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ImportStatus status;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_content")
    @Basic(fetch = FetchType.LAZY)
    private byte[] fileContent;

    @Column(name = "total_rows")
    private Integer totalRows;

    @Column(name = "valid_rows")
    private Integer validRows;

    @Column(name = "duplicate_rows")
    private Integer duplicateRows;

    @Column(name = "imported_rows")
    private Integer importedRows;

    @Column(name = "errors")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Map<String, Object>> errors;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "completed_at")
    private Instant completedAt;

    public ImportJob() {
        // for JPA
    }

    public UUID getId() {
        return id;
    }

    public ImportKind getKind() {
        return kind;
    }

    public void setKind(ImportKind kind) {
        this.kind = kind;
    }

    public ImportStatus getStatus() {
        return status;
    }

    public void setStatus(ImportStatus status) {
        this.status = status;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public byte[] getFileContent() {
        return fileContent;
    }

    public void setFileContent(byte[] fileContent) {
        this.fileContent = fileContent;
    }

    public Integer getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }

    public Integer getValidRows() {
        return validRows;
    }

    public void setValidRows(Integer validRows) {
        this.validRows = validRows;
    }

    public Integer getDuplicateRows() {
        return duplicateRows;
    }

    public void setDuplicateRows(Integer duplicateRows) {
        this.duplicateRows = duplicateRows;
    }

    public Integer getImportedRows() {
        return importedRows;
    }

    public void setImportedRows(Integer importedRows) {
        this.importedRows = importedRows;
    }

    public List<Map<String, Object>> getErrors() {
        return errors;
    }

    public void setErrors(List<Map<String, Object>> errors) {
        this.errors = errors;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

}
