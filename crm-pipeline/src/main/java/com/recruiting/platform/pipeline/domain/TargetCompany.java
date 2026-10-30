package com.recruiting.platform.pipeline.domain;

import com.recruiting.platform.contracts.TargetCompanyStatus;
import com.recruiting.platform.contracts.TargetTier;
import jakarta.persistence.*;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;

/** Maps table {@code target_company} (1.3.0, SX-003): a company to approach for a search. Exactly one of {@code accountId} and {@code companyName} is set (CHECK). */
@Entity
@Table(name = "target_company")
@Audited
public class TargetCompany extends AuditableEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "job_order_id", updatable = false)
    private UUID jobOrderId;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "company_name", length = 200)
    private String companyName;

    @Column(name = "sector", length = 100)
    private String sector;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier")
    private TargetTier tier;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TargetCompanyStatus status = TargetCompanyStatus.TO_MAP;

    @Column(name = "rationale", length = 1000)
    private String rationale;

    @Column(name = "source", length = 200)
    private String source;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "off_limits_override_reason", length = 500)
    private String offLimitsOverrideReason;

    public TargetCompany() {
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

    public String getSector() {
        return sector;
    }
    public void setSector(String sector) {
        this.sector = sector;
    }

    public TargetTier getTier() {
        return tier;
    }
    public void setTier(TargetTier tier) {
        this.tier = tier;
    }

    public TargetCompanyStatus getStatus() {
        return status;
    }
    public void setStatus(TargetCompanyStatus status) {
        this.status = status;
    }

    public String getRationale() {
        return rationale;
    }
    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public String getSource() {
        return source;
    }
    public void setSource(String source) {
        this.source = source;
    }

    public String getNotes() {
        return notes;
    }
    public void setNotes(String notes) {
        this.notes = notes;
    }

    public UUID getOwnerId() {
        return ownerId;
    }
    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public String getOffLimitsOverrideReason() {
        return offLimitsOverrideReason;
    }
    public void setOffLimitsOverrideReason(String offLimitsOverrideReason) {
        this.offLimitsOverrideReason = offLimitsOverrideReason;
    }

}
