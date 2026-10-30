package com.recruiting.platform.pipeline.domain;

import com.recruiting.platform.contracts.ConflictDecision;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

/** Maps table {@code conflict_check} (1.3.0, PB-018/019): append-only log of conflict checks, no names in hits, not audited. */
@Entity
@Table(name = "conflict_check")
public class ConflictCheck {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "account_id", updatable = false)
    private UUID accountId;

    @Column(name = "company_name", length = 200, updatable = false)
    private String companyName;

    @Column(name = "job_order_id", updatable = false)
    private UUID jobOrderId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "hits", updatable = false)
    private List<Map<String,Object>> hits = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", updatable = false)
    private ConflictDecision decision;

    @Column(name = "override_reason", length = 500, updatable = false)
    private String overrideReason;

    @Column(name = "checked_by", updatable = false)
    private UUID checkedBy;

    @Column(name = "checked_at", updatable = false)
    private Instant checkedAt;

    public ConflictCheck() {
        // for JPA
    }

    public UUID getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }
    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public String getCompanyName() {
        return companyName;
    }
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public UUID getJobOrderId() {
        return jobOrderId;
    }
    public void setJobOrderId(UUID jobOrderId) {
        this.jobOrderId = jobOrderId;
    }

    public List<Map<String,Object>> getHits() {
        return hits;
    }
    public void setHits(List<Map<String,Object>> hits) {
        this.hits = hits;
    }

    public ConflictDecision getDecision() {
        return decision;
    }
    public void setDecision(ConflictDecision decision) {
        this.decision = decision;
    }

    public String getOverrideReason() {
        return overrideReason;
    }
    public void setOverrideReason(String overrideReason) {
        this.overrideReason = overrideReason;
    }

    public UUID getCheckedBy() {
        return checkedBy;
    }
    public void setCheckedBy(UUID checkedBy) {
        this.checkedBy = checkedBy;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }
    public void setCheckedAt(Instant checkedAt) {
        this.checkedAt = checkedAt;
    }

}
