package com.recruiting.platform.accounts.domain;

import com.recruiting.platform.contracts.OffLimitsReason;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;

/**
 * Maps table {@code off_limits_rule} (1.2.0). Exactly one of {@code accountId} and {@code companyName} is set
 * (CHECK). At most one open rule (no end date) per target (partial unique indexes). The note holds no personal data.
 */
@Entity
@Table(name = "off_limits_rule")
@Audited
public class OffLimitsRule extends AuditableEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "company_name", length = 200)
    private String companyName;

    @Column(name = "reason")
    @Enumerated(EnumType.STRING)
    private OffLimitsReason reason;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "starts_on")
    private LocalDate startsOn = LocalDate.now(java.time.ZoneId.of("Europe/Istanbul"));

    @Column(name = "ends_on")
    private LocalDate endsOn;

    public OffLimitsRule() {
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

    public OffLimitsReason getReason() {
        return reason;
    }

    public void setReason(OffLimitsReason reason) {
        this.reason = reason;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDate getStartsOn() {
        return startsOn;
    }

    public void setStartsOn(LocalDate startsOn) {
        this.startsOn = startsOn;
    }

    public LocalDate getEndsOn() {
        return endsOn;
    }

    public void setEndsOn(LocalDate endsOn) {
        this.endsOn = endsOn;
    }

    @Column(name = "include_subsidiaries")
    private boolean includeSubsidiaries;

    public boolean isIncludeSubsidiaries() {
        return includeSubsidiaries;
    }
    public void setIncludeSubsidiaries(boolean includeSubsidiaries) {
        this.includeSubsidiaries = includeSubsidiaries;
    }
}
