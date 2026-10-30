package com.recruiting.platform.security.domain;

import com.recruiting.platform.contracts.ExportStatus;
import com.recruiting.platform.contracts.ListEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

/** Maps table {@code export_job} (1.3.0, CO-013): the export log itself, not audited. Holds no free-text search terms; the CSV bytes expire after 24 hours. */
@Entity
@Table(name = "export_job")
public class ExportJob {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity", updatable = false)
    private ListEntity entity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ExportStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "filters", updatable = false)
    private Map<String,Object> filters;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "filter_keys", length = 50, updatable = false)
    private List<String> filterKeys = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "columns", length = 100, updatable = false)
    private List<String> columns = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "sort", length = 100, updatable = false)
    private List<String> sort = new ArrayList<>();

    @Column(name = "purpose", length = 300, updatable = false)
    private String purpose;

    @Column(name = "include_compensation", updatable = false)
    private boolean includeCompensation;

    @Column(name = "row_count", updatable = false)
    private Integer rowCount;

    @Column(name = "requested_by", updatable = false)
    private UUID requestedBy;

    @Column(name = "requested_at", updatable = false)
    private Instant requestedAt;

    @Column(name = "decided_by")
    private UUID decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decision_reason", length = 500)
    private String decisionReason;

    @Column(name = "storage_key", length = 300)
    private String storageKey;

    @Column(name = "ready_at")
    private Instant readyAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    public ExportJob() {
        // for JPA
    }

    public UUID getId() {
        return id;
    }

    public ListEntity getEntity() {
        return entity;
    }
    public void setEntity(ListEntity entity) {
        this.entity = entity;
    }

    public ExportStatus getStatus() {
        return status;
    }
    public void setStatus(ExportStatus status) {
        this.status = status;
    }

    public Map<String,Object> getFilters() {
        return filters;
    }
    public void setFilters(Map<String,Object> filters) {
        this.filters = filters;
    }

    public List<String> getFilterKeys() {
        return filterKeys;
    }
    public void setFilterKeys(List<String> filterKeys) {
        this.filterKeys = filterKeys;
    }

    public List<String> getColumns() {
        return columns;
    }
    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public List<String> getSort() {
        return sort;
    }
    public void setSort(List<String> sort) {
        this.sort = sort;
    }

    public String getPurpose() {
        return purpose;
    }
    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public boolean isIncludeCompensation() {
        return includeCompensation;
    }
    public void setIncludeCompensation(boolean includeCompensation) {
        this.includeCompensation = includeCompensation;
    }

    public Integer getRowCount() {
        return rowCount;
    }
    public void setRowCount(Integer rowCount) {
        this.rowCount = rowCount;
    }

    public UUID getRequestedBy() {
        return requestedBy;
    }
    public void setRequestedBy(UUID requestedBy) {
        this.requestedBy = requestedBy;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }
    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public UUID getDecidedBy() {
        return decidedBy;
    }
    public void setDecidedBy(UUID decidedBy) {
        this.decidedBy = decidedBy;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }
    public void setDecidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
    }

    public String getDecisionReason() {
        return decisionReason;
    }
    public void setDecisionReason(String decisionReason) {
        this.decisionReason = decisionReason;
    }

    public String getStorageKey() {
        return storageKey;
    }
    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public Instant getReadyAt() {
        return readyAt;
    }
    public void setReadyAt(Instant readyAt) {
        this.readyAt = readyAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

}
